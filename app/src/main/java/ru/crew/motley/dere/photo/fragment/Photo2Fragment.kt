package ru.crew.motley.dere.photo.fragment

import android.Manifest.permission.*
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.res.Configuration
import android.graphics.*
import android.hardware.camera2.*
import android.location.Location
import android.media.ImageReader
import android.media.MediaScannerConnection
import android.os.*
import android.support.design.widget.BottomSheetDialog
import android.support.media.ExifInterface
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.view.PagerAdapter
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.*
import android.view.Surface.*
import android.view.View.*
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.util.ContentMetadata
import io.branch.referral.util.LinkProperties
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_photo2.*
import kotlinx.android.synthetic.main.photo_controls.*
import kotlinx.android.synthetic.main.top_photo_options.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import ru.crew.motley.dere.GlideApp
import ru.crew.motley.dere.R
import ru.crew.motley.dere.db.Comment
import ru.crew.motley.dere.networkrequest.RestClient
import ru.crew.motley.dere.networkrequest.RetrofitRequest
import ru.crew.motley.dere.networkrequest.feedmodels.UploadImageResponse
import ru.crew.motley.dere.networkrequest.models.ImageUrl
import ru.crew.motley.dere.photo.CameraUtils
import ru.crew.motley.dere.photo.LocationProvider
import ru.crew.motley.dere.photo.LocationProviderCallback
import ru.crew.motley.dere.photo.PhotoSaver
import ru.crew.motley.dere.photo.activity.*
import ru.crew.motley.dere.utils.GetSampledImage
import ru.crew.motley.dere.utils.KeyboardUtil
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

fun Fragment.toast(resouceId: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this.activity, resouceId, duration).show()
}

fun Fragment.toast(string: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this.activity, string, duration).show()
}

data class Optional<M>(val value: M?)

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class Photo2Fragment : Fragment(), View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback, GetSampledImage.SampledImageAsyncResp {

    companion object {
        private val ORIENTATIONS = SparseIntArray()
        private val FRAGMENT_DIALOG = "dialog"
        private val GPS_FRAGMENT_DIALOG = "gps_dialog"
        private val UPLOADING_DIALOG = "uploadingDialog"
        private val DELETE_DIALOG = "deleteDialog"

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }

        private val TAG = "Photo2Fragment"

        private const val STATE_PREVIEW = 0
        private const val STATE_WAITING_LOCK = 1
        private const val STATE_WAITING_PRECAPTURE = 2
        private const val STATE_WAITING_NON_PRECAPTURE = 3
        private const val STATE_PICTURE_TAKEN = 4


        var click = 0;


        private val folderPath = Environment.getExternalStorageDirectory().absolutePath +
                "/" + PhotoSaver.APP_FOLER
        private val photoPath = folderPath + "/" + PhotoSaver.PHOTO_FILE_NAME

        fun newInstance(): Photo2Fragment {
            val fragment = Photo2Fragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }

    private val mSurfaceTextureListener = object : TextureView.SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            openCamera(width, height)
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {}

    }

    private var mCameraId: String? = null
    private var mCaptureSession: CameraCaptureSession? = null
    private var mCameraDevice: CameraDevice? = null
    private var mPreviewSize: Size? = null

    private val mStateCallback = object : CameraDevice.StateCallback() {

        override fun onOpened(cameraDevice: CameraDevice) {
            mCameraOpenCloseLock.release()
            mCameraDevice = cameraDevice
            createCameraPreviewSession()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            mCameraOpenCloseLock.release()
            cameraDevice.close()
            mCameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            mCameraOpenCloseLock.release()
            cameraDevice.close()
            mCameraDevice = null
            val activity = activity
            activity?.finish()
        }

    }

    var picComment = "";

    private var mBackgroundThread: HandlerThread? = null
    private var mBackgroundHandler: Handler? = null
    private var mImageReader: ImageReader? = null
    private val prepareHandler = Handler()
    private val mOnImageAvailableListener = getOnImageAvailableListener()
    private var stopCapture: Boolean = false
    private var lastPhoto: File? = null
    private var onActivityResultInvoked = false
    private lateinit var awaitingDialog: AwaitingDialog
    private var photoComment: Comment? = null
    private var generatedUrl: String? = null
    private var zoomed = false

    fun getOnImageAvailableListener(): ImageReader.OnImageAvailableListener {
        return ImageReader.OnImageAvailableListener { reader ->
            if (!stopCapture) {
                stopCapture = true

                val image = reader.acquireNextImage()
//                val image = reader.acquireLatestImage()
                val buffer = image.planes[0].buffer
                mCaptureSession!!.abortCaptures()
                mCaptureSession!!.stopRepeating()
                unlockFocus(null)


                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                image.close()

                val rotation = activity!!.windowManager.defaultDisplay.rotation
                val orientation = CameraUtils.orientation(getOrientation(rotation))
                savePhoto(bytes, orientation)
            }
        }
    }

    var largest: Size? = null

    private var mPreviewRequestBuilder: CaptureRequest.Builder? = null
    private var mPreviewRequest: CaptureRequest? = null
    private var mState = STATE_PREVIEW
    private val mCameraOpenCloseLock = Semaphore(1)
    private var mFlashSupported: Boolean = false
    private var mSensorOrientation: Int = 0
    private var scaledRotatedPhoto: Bitmap? = null
    private val mCaptureCallback = object : CameraCaptureSession.CaptureCallback() {

        private fun process(result: CaptureResult) {
            when (mState) {
                STATE_PREVIEW -> {
                }
                STATE_WAITING_LOCK -> {
                    val afState = result.get(CaptureResult.CONTROL_AF_STATE)
                    if (afState == null) {
                        captureStillPicture()
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                        if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN
                            captureStillPicture()
                        } else {
                            runPrecaptureSequence()
                        }
                    } else if (!CameraUtils.isFocusable(activity!!)) {
                        captureStillPicture()
                    }
                }
                STATE_WAITING_PRECAPTURE -> {
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE
                    }
                }
                STATE_WAITING_NON_PRECAPTURE -> {
                    val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN
                        captureStillPicture()
                    }
                }
            }
        }

        override fun onCaptureProgressed(session: CameraCaptureSession,
                                         request: CaptureRequest,
                                         partialResult: CaptureResult) {
            process(partialResult)
        }

        override fun onCaptureCompleted(session: CameraCaptureSession,
                                        request: CaptureRequest,
                                        result: TotalCaptureResult) {
            process(result)
        }

    }

    private var currentLocation: Location? = null
    private val locationProvider by lazy {
        LocationProvider(
                context?.applicationContext!!,
                createProviderCallback(),
                createLocationCallback())
    }

    private fun chooseOptimalSize(
            choices: Array<Size>,
            textureViewWidth: Int,
            textureViewHeight: Int,
            maxWidth: Int,
            maxHeight: Int,
            aspectRatio: Size)
            : Size {

        val bigEnough = ArrayList<Size>()
        val notBigEnough = ArrayList<Size>()
        val w = aspectRatio.width
        val h = aspectRatio.height
        for (option in choices) {
            if (option.width <= maxWidth * 1.7 && option.height <= maxHeight * 1.7 &&
                    option.height == option.width * h / w) {
                if (option.width >= textureViewWidth && option.height >= textureViewHeight) {
                    bigEnough.add(option)
                } else {
                    notBigEnough.add(option)
                }
            }
        }

        debug.append("w ${aspectRatio.width} h ${aspectRatio.height}\n")
        bigEnough.forEach { debug.append("b -  w ${it.width} h ${it.height}\n") }
        notBigEnough.forEach { debug.append("nb -  w ${it.width} h ${it.height}\n") }

        return when {
            bigEnough.size > 0 -> Collections.max(bigEnough, CompareSizesByArea())
            notBigEnough.size > 0 -> Collections.max(notBigEnough, CompareSizesByArea())
            else -> {
                Log.e(TAG, "Couldn't find any suitable preview size")
                choices[0]
            }
        }
    }

    private fun createProviderCallback() =
            object : LocationProviderCallback {
                override fun onPermissionRequest(requestPermission: Int) {
//                    requestPermissions(arrayOf(ACCESS_FINE_LOCATION), LocationProvider.REQUEST_PERMISSIONS_REQUEST_CODE)
                }

                override fun onResolutionRequired(ex: ResolvableApiException) {
                    activity?.let {
                        ex.startResolutionForResult(it, LocationProvider.REQUEST_CHECK_SETTINGS)
                    }
                }
            }

    private fun createLocationCallback() =
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    hideAwaitingDialog()
                    currentLocation = locationResult.lastLocation
                }
            }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_photo2, container, false)
        locationProvider.onCreate()
        retainInstance = true
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        takePhoto.setOnClickListener(this)


        showLocation.setOnClickListener {
            val exif = ExifInterface(lastPhoto!!.absolutePath)
            val lat = exif.latLong?.get(0)
            val lon = exif.latLong?.get(1)
            if (lat != null && lon != null) {
                val i = PhotoLocationActivity.getIntent(activity, lat, lon, currentLocation?.latitude, currentLocation?.longitude)
                startActivity(i)
            }
        }
        deletePhoto.setOnClickListener {
//            deletePhoto.setImageResource(R.drawable.ic_delete_red)
            ConfirmationDialog.newInstance(
                    object : ConfirmationDialog.DlgCallback {
                        override fun onConfirm() {
                            deletePhoto()
//                            deletePhoto.setImageResource(R.drawable.ic_delete)
                        }

                        override fun onCancel() {
//                            deletePhoto.setImageResource(R.drawable.ic_delete)
                        }
                    })
                    .show(childFragmentManager, DELETE_DIALOG)
        }
