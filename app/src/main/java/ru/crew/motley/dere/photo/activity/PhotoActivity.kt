package ru.crew.motley.dere.photo.activity

import android.content.Context
import android.widget.Toast


fun Context.toast(resourceId: Int) = toast(getString(resourceId))

fun Context.toast(message: CharSequence) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

class PhotoActivity { /*: AppCompatActivity() {*/

    companion object {
        private const val TAG = "PhotoActivity"

        const val PERMISSION_REQUEST_CAMERA = 1001
        const val PERMISSION_REQUEST_SD = 1002
        /*private const val CAMERA_ID = 0
        private const val ASPECT_TOLERANCE = 0.2
        private const val TARGET_RATIO = 16.toDouble() / 9.toDouble()

        fun getIntent(context: Context) = Intent(context, PhotoActivity::class.java)*/
    }

/*
    //    var sv: SurfaceView
    private lateinit var holder: SurfaceHolder
    private lateinit var holderCallback: HolderCallback
    internal var camera: Camera? = null

//    var pictureButton: ImageButton

    private var debugPhoto: TextView? = null
    private var photoSizes: TextView? = null

    private val mInSampleSize: Int = 0

    private val handler = Handler()

    fun getIntent(context: Context): Intent {
        val i = Intent(context, PhotoActivity::class.java)
        return i
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_photo)
//        pictureButton = findViewById(R.id.btnTakePicture)
//        sv = findViewById(R.id.surfaceView)
        holder = surfaceView.holder
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
//        debugPhoto = findViewById(R.id.photo_debug)
//        photoSizes = findViewById(R.id.photo_size)
        takePhoto.setOnClickListener { requestSDPermissionAndTakePhoto() }
    }

    override fun onResume() {
        super.onResume()
        requestCameraPermissionAndShow()
    }

    private fun prepareCamera() {
        camera = Camera.open(CAMERA_ID)
        setCameraDisplayOrientation(CAMERA_ID)

        val params = camera!!.parameters
        for (focusMode in params.supportedFocusModes) {
//            debugPhoto!!.append(focusMode + "\n")
        }
        params.focusMode = FOCUS_MODE_CONTINUOUS_PICTURE
//        if (params.supportedFocusModes.contains(FOCUS_MODE_CONTINUOUS_PICTURE)) {
//            params.focusMode = FOCUS_MODE_CONTINUOUS_PICTURE
//        }
        params.pictureFormat = ImageFormat.JPEG
        params.jpegQuality = 100
        params.setRotation(90)
        camera!!.parameters = params
        setPreviewSize()
        holderCallback = HolderCallback()
        holder.addCallback(holderCallback)
    }

    override fun onPause() {
        super.onPause()
        if (camera != null)
            camera!!.release()
        camera = null
    }

    inner class HolderCallback : SurfaceHolder.Callback {

        override fun surfaceCreated(holder: SurfaceHolder) {
            camera?.setPreviewDisplay(holder)
            camera?.startPreview()
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            camera?.let {
                it.stopPreview()
                setCameraDisplayOrientation(CAMERA_ID)
                val p = it.parameters
                val pictureSizes = p.supportedPictureSizes
                val pictureSize = getPictureSize0(pictureSizes)
                p.setPictureSize(pictureSize.width, pictureSize.height)
                it.parameters = p
                it.setPreviewDisplay(holder)
                it.startPreview()
            }
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            camera?.stopPreview()
        }

    }

    private fun setPreviewSize() {
        val p = camera!!.parameters
        val previewSizes = p.supportedPreviewSizes
        val pictureSizes = p.supportedPictureSizes
        val displaySize = Point()
        windowManager.defaultDisplay.getSize(displaySize)
        val maxPreviewWidth = displaySize.y
        val maxPreviewHeight = displaySize.x

        val size = getOptimalPreviewSize(previewSizes, maxPreviewWidth, maxPreviewHeight)
        p.setPreviewSize(size.width, size.height)
        p.jpegQuality = 100
        val pictureSize = getPictureSize0(pictureSizes)
        p.setPictureSize(pictureSize.width, pictureSize.height)
        for (size1 in pictureSizes) {
//            photoSizes!!.append("s w/s " + size1.width + " " + size1.height + "\n")
        }
        camera!!.parameters = p
        setMyPreviewSize(size.height, size.width)
    }

    private fun getPictureSize(pictureSizes: List<Camera.Size>): Camera.Size {
        var result: Camera.Size = pictureSizes[0]
        if (result.width < result.height) {
            for (size in pictureSizes) {
                if (size.width > result.width) {
                    result = size
                }
            }
        } else {
            for (size in pictureSizes) {
                if (size.height > result.height) {
                    result = size
                }
            }
        }
        return result
    }

    private fun getPictureSize0(pictureSizes: List<Camera.Size>): Camera.Size {
        var optimalSize: Camera.Size? = null
        var minDiff = java.lang.Double.MAX_VALUE
        var dimension = 0

        for (size in pictureSizes) {
//            val ratio = size.width.toDouble() / size.height
            val ratio0 =
                    if (size.width > size.height) size.width.toDouble() / size.height
                    else size.height.toDouble() / size.height

            Log.d(TAG, "ratio -- $ratio0, target ratio -- $TARGET_RATIO")
            if (Math.abs(ratio0 - TARGET_RATIO) > ASPECT_TOLERANCE || size.height < dimension)
                continue
            dimension = size.height
            optimalSize = size
//            if (Math.abs(size.height - h) < minDiff) {
//                optimalSize = size
//                minDiff = Math.abs(size.height - h).toDouble()
//            }
        }
        return optimalSize!!
    }

    private fun setCameraDisplayOrientation(cameraId: Int) {
        val rotation = windowManager.defaultDisplay.rotation
//        photoSizes!!.append("DD r - $rotation\n")
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }
        var result = 0
        val info = Camera.CameraInfo()
        Camera.getCameraInfo(cameraId, info)
//        if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
        result = (360 - degrees + info.orientation) % 360
//        } else
//         передняя камера
//            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//                result = 360 - degrees - info.orientation
//                result += 360
//            }
//        result %= 360
        camera!!.setDisplayOrientation(result)
    }

    private fun requestCameraPermissionAndShow() {
        if (ContextCompat.checkSelfPermission(this, CAMERA) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(CAMERA),
                    PERMISSION_REQUEST_CAMERA)
        } else {
            prepareCamera()
        }
    }

    private fun requestSDPermissionAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(WRITE_EXTERNAL_STORAGE),
                    PERMISSION_REQUEST_SD)
        } else {
            takePicture()
        }
    }

    private fun autoFocusAndTakePucture() {
        camera?.autoFocus { success, camera ->
            if (success) {
                camera.takePicture(null, null) { data, camera ->
                    try {
//                        savePhoto(data)
//                        val options = BitmapFactory.Options()
//                        options.inJustDecodeBounds = true
//                        var bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options)
//                        val displaySize = Point()
//                        this.windowManager.defaultDisplay.getSize(displaySize)
//                        val inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, displaySize.x, displaySize.y)
//                        options.inJustDecodeBounds = false
//                        options.inSampleSize = inSampleSize
//                        bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options)
//                        Log.d(TAG, " w " + bitmap.width + "  h " + bitmap.height)
//                        takePhoto.isEnabled = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        takePhoto.isEnabled = true
                    } finally {
                        camera.stopPreview()
                        camera.setPreviewCallback(null)
                        camera.release()
                        this.camera = null
                    }
                }
            }
        }
    }

    private fun takePicture() {
        takePhoto.isEnabled = false

//        camera?.cancelAutoFocus()
        camera?.takePicture(null, null) { data, _ ->
            try {
                savePhoto(data)
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                var bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options)
                val displaySize = Point()
                this.windowManager.defaultDisplay.getSize(displaySize)
                val inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, displaySize.x, displaySize.y)
                options.inJustDecodeBounds = false
                options.inSampleSize = inSampleSize
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.size, options)
                Log.d(TAG, " w " + bitmap.width + "  h " + bitmap.height)
//                val i = PiideoActivity.getIntent(this, mPiideoName, mMessage, mMessageId)
//                startActivity(i)
//                finish()
                takePhoto.isEnabled = true
//                camera?.aut
            } catch (e: Exception) {
                e.printStackTrace()
                takePhoto.isEnabled = true
            }
        }
    }

    private fun savePhoto(data: ByteArray) {
        PhotoSaver(handler, PhotoSaver.TEMP_FILE_NAME)
                .byteArray(data)
                .start()
    }


//    private fun getViewBitmap(): Bitmap {
//        sv.isDrawingCacheEnabled = true
//        sv.buildDrawingCache(true)
//        val bitmap = Bitmap.createBitmap(sv.drawingCache)
//        sv.isDrawingCacheEnabled = false
//        sv.destroyDrawingCache()
//        return bitmap
//    }

    private fun getOptimalPreviewSize(sizes: List<Camera.Size>, w: Int, h: Int): Camera.Size {

        var optimalSize: Camera.Size? = null
        var minDiff = java.lang.Double.MAX_VALUE

        for (size in sizes) {
            val ratio = size.width.toDouble() / size.height
            Log.d(TAG, "ratio -- $ratio, target ratio -- $TARGET_RATIO")
            if (Math.abs(ratio - TARGET_RATIO) > ASPECT_TOLERANCE)
                continue
            if (Math.abs(size.height - h) < minDiff) {
                optimalSize = size
                minDiff = Math.abs(size.height - h).toDouble()
            }
        }
        return optimalSize!!
    }

    private fun setMyPreviewSize(width: Int, height: Int) {
        // Get the set dimensions
        val newProportion = height.toFloat() / width.toFloat()

        // Get the width of the screen

        val displaySize = Point()
        windowManager.defaultDisplay.getSize(displaySize)
        val maxPreviewWidth = displaySize.x
        val maxPreviewHeight = displaySize.y

        //        int screenWidth = getWindowManager().getDefaultDisplay().getWidth();
        //        int screenHeight = getWindowManager().getDefaultDisplay().getHeight();
        val screenProportion = maxPreviewHeight.toFloat() / maxPreviewWidth.toFloat()

        // Get the SurfaceView layout parameters
        val lp = surfaceView.layoutParams
        lp.height = maxPreviewHeight
        lp.width = maxPreviewWidth
//        if (newProportion < screenProportion) {
//            lp.width = maxPreviewWidth
//            lp.height = (maxPreviewHeight.toFloat() / newProportion).toInt()
//        } else {
//            lp.height = (newProportion * maxPreviewWidth.toFloat()).toInt()
//            lp.width = maxPreviewHeight
//        }
        // Commit the layout parameters
        surfaceView.layoutParams = lp
    }

//    fun saveFile2(data: ByteArray) {
//        Single.just(data).map({ d ->
//            val piideoFolder = File(Recorder.HOME_PATH)
//            if (!piideoFolder.exists()) {
//                piideoFolder.mkdir()
//            }
//            val photoFile = File(piideoFolder, mPiideoName!! + ".jpg")
//            val outStream = FileOutputStream(photoFile)
//            outStream.write(data)
//            outStream.flush()
//            outStream.close()
//            0
//        }).subscribeOn(Schedulers.io())
//                .subscribe()
//
//    }

    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (width > reqWidth) {
            val halfWidth = width / 2
            while (halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CAMERA -> {
                if (grantResults[0] == PERMISSION_GRANTED) {
                    prepareCamera()
                } else {
                    toast(R.string.perm_camera_warn)
                    finish()
                }
            }
            PERMISSION_REQUEST_SD -> {
                if (grantResults[0] == PERMISSION_GRANTED) {
                    autoFocusAndTakePucture()
                } else {
                    toast(R.string.perm_photo_warn)
                }
            }
        }
    }*/



}