//        showGallery.setOnClickListener {
//            val i = GalleryActivity.getIntent(activity)
//            startActivity(i)
//        }
        shareLink.setOnClickListener { itShareLink ->
            shareLink.setImageResource(R.drawable.ic_share_green)
//            itShareLink.isEnabled = false
            lastPhoto?.let { file ->
                generatedUrl?.let {
                    showShareView()
                    itShareLink.isEnabled = true
                    return@setOnClickListener
                }
                awaitingDialog = AwaitingDialog.newInstance("Uploading...")
                awaitingDialog.show(childFragmentManager, UPLOADING_DIALOG)
                GetSampledImage(this, activity!!).execute(file.absolutePath, photoPath, resources.getDimension(R.dimen.user_image_downsample_size).toInt().toString())
//                share(file.absolutePath)
            }
        }

        editComment?.setOnClickListener {
            var currentComment = photoComment?.fileComment ?: ""
            val dialog = CommentingDialog.getInstance(
                    currentComment,
                    object : CommentingDialog.DlgCallback {
                        override fun onConfirm(commentText: String) {
                            val photoName = photoPath.substringAfterLast("/")
                            val newComment =
                                    if (photoComment == null)
                                        Comment(fileName = photoName, fileComment = commentText)
                                    else
                                        photoComment!!.apply { fileComment = commentText }
                            saveCommentExif(commentText)
                            activity?.getSharedPreferences("DEFAULT", Context.MODE_PRIVATE)
                                    ?.edit()
                                    ?.putString(newComment.fileName, commentText)
                                    ?.apply()
                            photoComment = newComment
                            comment.setText(photoComment?.fileComment ?: "")
                        }

                        override fun onCancel() {
//                            editComment.setImageResource(R.drawable.ic_edit)
                        }
                    }
            )
            dialog.show(childFragmentManager, "COMMENT_DIALOG")
        }

        val requiredPermission = requiredPermissions()
        if (requiredPermission.isNotEmpty()) {
            requestPermissions(requiredPermissions(), 333)
        } else {
            if (!File(PhotoSaver.APP_FOLDER_PATH).exists()) {
                File(PhotoSaver.APP_FOLDER_PATH).mkdir()
            }
            showThumbnail()
        }
    }

    private fun saveCommentExif(comment: String) {
//        Log.d(TAG, "1 $comment")
//        Log.d(TAG, "1 ${comment.toByteArray(Charsets.UTF_8)[0]}")
//        Log.d(TAG, "1 ${comment.toByteArray(Charsets.UTF_8).map { it.toString() }}")
//        Log.d(TAG, "1 ${comment.toByteArray(Charsets.UTF_8).joinToString(",") { it.toString() }}")
//
//        val encodedComment = comment.toByteArray(Charsets.UTF_8).joinToString(",") { it.toString() }
//        Log.d(TAG, "2 ${encodedComment.split(",").map { it.toByte() }}")
//        Log.d(TAG, "2 ${String(encodedComment.split(",").map { it.toByte() }.toByteArray())}")

        Single.just(comment)
                .map { comment ->
                    lastPhoto?.absolutePath?.let {
                        val exif = ExifInterface(it)
                        exif.setAttribute(ExifInterface.TAG_USER_COMMENT, comment.toByteArray(Charsets.UTF_8).joinToString(",") { it.toString() })
                        exif.saveAttributes()
                    }
                    0
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()
    }

    private fun showThumbnail() {
        updateLastPhoto()
        if (lastPhoto == null) {
            thumbnail.setImageBitmap(null)
            scaledRotatedPhoto = null
            return
        }
        lastPhoto?.let {
            if (it.exists()) {
                val scaledBitmap = scaledBitmap(it.absolutePath, 160, 90)
                val angle = CameraUtils.orientation(it.absolutePath)
                val rotatedAndScaledBitmap = CameraUtils.rotateImage(scaledBitmap, angle.toFloat())
                GlideApp.with(this@Photo2Fragment).asBitmap()
                        .centerCrop()
                        .load(rotatedAndScaledBitmap)
                        .into(thumbnail)
                //mychanges
                //thumbnail.setImageBitmap(rotatedAndScaledBitmap)

                thumbnail.setOnClickListener {
                    zoomImageFromThumb(thumbnail)
                }


                prepareHandler.post {
                    val scaledBitmap = scaledBitmap(it.absolutePath, 2160, 1080)
                    scaledRotatedPhoto = CameraUtils.rotateImage(scaledBitmap, angle.toFloat())
                }
            }
        }
        loadComment()
    }

    private fun loadComment() {
//        lastPhoto?.absolutePath?.substringAfterLast("/")?.let {
//            photoComment = DBLab.getInstance(activity!!).readCommentValue(it)
//        }
        lastPhoto?.absolutePath?.let {
            if (File(it).exists())
                readCommentExif(it)
        }
    }

    private fun readCommentExif(photoPath: String) {
        Single.just(photoPath)
                .map {
                    val exif = ExifInterface(it)
                    Optional(exif.getAttribute(ExifInterface.TAG_USER_COMMENT))
                }
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe { opt ->
                    opt?.value?.let {
                        photoComment = if (it.split(",")[0].toByteOrNull() != null) {
                            Comment(
                                    fileName = photoPath.substringAfterLast("/"),
                                    fileComment = String(it.split(",").map { it.toByte() }.toByteArray()))
                        } else {
                            Comment(
                                    fileName = photoPath.substringAfterLast("/"),
                                    fileComment = it)
                        }
                        picComment = photoComment?.fileComment ?: ""
                        comment.setText(photoComment?.fileComment ?: "")
                    }

                    if(opt?.value == null) {
                        comment.setText("")
                        photoComment = Comment(
                                fileName = photoPath.substringAfterLast("/"),
                                fileComment = "")
                    }
                }
    }


    private fun loadCommentNew() {
//        lastPhoto?.absolutePath?.substringAfterLast("/")?.let {
//            photoComment = DBLab.getInstance(activity!!).readCommentValue(it)
//        }
        lastPhoto?.absolutePath?.let {
            if (File(it).exists())
                readCommentExifNew(it)
        }
    }

    private fun readCommentExifNew(photoPath: String) {
        Single.just(photoPath)
                .map {
                    val exif = ExifInterface(it)
                    Optional(exif.getAttribute(ExifInterface.TAG_USER_COMMENT))
                }
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe { opt ->
                    opt?.value?.let {
                        photoComment = if (it.split(",")[0].toByteOrNull() != null) {
                            Comment(
                                    fileName = photoPath.substringAfterLast("/"),
                                    fileComment = String(it.split(",").map { it.toByte() }.toByteArray()))
                        } else {
                            Comment(
                                    fileName = photoPath.substringAfterLast("/"),
                                    fileComment = it)
                        }
                        picComment = photoComment?.fileComment ?: ""
                        comment.setText(photoComment?.fileComment ?: "")
                        requireActivity().runOnUiThread(Runnable {
                            openOptions()
                        })

                    }

                    if(opt?.value == null) {
                        comment.setText("")
                        photoComment = Comment(
                                fileName = photoPath.substringAfterLast("/"),
                                fileComment = "")
                    }
                }
    }

    private fun requiredPermissions() = mutableListOf<String>()
            .apply {
                if (notGranted(WRITE_EXTERNAL_STORAGE)) add(WRITE_EXTERNAL_STORAGE)
                if (notGranted(ACCESS_FINE_LOCATION)) add(ACCESS_FINE_LOCATION)
            }
            .toTypedArray()


    private fun notGranted(name: String) =
            ContextCompat.checkSelfPermission(activity!!, name) != PERMISSION_GRANTED

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun firstTimeAsked() {
        context?.getSharedPreferences("DEFAULT", Context.MODE_PRIVATE)
                ?.edit()
                ?.putBoolean("ASKED", true)
                ?.apply()
    }

    private fun isFirstTimeAsked(): Boolean {
        return context?.getSharedPreferences("DEFAULT", Context.MODE_PRIVATE)
                ?.getBoolean("ASKED", false) ?: false
    }

    private fun saveZoomed() {
        context?.getSharedPreferences("DEFAULT", Context.MODE_PRIVATE)
                ?.edit()
                ?.putBoolean("ZOOMED", zoomed)
                ?.apply()
    }

    private fun isZoomed(): Boolean {
        return context?.getSharedPreferences("DEFAULT", Context.MODE_PRIVATE)
                ?.getBoolean("ZOOMED", false) ?: false
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        if (texture.isAvailable) {
            openCamera(texture.width, texture.height)
        } else {
            texture.surfaceTextureListener = mSurfaceTextureListener
        }
        if (notGranted(ACCESS_FINE_LOCATION))
            return
        if (expanded.visibility != VISIBLE || lastPhoto?.exists() == false) {
            showThumbnail()
            resetUI()
        }
        if (zoomed) {
            resetUI()
        }
        loadComment()
//        toast("On Resume $stopCapture")
        if (!isFirstTimeAsked()) {
            locationProvider.onResume()
            firstTimeAsked()
        } else if (onActivityResultInvoked) {
            AwaitingDialog.newInstance()
                    .show(childFragmentManager, GPS_FRAGMENT_DIALOG)
            locationProvider.onResume()
            Handler().postDelayed(
                    { hideAwaitingDialog() },
                    4000)
            onActivityResultInvoked = false
        } else if (currentLocation == null) {
            locationProvider.onResume()
            AwaitingDialog.newInstance()
                    .show(childFragmentManager, GPS_FRAGMENT_DIALOG)
        }
    }

    private fun hideAwaitingDialog() {
        val awaitingDialog = childFragmentManager.findFragmentByTag(GPS_FRAGMENT_DIALOG)
                as AwaitingDialog?
        awaitingDialog?.dismiss()
    }

    override fun onPause() {
        super.onPause()
        closeCamera()
        stopBackgroundThread()
        if (notGranted(ACCESS_FINE_LOCATION))
            return
        locationProvider.onPause()
//        toast("On Pause $stopCapture")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            333 -> {
                if (grantResults.contains(PERMISSION_DENIED)) {
                    activity?.toast(R.string.request_permission)
                    activity?.finish()
                } else {
                    if (!File(folderPath).exists()) {
                        File(folderPath).mkdirs()
                    }
                    showThumbnail()
//                    locationProvider.onResume()
                    prepareHandler.post {
                        if (File(photoPath).exists()) {
                            val scaledBitmap = scaledBitmap(photoPath, 2160, 1080)
                            val angle = CameraUtils.orientation(photoPath).toFloat()
                            scaledRotatedPhoto = CameraUtils.rotateImage(scaledBitmap, angle)
                        }
                    }
                }
            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    private fun setUpCameraOutputs(width: Int, height: Int) {
        val activity = activity
        val manager = activity!!.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)

                // We don't use a front facing camera in this sample.
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue
                }

                val map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: continue

                val displaySize = Point()
                activity.windowManager.defaultDisplay.getRealSize(displaySize)

                var rotatedPreviewWidth = width
                var rotatedPreviewHeight = height
                var maxPreviewWidth = displaySize.x
                var maxPreviewHeight = displaySize.y

                map.getOutputSizes(ImageFormat.JPEG)
                        .forEach { debug.append("a ${it.height}, ${it.width}\n") }

                largest = map.getOutputSizes(ImageFormat.JPEG)
                        .toMutableList()
                        .filter {
                            if (maxPreviewHeight > maxPreviewWidth) {
                                it.width.toDouble() / it.height > maxPreviewHeight.toDouble() / maxPreviewWidth - 0.2
                            } else {
                                it.width.toDouble() / it.height > maxPreviewWidth.toDouble() / maxPreviewHeight - 0.2
                            }
                        }
                        .maxBy { it.width * it.height }

                if (largest == null) {
                    largest = map.getOutputSizes(ImageFormat.JPEG)
                            .toMutableList()
                            .filter { it.width.toDouble() / it.height > 1.75 }
                            .maxBy { it.width * it.height }!!
                }
                mImageReader = ImageReader.newInstance(
                        largest!!.width,
                        largest!!.height,
                        ImageFormat.JPEG,
                        2)
                        .apply {
                            setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler)
                        }

                val displayRotation = activity.windowManager.defaultDisplay.rotation

                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
                var swappedDimensions = false
                when (displayRotation) {
                    ROTATION_0, ROTATION_180 -> if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                        swappedDimensions = true
                    }
                    ROTATION_90, ROTATION_270 -> if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                        swappedDimensions = true
                    }
                    else -> Log.e(TAG, "Display rotation is invalid: $displayRotation")
                }

                if (swappedDimensions) {
                    rotatedPreviewWidth = height
                    rotatedPreviewHeight = width
                    maxPreviewWidth = displaySize.y
                    maxPreviewHeight = displaySize.x
                }

                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture::class.java),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largest!!)

                debug.append("p w ${mPreviewSize?.width} h ${mPreviewSize?.height}\n")

                val orientation = resources.configuration.orientation
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    texture!!.setAspectRatio(
                            mPreviewSize!!.width, mPreviewSize!!.height)
                } else {
                    texture!!.setAspectRatio(
                            mPreviewSize!!.height, mPreviewSize!!.width)
                }

               /* texture!!.setAspectRatio(
                        mPreviewSize!!.width, mPreviewSize!!.height)*/

                // Check if the flash is supported.
                val available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                mFlashSupported = available ?: false

                mCameraId = cameraId
                return
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: NullPointerException) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(childFragmentManager, FRAGMENT_DIALOG)
        }

    }

    private fun openCamera(width: Int, height: Int) {
        if (ContextCompat.checkSelfPermission(activity!!, CAMERA) != PERMISSION_GRANTED) {
            requestPermissions(arrayOf(CAMERA), PhotoActivity.PERMISSION_REQUEST_CAMERA)
            return
        }
        setUpCameraOutputs(width, height)
        configureTransform(width, height)
        val activity = activity
        val manager = activity!!.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            if (!mCameraOpenCloseLock.tryAcquire(5000, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            manager.openCamera(mCameraId!!, mStateCallback, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
        }

    }

    private fun closeCamera() {
        try {
            mCameraOpenCloseLock.acquire()
            if (null != mCaptureSession) {
                mCaptureSession!!.close()
                mCaptureSession = null
            }
            if (null != mCameraDevice) {
                mCameraDevice!!.close()
                mCameraDevice = null
            }
            if (null != mImageReader) {
                mImageReader!!.close()
                mImageReader = null
            }
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            mCameraOpenCloseLock.release()
        }
    }

    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("CameraBackground").apply {
            start()
            mBackgroundHandler = Handler(looper)
        }
    }

    private fun stopBackgroundThread() {
        mBackgroundThread?.let {
            it.quitSafely()
            try {
                it.join()
                mBackgroundThread = null
                mBackgroundHandler = null
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

    }

    private fun createCameraPreviewSession() {
        try {
            val texture = texture.surfaceTexture
            texture.setDefaultBufferSize(mPreviewSize!!.width, mPreviewSize!!.height)

            val surface = Surface(texture)

            mPreviewRequestBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            mPreviewRequestBuilder!!.addTarget(surface)

            mCameraDevice!!.createCaptureSession(Arrays.asList(surface, mImageReader!!.surface),
                    object : CameraCaptureSession.StateCallback() {

                        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                            if (null == mCameraDevice) {
                                return
                            }

                            mCaptureSession = cameraCaptureSession
                            try {
                                mPreviewRequestBuilder!!.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                                setAutoFlash(mPreviewRequestBuilder)
                                mPreviewRequest = mPreviewRequestBuilder!!.build()
                                mCaptureSession!!.setRepeatingRequest(mPreviewRequest!!,
                                        mCaptureCallback, mBackgroundHandler)
                            } catch (e: CameraAccessException) {
                                e.printStackTrace()
                            }
                        }

                        override fun onConfigureFailed(
                                cameraCaptureSession: CameraCaptureSession) {
                            toast(R.string.cam_fail)
                        }
                    }, null
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        val activity = activity
        if (null == texture || null == mPreviewSize || null == activity) {
            return
        }
        val rotation = activity.windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, mPreviewSize!!.height.toFloat(), mPreviewSize!!.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale = Math.max(
                    viewHeight.toFloat() / mPreviewSize!!.height,
                    viewWidth.toFloat() / mPreviewSize!!.width)
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        texture!!.setTransform(matrix)
    }

    private fun takePicture() {
        takePhoto.isEnabled = false
        if (currentLocation == null) {
            locationProvider.onResume()
            Handler().postDelayed({ takePhoto?.isEnabled = true }, 8000)
        } else {
            lockFocus()
        }
    }

    private fun lockFocus() {
        try {
            mPreviewRequestBuilder!!.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START)
            mState = STATE_WAITING_LOCK
            mCaptureSession!!.capture(mPreviewRequestBuilder!!.build(), mCaptureCallback,
                    mBackgroundHandler)
            CameraUtils.printCameraFocusModes(context!!)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            takePhoto.isEnabled = true
        }

    }

    private fun runPrecaptureSequence() {
        try {
            mPreviewRequestBuilder!!.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START)
            mState = STATE_WAITING_PRECAPTURE
            mCaptureSession!!.capture(mPreviewRequestBuilder!!.build(), mCaptureCallback,
                    mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    private fun captureStillPicture() {
        try {
            stopCapture = false
            val activity = activity
            if (null == activity || null == mCameraDevice) {
                return
            }
            val captureBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(mImageReader!!.surface)
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            setAutoFlash(captureBuilder)
            val rotation = activity.windowManager.defaultDisplay.rotation
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation))

            val captureCallback = object : CameraCaptureSession.CaptureCallback() {

                override fun onCaptureCompleted(session: CameraCaptureSession,
                                                request: CaptureRequest,
                                                result: TotalCaptureResult) {
                }
            }
//            prepareHandler.post { prepareBitmap() }
            mCaptureSession!!.stopRepeating()
            mCaptureSession!!.abortCaptures()
            mCaptureSession!!.capture(captureBuilder.build(), captureCallback, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    private fun getOrientation(rotation: Int): Int {
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360
    }

    private fun unlockFocus(session: CameraCaptureSession?) {
        try {
            mPreviewRequestBuilder!!.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL)
            setAutoFlash(mPreviewRequestBuilder)
            if (mCaptureSession != null)
                mCaptureSession!!.capture(mPreviewRequestBuilder!!.build(), mCaptureCallback,
                        mBackgroundHandler)
            mState = STATE_PREVIEW
            if (mCaptureSession != null)
                mCaptureSession!!.setRepeatingRequest(mPreviewRequest!!, mCaptureCallback,
                        mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.takePhoto -> {
                takePicture()
            }
        }
    }

    private fun setAutoFlash(requestBuilder: CaptureRequest.Builder?) {
        return
        if (mFlashSupported) {
            requestBuilder!!.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
        }
    }

    private fun dec2DMS(coordinate: Double): String {
        var subResult = coordinate
        subResult = if (subResult > 0) subResult else -subResult  // -105.9876543 -> 105.9876543
        var sOut = Integer.toString(subResult.toInt()) + "/1,"   // 105/1,
        subResult = subResult % 1 * 60         // .987654321 * 60 = 59.259258
        sOut = sOut + Integer.toString(subResult.toInt()) + "/1,"   // 105/1,59/1,
        subResult = subResult % 1 * 60000             // .259258 * 60000 = 15555
        sOut = sOut + Integer.toString(subResult.toInt()) + "/1000"   // 105/1,59/1,15555/1000
        return sOut
    }


    internal class CompareSizesByArea : Comparator<Size> {

        override fun compare(lhs: Size, rhs: Size): Int {
            return java.lang.Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
        }

    }

//    private fun prepareBitmap() {
//        if (texture.isAvailable) {
//            bitmapSingleton = texture.bitmap
//            val displaySize = Point()
//            activity!!.windowManager.defaultDisplay.getRealSize(displaySize)
//            expanded?.layoutParams?.let {
//                it.width = displaySize.x
//                it.height = displaySize.y
//            }
//            thumbnail?.visibility = GONE
//            buttonContainer.visibility = VISIBLE
//        }
//    }

    private fun updateLastPhoto() {
        lastPhoto = File(PhotoSaver.APP_FOLDER_PATH)
                .listFiles()
                .maxBy { it.name?.substringAfter("_")!! }
    }

    private fun deletePhoto() {
        lastPhoto?.delete()
        MediaScannerConnection.scanFile(activity, arrayOf(lastPhoto?.absolutePath), null, null)
        updateLastPhoto()
        foldImage()

        if(lastPhoto != null){
            showThumbnail()
        }
    }

    private fun savePhoto(buffer: ByteArray, orientation: String) {
        Single.just<ByteArray>(buffer)
                .map { d ->
                    saveFile(d)
                }
                .map { photoFile ->
                    currentLocation?.let {
                        saveExif(photoFile.absolutePath, it, orientation)
                    }
                    photoFile
                }
                .map { photoFile ->
                    MediaScannerConnection.scanFile(activity, arrayOf(photoFile.absolutePath), null, null)
                    photoFile
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe { photoFile ->
//                    toast(R.string.cam_photo_saved)
//                    lastPhoto = photoFile
//                    val i = NewPhotoActivity.getIntent(activity, photoFile.absolutePath)
//                    resetUI()
//                    startActivity(i)
//                }
                .subscribe(
                        { photoFile ->
                            //                            toast(R.string.cam_photo_saved)
                            lastPhoto = photoFile
//                            val i = NewPhotoActivity.getIntent(activity, photoFile.absolutePath)
//                            startActivity(i)
                            var lat = "1.0"
                            var lng = "1.0"
                            if (currentLocation!=null){
                                currentLocation.let {
                                    lat = it!!.latitude.toString()
                                    lng= it!!.longitude.toString()
                                }
                            }
                            if (lastPhoto!!.exists()) {
                                Log.d("ddddddddd", lastPhoto!!.absolutePath)
                                //uploadImageToServer(lastPhoto!!, lat, lng)
                            }
                            resetUI()
                            showThumbnail()
                        },
                        {
                            Log.e(TAG, "Photo saving failed")
                            takePhoto.isEnabled = true
                            throw RuntimeException(it)
                        }
                )

    }


    fun uploadImageToServer(file: File,lat : String , lng : String) {

        Log.d("datatoupload",file.name +" "+lat+" "+lng)

        val requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file)
        val image = MultipartBody.Part.createFormData("image", file.absolutePath, requestFile)
        val sp = context!!.getSharedPreferences("sp",Context.MODE_PRIVATE)
        val uid = sp.getString("user_id","")

        val user_id = toRequestBody(uid)
        val latatitude = toRequestBody(lat)
        val longititude = toRequestBody(lng)

        RestClient.get().uploadImage(user_id,image,latatitude,longititude)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it !=null){
                        if (it.status.equals("true",true)){
                            Log.d("user_idddd",it.message)
                            //sp.edit().putString("user_id",loginResponse.response!!.get(0).userId).apply()
                        }
                    }
                },{
                    Log.d("ddddddddddd", it.localizedMessage)
                    Toast.makeText(requireActivity(), "Error ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                })
    }

    fun toRequestBody(value: String): RequestBody {
        return RequestBody.create(MediaType.parse("text/plain"), value)
    }

    fun handleResponse(uploadImageResponse: UploadImageResponse){
        if (uploadImageResponse!=null){
            if (uploadImageResponse.status.equals("true",true)){
                Log.d("user_idddd",uploadImageResponse.message)
                //sp.edit().putString("user_id",loginResponse.response!!.get(0).userId).apply()
            }
        }
    }

    fun handleError(error: Throwable){
        Log.d("ddddddddddd", error.localizedMessage)
        Toast.makeText(requireActivity(), "Error ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
    }




    private fun saveFile(data: ByteArray): File {
        val date = SimpleDateFormat("MM.yy.dd_HH.mm.ss", Locale.getDefault()).format(Date())
        val photoFile = File(photoPath + date + PhotoSaver.PHOTO_FILE_EXT)
        val outStream = FileOutputStream(photoFile)
        outStream.write(data)
        outStream.flush()
        outStream.close()
        return photoFile
    }

    private fun saveExif(fullPath: String, location: Location, orientation: String) {
        val ef = ExifInterface(fullPath)
        Log.d(TAG, "orientation $orientation")
//        ef.setAttribute(android.media.ExifInterface.TAG_ORIENTATION,
//                            "${ExifInterface.ORIENTATION_ROTATE_90}")
        ef.setAttribute(ExifInterface.TAG_GPS_LATITUDE, dec2DMS(location.latitude))
        ef.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, dec2DMS(location.longitude))
        if (location.latitude > 0)
            ef.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
        else
            ef.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "S")
        if (location.longitude > 0)
            ef.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E")
        else
            ef.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W")
        ef.saveAttributes()
    }

    private fun scaledBitmap(path: String, reqHeight: Int, reqWidth: Int): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true

        BitmapFactory.decodeStream(FileInputStream(path), null, options)
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeStream(FileInputStream(path), null, options)
    }

    // ------------- Thumbnail animation ---------------
    private var currentAnimator: Animator? = null
    private val shortAnimationDuration = 200L
    private val startBounds = Rect()
    private var startScale: Float = -1.0f

    private fun zoomImageFromThumb(thumbView: View) {
        currentAnimator?.cancel()


        expanded.setImageBitmap(scaledRotatedPhoto)

        val finalBounds = Rect()
        val globalOffset = Point()

        thumbView.getGlobalVisibleRect(startBounds)
        container.getGlobalVisibleRect(finalBounds, globalOffset)
        startBounds.offset(-globalOffset.x, -globalOffset.y)
        finalBounds.offset(-globalOffset.x, -globalOffset.y)

        takePhoto.visibility = GONE
        thumbnailContainer.visibility = INVISIBLE
        //mychanges
        buttonContainer.visibility = GONE
        topPhotoOptionsContainer.visibility = GONE

        //mychanges Visible to Gone

        showThumbnails();
        startActivity(Intent(context,CameraGalleryActivity::class.java))

        expandedBG.setOnClickListener {
            //openOptions()

        }


        if (finalBounds.width().toFloat() / finalBounds.height()
                > startBounds.width().toFloat() / startBounds.height()) {
            // Extend start bounds horizontally
            startScale = startBounds.height().toFloat() / finalBounds.height()
            val startWidth = (startScale * finalBounds.width()).toInt()
            val deltaWidth = ((startWidth - startBounds.width()).toFloat() / 2).toInt()
            startBounds.left -= deltaWidth
            startBounds.right += deltaWidth
        } else {
            // Extend start bounds vertically
            startScale = startBounds.width().toFloat() / finalBounds.width()
            val startHeight = (startScale * finalBounds.height()).toInt()
            val deltaHeight = ((startHeight - startBounds.height()).toFloat() / 2).toInt()
            startBounds.top -= deltaHeight
            startBounds.bottom += deltaHeight
        }

        thumbView.alpha = 0f
        //expanded.visibility = View.VISIBLE
        //viewPager.visibility = View.VISIBLE

        expanded.pivotX = 0f
        expanded.pivotY = 0f

        val set = AnimatorSet()
        set.play(ObjectAnimator.ofFloat(
                expanded,
                View.X,
                startBounds.left.toFloat(),
                finalBounds.left.toFloat()))
                .with(ObjectAnimator.ofFloat(
                        expanded,
                        View.Y,
                        startBounds.top.toFloat(),
                        finalBounds.top.toFloat()))
                .with(ObjectAnimator.ofFloat(
                        expanded,
                        View.SCALE_X,
                        startScale,
                        1f))
                .with(ObjectAnimator.ofFloat(
                        expanded,
                        View.SCALE_Y,
                        startScale,
                        1f))
        set.duration = shortAnimationDuration
        set.interpolator = DecelerateInterpolator()
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                currentAnimator = null
                expandedBG.visibility = VISIBLE
                thumbnail.setOnClickListener(null)

            }

            override fun onAnimationCancel(animation: Animator) {
                currentAnimator = null
            }
        })
        set.start()
        currentAnimator = set
        handleBackPress()
        zoomed = false
        saveZoomed()
    }




    protected fun openOptions() {

        val dialog = BottomSheetDialog(requireActivity(),R.style.SheetDialog)
        //dialog.window.decorView.setBackgroundResource(android.R.color.transparent)
        val view = layoutInflater.inflate(R.layout.bottomsheetoption, null)
        dialog.setContentView(view)
        KeyboardUtil(requireActivity(), view)
        val linLocation = view.findViewById<LinearLayout>(R.id.linLocation);
        val linShare = view.findViewById<LinearLayout>(R.id.linShare);
        val linLEdit = view.findViewById<LinearLayout>(R.id.linLEdit);
        val linDeletebtn = view.findViewById<LinearLayout>(R.id.linDeletebtn);

        val linEditTextLayout = view.findViewById<LinearLayout>(R.id.linEditTextLayout)
        val ivEdit = view.findViewById<ImageView>(R.id.ivEdit)
        val linText = view.findViewById<LinearLayout>(R.id.linText)
        val etText = view.findViewById<EditText>(R.id.etText)
        val linDeleteLayout = view.findViewById<LinearLayout>(R.id.linDeleteLayout)
        val linEditLinkLayout = view.findViewById<LinearLayout>(R.id.linEditLinkLayout)
        val btnSave = view.findViewById<TextView>(R.id.btnSave)

        val ivLocation = view.findViewById<ImageView>(R.id.ivLocation)
        val ivShare = view.findViewById<ImageView>(R.id.ivShare)
        val ivLink = view.findViewById<ImageView>(R.id.ivLink)
        val ivDelete = view.findViewById<ImageView>(R.id.ivDelete)


        val tvLocation = view.findViewById<TextView>(R.id.tvLocation)
        val tvShare = view.findViewById<TextView>(R.id.tvShare)
        val tvLink = view.findViewById<TextView>(R.id.tvLink)
        val tvDelete = view.findViewById<TextView>(R.id.tvDelete)

        val btnKeep = view.findViewById<TextView>(R.id.btnKeep)
        val btnDelete = view.findViewById<TextView>(R.id.btnDelete)


        etText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        etText.setRawInputType(InputType.TYPE_CLASS_TEXT);

        etText.setText(picComment);

        btnKeep.setOnClickListener {

            ivLocation.setImageResource(R.drawable.location)
            ivShare.setImageResource(R.drawable.sharenew)
            ivLink.setImageResource(R.drawable.editlink)
            ivDelete.setImageResource(R.drawable.delete)

            tvLocation.setTextColor(ContextCompat.getColor(requireActivity(),R.color.blackish))
            tvShare.setTextColor(ContextCompat.getColor(requireActivity(),R.color.blackish))
            tvLink.setTextColor(ContextCompat.getColor(requireActivity(),R.color.blackish))
            tvDelete.setTextColor(ContextCompat.getColor(requireActivity(),R.color.blackish))

            dialog.dismiss()

        }

        btnDelete.setOnClickListener {
            onDelete()

            ivLocation.setImageResource(R.drawable.location)
            ivShare.setImageResource(R.drawable.sharenew)
            ivLink.setImageResource(R.drawable.editlink)
            ivDelete.setImageResource(R.drawable.delete)

            tvLocation.setTextColor(ContextCompat.getColor(requireActivity(),R.color.blackish))
            tvShare.setTextColor(ContextCompat.getColor(requireActivity(),R.color.blackish))
            tvLink.setTextColor(ContextCompat.getColor(requireActivity(),R.color.blackish))
            tvDelete.setTextColor(ContextCompat.getColor(requireActivity(),R.color.blackish))

            dialog.dismiss()
        }


        linLocation.setOnClickListener {

            linEditTextLayout.visibility = View.VISIBLE
            linDeleteLayout.visibility = View.GONE
            linEditLinkLayout.visibility = View.GONE

            ivLocation.setImageResource(R.drawable.location)
            ivShare.setImageResource(R.drawable.sharenew)
            ivLink.setImageResource(R.drawable.editlink)
            ivDelete.setImageResource(R.drawable.delete)

            tvLocation.setTextColor(ContextCompat.getColor(requireActivity(),R.color.blackish))
            tvShare.setTextColor(ContextCompat.getColor(requireActivity(),R.color.blackish))
            tvLink.setTextColor(ContextCompat.getColor(requireActivity(),R.color.blackish))
            tvDelete.setTextColor(ContextCompat.getColor(requireActivity(),R.color.blackish))
            dialog.dismiss()


            val exif = ExifInterface(lastPhoto!!.absolutePath)
            val lat = exif.latLong?.get(0)
            val lon = exif.latLong?.get(1)
            if (lat != null && lon != null) {
                val i = PhotoLocationActivity.getIntent(activity, lat, lon, currentLocation?.latitude, currentLocation?.longitude)
                startActivity(i)
            }

        }

        linShare.setOnClickListener {

            linEditTextLayout.visibility = View.VISIBLE
            linDeleteLayout.visibility = View.GONE
            linEditLinkLayout.visibility = View.GONE

            ivLocation.setImageResource(R.drawable.location)
            ivShare.setImageResource(R.drawable.sharenew)
            ivLink.setImageResource(R.drawable.editlink)
            ivDelete.setImageResource(R.drawable.delete)

            tvLocation.setTextColor(ContextCompat.getColor(requireActivity(),R.color.blackish))
            tvShare.setTextColor(ContextCompat.getColor(requireActivity(),R.color.blackish))
            tvLink.setTextColor(ContextCompat.getColor(requireActivity(),R.color.blackish))
            tvDelete.setTextColor(ContextCompat.getColor(requireActivity(),R.color.blackish))

            //shareLink.setImageResource(R.drawable.ic_share_green)
//            itShareLink.isEnabled = false
            linShare.isEnabled = false
            lastPhoto?.let { file ->
                generatedUrl?.let {
                    showShareView()
                    linShare.isEnabled = true
                    return@setOnClickListener
                }
                awaitingDialog = AwaitingDialog.newInstance("Uploading...")
                awaitingDialog.show(childFragmentManager, UPLOADING_DIALOG)
                GetSampledImage(this, activity!!).execute(file.absolutePath, photoPath, resources.getDimension(R.dimen.user_image_downsample_size).toInt().toString())
//                share(file.absolutePath)
            }

            dialog.dismiss()

        }

        linLEdit.setOnClickListener {

            linEditTextLayout.visibility = View.GONE
            linDeleteLayout.visibility = View.GONE
            linEditLinkLayout.visibility = View.VISIBLE

            ivLocation.setImageResource(R.drawable.location)
            ivShare.setImageResource(R.drawable.sharenew)
            ivLink.setImageResource(R.drawable.linkactive)
            ivDelete.setImageResource(R.drawable.delete)

            tvLocation.setTextColor(ContextCompat.getColor(requireActivity(),R.color.blackish))
            tvShare.setTextColor(ContextCompat.getColor(requireActivity(),R.color.blackish))
            tvLink.setTextColor(ContextCompat.getColor(requireActivity(),R.color.green))
            tvDelete.setTextColor(ContextCompat.getColor(requireActivity(),R.color.blackish))
            //dialog.dismiss()

        }

        linDeletebtn.setOnClickListener {

            linEditTextLayout.visibility = View.GONE
            linDeleteLayout.visibility = View.VISIBLE
            linEditLinkLayout.visibility = View.GONE

            ivLocation.setImageResource(R.drawable.location)
            ivShare.setImageResource(R.drawable.sharenew)
            ivLink.setImageResource(R.drawable.editlink)
            ivDelete.setImageResource(R.drawable.deleteactive)

            tvLocation.setTextColor(ContextCompat.getColor(requireActivity(),R.color.blackish))
            tvShare.setTextColor(ContextCompat.getColor(requireActivity(),R.color.blackish))
            tvLink.setTextColor(ContextCompat.getColor(requireActivity(),R.color.blackish))
            tvDelete.setTextColor(ContextCompat.getColor(requireActivity(),R.color.red))
            //dialog.dismiss()

        }

        ivEdit.setOnClickListener {

            if (click == 0){
                ivEdit.setImageResource(R.drawable.editdone)
                linText.setBackgroundResource(R.drawable.dropshadow)
                etText.isClickable = true;
                etText.isFocusableInTouchMode = true
                etText.isFocusable = true
                openKeyboard(etText)
                click = 1
            }else{
                ivEdit.setImageResource(R.drawable.edittext)
                linText.setBackgroundResource(android.R.color.transparent)
                etText.isClickable = false;
                etText.isFocusableInTouchMode = false
                etText.isFocusable = false
                click = 0
                closeKeyboard(etText)
                editComment(etText)
            }

        }

        //behavior.peekHeight = 200
        if (dialog!=null){
            if (!dialog.isShowing){
                dialog.show()
            }
        }

        //behavior.peekHeight = 200
    }


    fun openKeyboard(InputEditText: EditText){
        try {
            InputEditText.requestFocus();
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm!!.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        }catch (e : java.lang.Exception){
            e.printStackTrace()
        }
    }


    fun closeKeyboard(view : View){
        try {
            val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager;
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }catch (e: java.lang.Exception){
            e.printStackTrace()
        }
    }

    fun onDelete(){
        ConfirmationDialog.newInstance(
                object : ConfirmationDialog.DlgCallback {
                    override fun onConfirm() {
                        deletePhoto()
                    }

                    override fun onCancel() {
//                            deletePhoto.setImageResource(R.drawable.ic_delete)
                    }
                })
                .show(childFragmentManager, "CONFIRMATION_DIALOG")
    }

    fun editComment(comment : EditText){

        val commentText = comment.text.toString()
        var currentComment = photoComment?.fileComment ?: ""

        val photoName = photoPath.substringAfterLast("/")
        val newComment =
                if (photoComment == null)
                    Comment(fileName = photoName, fileComment = commentText)
                else
                    photoComment!!.apply { fileComment = commentText }
        saveCommentExif(commentText)
        activity?.getSharedPreferences("DEFAULT", Context.MODE_PRIVATE)
                ?.edit()
                ?.putString(newComment.fileName, commentText)
                ?.apply()
        photoComment = newComment
        comment.setText(photoComment?.fileComment ?: "")

        loadComment()
    }


    private fun handleBackPress() {

        view?.let {
            it.isFocusableInTouchMode = true
            it.requestFocus()
            it.setOnKeyListener { _, keyCode, event ->

                if (keyCode == KeyEvent.KEYCODE_BACK) {

                    val deleteDialog = childFragmentManager.findFragmentByTag(DELETE_DIALOG)
                    val commentDialog = childFragmentManager.findFragmentByTag("COMMENT_DIALOG")
                    if (deleteDialog != null && deleteDialog.isVisible) {
                        (deleteDialog as ConfirmationDialog).dismiss()
                    } else if (commentDialog != null && commentDialog.isVisible) {
                        (commentDialog as CommentingDialog).dismiss()
                    } else {
                        foldImage()
                    }
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun foldImage() {
        currentAnimator?.cancel()
        val set = AnimatorSet()
        set.play(ObjectAnimator.ofFloat(expanded, View.X, startBounds.left.toFloat()))
                .with(ObjectAnimator.ofFloat(expanded, View.Y, startBounds.top.toFloat()))
                .with(ObjectAnimator.ofFloat(expanded, View.SCALE_X, startScale))
                .with(ObjectAnimator.ofFloat(expanded, View.SCALE_Y, startScale))
        set.duration = shortAnimationDuration
        set.interpolator = DecelerateInterpolator()
        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) = resetUI()

            override fun onAnimationCancel(animation: Animator) = resetUI()
        })
        set.start()
        currentAnimator = set
        expandedBG.visibility = GONE
        //mychanges
        //zoomed = true
        zoomed = false
        saveZoomed()
    }

    private fun resetUI() {
        if (isZoomed()) {
            hideActiveCamera()
            currentAnimator = null
            view?.let {
                it.isFocusableInTouchMode = true
                it.requestFocus()
                it.setOnKeyListener { _, keyCode, event ->

                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        val deleteDialog = childFragmentManager.findFragmentByTag(DELETE_DIALOG)
                        val commentDialog = childFragmentManager.findFragmentByTag("COMMENT_DIALOG")
                        if (deleteDialog != null && deleteDialog.isVisible) {
                            (deleteDialog as ConfirmationDialog).dismiss()
                        } else if (commentDialog != null && commentDialog.isVisible) {
                            (commentDialog as CommentingDialog).dismiss()
                        } else {
                            foldImage()
                        }
                        true
                    } else {
                        false
                    }
                }
            }
            thumbnail.setOnClickListener(null)
            expanded.setImageBitmap(scaledRotatedPhoto)
        } else {
            showActiveCamera()
            currentAnimator = null
            view?.let {
                it.isFocusableInTouchMode = false
                it.setOnKeyListener(null)
            }
            thumbnail.setOnClickListener {
                zoomImageFromThumb(thumbnail)
            }
        }
    }

    fun showActiveCamera(){
        takePhoto.isEnabled = true
        thumbnail.alpha = 1f
        takePhoto.visibility = VISIBLE
        thumbnail.visibility = VISIBLE
        thumbnailContainer.visibility = VISIBLE
        expandedBG.visibility = GONE
        //expanded.visibility = View.GONE
        //viewPager.visibility = View.GONE
        buttonContainer.visibility = GONE
        topPhotoOptionsContainer.visibility = GONE
    }

    fun hideActiveCamera(){
        takePhoto.isEnabled = false
        thumbnail.alpha = 0f
        takePhoto.visibility = INVISIBLE
        thumbnail.visibility = INVISIBLE
        thumbnailContainer.visibility = INVISIBLE
        expandedBG.visibility = VISIBLE
        //expanded.visibility = View.VISIBLE
        //viewPager.visibility = View.VISIBLE
        //topPhotoOptionsContainer.visibility = VISIBLE
        //mychagse
        topPhotoOptionsContainer.visibility = GONE
    }

    override fun onSampledImageAsyncPostExecute(file: File) {
        share(file)
    }

    private fun share(file: File) {
        val requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file)
        val imageFile = MultipartBody.Part.createFormData("image", file.absolutePath, requestFile)
        RestClient.get().placesList(imageFile)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableObserver<Response<*>>() {
                    override fun onNext(response: Response<*>) {
                        try {
                            val pojoNetworkResponse = RetrofitRequest
                                    .checkForResponseCode(response.code())
                            if (pojoNetworkResponse.isSuccess && null != response.body()) {
                                val imageName = response.body() as ImageUrl
                                val stub = resources.getString(R.string.image_comment_stub)
//                                val exif = ExifInterface(file.absolutePath)
                                var comment = activity?.getSharedPreferences("DEFAULT", Context.MODE_PRIVATE)
                                        ?.getString(file.name, null)
                                if (TextUtils.isEmpty(comment))
                                    comment = stub
                                shareLink(
                                        file.absolutePath.substringAfterLast("/"),
                                        imageName.image,
                                        comment!!)
                            } else {
                                awaitingDialog.dismiss()
//                                retrofitErrorMessage
//                                        .postValue(RetrofitErrorMessage(errorMessage =
//                                        RetrofitRequest.getErrorMessage(response.errorBody()!!)))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            awaitingDialog.dismiss()

                        }

                    }

                    override fun onError(t: Throwable) {
                        t.printStackTrace()
                    }

                    override fun onComplete() {

                    }
                })
    }

//    private fun uploadFile(imagePath: String) {
//        val stream = FileInputStream(File(imagePath))
//        val storage = FirebaseStorage.getInstance().reference
//        val imageName = imagePath.substringAfterLast("/")
//        storage.child(imageName)
//                .putStream(stream)
//                .addOnFailureListener {
//                    Log.e(TAG, "Uploading failed.", it)
//                    toast("Uploading failed.")
//                }
//                .addOnSuccessListener { taskSnapshot ->
//                    val downloadUrl = taskSnapshot.downloadUrl
//                    Log.d(TAG, "${downloadUrl.toString()} - uploaded")
//                    Handler().postDelayed( {shareLink(imageName, downloadUrl.toString()) }, 5000)
//                }
//        Log.d(TAG, imagePath)
//    }

//    private fun shareLink(imageName: String) {
//        val link = FirebaseDynamicLinks.getInstance().createDynamicLink()
//                .setLongLink(Uri.parse("https://qjxx9.app.goo.gl/" +
//                        "?link=https%3A%2F%2Fwww.dere.online%2F%3Fid%3D$imageName" +
//                        "&apn=ru.crew.motley.dere"))
//                .buildDynamicLink()
//        Log.d(TAG, "${link.uri}")
//        ShareCompat.IntentBuilder.from(activity)
//                .setType("text/plain")
//                .setChooserTitle("Share link...")
//                .setText(link.uri.toString())
//                .startChooser()
//    }

    private fun shareLink(imageName: String, imageUrl: String, comment: String) {
        val buoMeta = ContentMetadata().apply {
            customMetadata["id"] = imageName
            customMetadata["image_url"] = imageUrl
            customMetadata["\$og_image_url"] = imageUrl
            customMetadata["\$og_image_width"] = "0"
            customMetadata["\$og_image_height"] = "0"
//            customMetadata["\$og_description"] = "Directions to where you've been and where you want to go"
//            customMetadata["\$og_title"] = "Check out this spot and how to get Dere"
            customMetadata["\$og_type"] = "website"
            customMetadata["comment"] = photoComment?.fileComment
        }
        val buo = BranchUniversalObject()
                .setCanonicalIdentifier(UUID.randomUUID().toString())
                .setTitle("Check out this spot and how to get Dere")
                .setContentDescription(comment)
//                .setContentImageUrl(imageUrl)
                .setContentMetadata(buoMeta)
                .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)
        val linkProperties = LinkProperties()
        generateLinkAndShareView(linkProperties, buo)
    }

    private fun generateLinkAndShareView(linkProperties: LinkProperties, branchUniversalObject: BranchUniversalObject) {
        branchUniversalObject.generateShortUrl(
                activity!!,
                linkProperties,
                { url, error ->
                    if (error == null) {
                        generatedUrl = url
                        showShareView()
                    } else {
                        Log.e(TAG, "link creation: Branch error: " + error.message)
                    }
                    if (!awaitingDialog.isHidden) awaitingDialog.dismiss()
                    shareLink.isEnabled = true
                })
    }

    var photoItemsVisible = false;
    private fun togglePhotoItems() {
        if(photoItemsVisible){
            buttonContainer.visibility = INVISIBLE

            val color = R.color.black_trans_40;
            if (Build.VERSION.SDK_INT >= 23) {
                // Call some material design APIs here
//                newPhoto.setForeground(Drawable)
            }
        }else{
            buttonContainer.visibility = VISIBLE
        }
            photoItemsVisible = !photoItemsVisible;
    }

    private fun showShareView() {
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, generatedUrl)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(Intent.createChooser(i, "Share link..."))
        Handler().postDelayed({ shareLink.setImageResource(R.drawable.ic_share) }, 1000)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            LocationProvider.REQUEST_CHECK_SETTINGS -> {
                if (resultCode == Activity.RESULT_OK) {
                    onActivityResultInvoked = true
                } else {
                    locationProvider.onPause()
                    takePhoto.isEnabled = true
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(CalligraphyContextWrapper.wrap(context))
    }


    protected fun showThumbnails() {
        val photoFiles = File(PhotoSaver.APP_FOLDER_PATH).listFiles()
        photoFiles.sortByDescending {
            it.name.substringAfter("_")
        }
        readCommentsExif(photoFiles)
    }

    private fun readCommentsExif(photoFiles: Array<File>) {
        Single.just(photoFiles)
                .map {
                    it.map {
                        val exif = ExifInterface(it.absolutePath)
                        val comment = exif.getAttribute(ExifInterface.TAG_USER_COMMENT)
                        if (comment == null) {
                            Pair(null, it)
                        } else {
                            if (comment.split(",")[0].toByteOrNull() != null) {
                                Pair(String(comment.split(",").map { it.toByte() }.toByteArray()), it)
                            } else {
                                Pair(comment, it)
                            }
                        }
                    }
//                            .filter {
//                                querySearch.text.toString().toLowerCase().isBlank() || it.first?.toLowerCase()?.contains(querySearch.text.toString().toLowerCase()) ?: false
//                            }

                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { commentsWithFiles ->
                    showRecycler(commentsWithFiles)
                }
    }

    private fun showRecycler(commentsWithFiles: List<Pair<String?, File>>) {
        val adapter = PhotoGalleryAdapter(requireActivity(),commentsWithFiles)
        viewPager.adapter = adapter;
        //viewPager.currentItem = selectedImage;
    }

    inner class PhotoGalleryAdapter(private val context: Context,private val photoFiles: List<Pair<String?, File>>) : PagerAdapter() {


        private val inflater: LayoutInflater
        private var commentHas: String? = null


        init {
            inflater = LayoutInflater.from(context)
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }

        override fun getCount(): Int {
            return getFilteredPhotoFiles().size
        }

        private fun getFilteredPhotoFiles(): List<File> {
            commentHas?.let { value -> Log.d("BLANK", ""  + value.isBlank()); }
            commentHas?.let { value ->
                return photoFiles.filter { value.isBlank() || it.first?.toLowerCase()?.contains(value.toLowerCase()) ?: false }
                        .map { it.second }
            }
            return photoFiles.map { it.second }
        }

        override fun instantiateItem(view: ViewGroup, position: Int): Any {
            val imageLayout = inflater.inflate(R.layout.fullimage_layout, view, false)!!

            var imageView = imageLayout.findViewById(R.id.newAPhoto) as ImageView

            GlideApp.with(context)
                    .load(getFilteredPhotoFiles()[position])
                    //.override(photoWidth, photoWidth)
                    .into(imageView)

            imageView.setOnClickListener {
                //openOptions()
                //photoPath = getFilteredPhotoFiles()[position].absolutePath
                lastPhoto = getFilteredPhotoFiles()[position]
                Log.d("selectedImageA",getFilteredPhotoFiles()[position].absolutePath)
                loadCommentNew()

            }

            //imageView.setImageBitmap(Constant.decodeSampledBitmapFromResource(context.getResources(), wlist[position].img, 540, 900))




            view.addView(imageLayout, 0)

            return imageLayout
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view.equals(`object`)
        }

        override fun restoreState(state: Parcelable?, loader: ClassLoader?) {}

        override fun saveState(): Parcelable? {
            return null
        }
    }


}