package ru.crew.motley.dere.photo.activity

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Parcelable
import android.support.design.widget.BottomSheetDialog
import android.support.media.ExifInterface
import android.support.v4.content.ContextCompat
import android.support.v4.view.PagerAdapter
import android.support.v7.widget.GridLayoutManager
import android.text.InputType
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.gson.Gson
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.util.ContentMetadata
import io.branch.referral.util.LinkProperties
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_new_photo.*
import kotlinx.android.synthetic.main.fragment_gallery.view.*
import kotlinx.android.synthetic.main.top_photo_options.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import ru.crew.motley.dere.GlideApp
import ru.crew.motley.dere.R
import ru.crew.motley.dere.db.Comment
import ru.crew.motley.dere.gallery.fragment.GalleryFragment
import ru.crew.motley.dere.networkrequest.RestClient
import ru.crew.motley.dere.networkrequest.RetrofitRequest
import ru.crew.motley.dere.networkrequest.feedmodels.MyRollResponse
import ru.crew.motley.dere.networkrequest.models.ImageUrl
import ru.crew.motley.dere.photo.PhotoSaver
import ru.crew.motley.dere.photo.fragment.AwaitingDialog
import ru.crew.motley.dere.photo.fragment.ConfirmationDialog
import ru.crew.motley.dere.utils.GetSampledImage
import ru.crew.motley.dere.utils.KeyboardUtil
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper
import java.io.File
import java.util.*

class CameraGalleryActivity : AppCompatActivity(),GetSampledImage.SampledImageAsyncResp {

    companion object {
         public const val TAG = "PhotoViewActivity"
        public const val UPLOADING_DIALOG = "uploadingDialog"
    }

    protected lateinit var photoPath: String
    private lateinit var awaitingDialog: AwaitingDialog
    private var photoComment: Comment? = null
    private var generatedUrl: String? = null
    var click = 0;
    var picComment = "";
    var selectedImage = 0;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideBars()
        setContentView(R.layout.activity_camera_gallery)
        showThumbnails()
        //loadComment()
    }


    fun fetch__myClickImage(){

        var sp = getSharedPreferences("sp",Context.MODE_PRIVATE)
        var user_id = sp.getString("user_id","")
        RestClient.get().getMyRolls(user_id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if(it.status.equals("true",true)){
                        Log.d("myrollresponse",""+ Gson().toJson(it, MyRollResponse::class.java))
                        showImages(it.response!!)
                    }else{
                        Log.d("myrollresponse",it.message);
                    }
                },{
                    Log.d("ddddddddddd", it.localizedMessage ?: "error")
                    Toast.makeText(this@CameraGalleryActivity, "Error ${it.localizedMessage ?: "error" }", Toast.LENGTH_SHORT).show()
                })

    }

    fun showImages(datalist : ArrayList<MyRollResponse.Response>){
        val adapter = WPhotoGalleryAdapter(this@CameraGalleryActivity,datalist)
        viewPager.adapter = adapter;
        viewPager.currentItem = selectedImage;
    }

    protected fun hideBars() {

        try {

            val decorView = getWindow().getDecorView();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN ; View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                decorView.setSystemUiVisibility(option);
                getWindow().setStatusBarColor(Color.TRANSPARENT);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val window = getWindow();
                // Translucent status bar
                window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager
                        .LayoutParams.FLAG_TRANSLUCENT_STATUS);
            }

        }catch (e:  java.lang.Exception){
            e.printStackTrace();
        }

      /*  window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        //window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)*/
    }

    protected fun loadComment() {
        val photoName = photoPath.substringAfterLast("/")
//        photoComment = DBLab.getInstance(this).readCommentValue(photoName)
        readCommentExif(photoPath)
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
                    }
                    picComment = photoComment?.fileComment ?: ""
                    comment.setText(photoComment?.fileComment ?: "")
                }
    }


    protected fun loadCommentNew() {
        val photoName = photoPath.substringAfterLast("/")
//        photoComment = DBLab.getInstance(this).readCommentValue(photoName)
        readCommentExifNew(photoPath)

    }

    private fun readCommentExifNew(photoPath: String) {
        Single.just(photoPath)
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .map {
                    val exif = ExifInterface(it)
                    Optional(exif.getAttribute(ExifInterface.TAG_USER_COMMENT))
                }
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
                    }
                    picComment = photoComment?.fileComment ?: ""
                    comment.setText(photoComment?.fileComment ?: "")
                    runOnUiThread(Runnable {
                        openOptions()
                    })

                }
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
        val adapter = PhotoGalleryAdapter(this@CameraGalleryActivity,commentsWithFiles)
        viewPager.adapter = adapter;
        viewPager.currentItem = selectedImage;
    }



    fun openOptions() {
        val dialog = BottomSheetDialog(this,R.style.SheetDialog)
        //dialog.window.decorView.setBackgroundResource(android.R.color.transparent)
        val view = layoutInflater.inflate(R.layout.bottomsheetoption, null)
        dialog.setContentView(view)
        KeyboardUtil(this, view)
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

        //readCommentExifNew(photoPath,etText)

        //loadCommentNew()

        btnSave.setOnClickListener {

            linEditTextLayout.visibility = View.VISIBLE
            linDeleteLayout.visibility = View.GONE
            linEditLinkLayout.visibility = View.GONE

            ivLocation.setImageResource(R.drawable.location)
            ivShare.setImageResource(R.drawable.sharenew)
            ivLink.setImageResource(R.drawable.editlink)
            ivDelete.setImageResource(R.drawable.delete)

            tvLocation.setTextColor(ContextCompat.getColor(this,R.color.blackish))
            tvShare.setTextColor(ContextCompat.getColor(this,R.color.blackish))
            tvLink.setTextColor(ContextCompat.getColor(this,R.color.blackish))
            tvDelete.setTextColor(ContextCompat.getColor(this,R.color.blackish))

            closeKeyboard(btnSave)
        }

        btnKeep.setOnClickListener {

            linEditTextLayout.visibility = View.VISIBLE
            linDeleteLayout.visibility = View.GONE
            linEditLinkLayout.visibility = View.GONE

            ivLocation.setImageResource(R.drawable.location)
            ivShare.setImageResource(R.drawable.sharenew)
            ivLink.setImageResource(R.drawable.editlink)
            ivDelete.setImageResource(R.drawable.delete)

            tvLocation.setTextColor(ContextCompat.getColor(this,R.color.blackish))
            tvShare.setTextColor(ContextCompat.getColor(this,R.color.blackish))
            tvLink.setTextColor(ContextCompat.getColor(this,R.color.blackish))
            tvDelete.setTextColor(ContextCompat.getColor(this,R.color.blackish))

            //dialog.dismiss()

        }

        btnDelete.setOnClickListener {
            onDelete()

            ivLocation.setImageResource(R.drawable.location)
            ivShare.setImageResource(R.drawable.sharenew)
            ivLink.setImageResource(R.drawable.editlink)
            ivDelete.setImageResource(R.drawable.delete)

            tvLocation.setTextColor(ContextCompat.getColor(this,R.color.blackish))
            tvShare.setTextColor(ContextCompat.getColor(this,R.color.blackish))
            tvLink.setTextColor(ContextCompat.getColor(this,R.color.blackish))
            tvDelete.setTextColor(ContextCompat.getColor(this,R.color.blackish))

            dialog.dismiss()
        }




        linLocation.setOnClickListener {

            closeKeyboard(linLocation)

            linEditTextLayout.visibility = View.VISIBLE
            linDeleteLayout.visibility = View.GONE
            linEditLinkLayout.visibility = View.GONE

            ivLocation.setImageResource(R.drawable.location)
            ivShare.setImageResource(R.drawable.sharenew)
            ivLink.setImageResource(R.drawable.editlink)
            ivDelete.setImageResource(R.drawable.delete)

            tvLocation.setTextColor(ContextCompat.getColor(this,R.color.blackish))
            tvShare.setTextColor(ContextCompat.getColor(this,R.color.blackish))
            tvLink.setTextColor(ContextCompat.getColor(this,R.color.blackish))
            tvDelete.setTextColor(ContextCompat.getColor(this,R.color.blackish))
            dialog.dismiss()

            val exif = ExifInterface(photoPath)
            val lat = exif.latLong?.get(0)
            val lon = exif.latLong?.get(1)

            if (lat != null && lon != null) {
                val i = PhotoLocationActivity.getIntent(
                        this,
                        lat,
                        lon,
                        cLat,
                        cLng)
                startActivity(i)
                Log.d("latlng",cLat.toString()+""+cLat)
            }

        }

        linShare.setOnClickListener {

            closeKeyboard(linShare)

            linEditTextLayout.visibility = View.VISIBLE
            linDeleteLayout.visibility = View.GONE
            linEditLinkLayout.visibility = View.GONE

            ivLocation.setImageResource(R.drawable.location)
            ivShare.setImageResource(R.drawable.sharenew)
            ivLink.setImageResource(R.drawable.editlink)
            ivDelete.setImageResource(R.drawable.delete)

            tvLocation.setTextColor(ContextCompat.getColor(this,R.color.blackish))
            tvShare.setTextColor(ContextCompat.getColor(this,R.color.blackish))
            tvLink.setTextColor(ContextCompat.getColor(this,R.color.blackish))
            tvDelete.setTextColor(ContextCompat.getColor(this,R.color.blackish))
            //dialog.dismiss()
            linShare.isEnabled = false
            generatedUrl?.let {
                showShareView()
                linShare.isEnabled = true
                return@setOnClickListener
            }
            awaitingDialog = AwaitingDialog.newInstance("Uploading...")
            awaitingDialog.isCancelable = false
            awaitingDialog.show(supportFragmentManager, UPLOADING_DIALOG)
            GetSampledImage(this, this).execute(photoPath, photoPath, resources.getDimension(R.dimen.user_image_downsample_size).toInt().toString())

            dialog.dismiss()


        }

        linLEdit.setOnClickListener {

            closeKeyboard(linLEdit)

            linEditTextLayout.visibility = View.GONE
            linDeleteLayout.visibility = View.GONE
            linEditLinkLayout.visibility = View.VISIBLE

            ivLocation.setImageResource(R.drawable.location)
            ivShare.setImageResource(R.drawable.sharenew)
            ivLink.setImageResource(R.drawable.linkactive)
            ivDelete.setImageResource(R.drawable.delete)

            tvLocation.setTextColor(ContextCompat.getColor(this,R.color.blackish))
            tvShare.setTextColor(ContextCompat.getColor(this,R.color.blackish))
            tvLink.setTextColor(ContextCompat.getColor(this,R.color.green))
            tvDelete.setTextColor(ContextCompat.getColor(this,R.color.blackish))
            //dialog.dismiss()

        }

        linDeletebtn.setOnClickListener {

            closeKeyboard(linDeletebtn)

            linEditTextLayout.visibility = View.GONE
            linDeleteLayout.visibility = View.VISIBLE
            linEditLinkLayout.visibility = View.GONE

            ivLocation.setImageResource(R.drawable.location)
            ivShare.setImageResource(R.drawable.sharenew)
            ivLink.setImageResource(R.drawable.editlink)
            ivDelete.setImageResource(R.drawable.deleteactive)

            tvLocation.setTextColor(ContextCompat.getColor(this,R.color.blackish))
            tvShare.setTextColor(ContextCompat.getColor(this,R.color.blackish))
            tvLink.setTextColor(ContextCompat.getColor(this,R.color.blackish))
            tvDelete.setTextColor(ContextCompat.getColor(this,R.color.red))
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

        dialog.setOnCancelListener {
            Log.d("dismisssscc","yes")
            hideSoftKeyboard(etText)
        }


        //behavior.peekHeight = 200
        if (dialog!=null){
            if (!dialog.isShowing){
                dialog.show()
            }
        }
    }


    fun hideSoftKeyboard(etText: EditText) {
        try {
            etText.isClickable = true;
            etText.isFocusableInTouchMode = true
            etText.isFocusable = true
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm!!.toggleSoftInput(InputMethodManager.HIDE_NOT_ALWAYS, 0)
        }catch (e : java.lang.Exception){
            e.printStackTrace()
        }

    }


    fun openKeyboard(InputEditText: EditText){
        try {
            InputEditText.requestFocus();
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm!!.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        }catch (e : java.lang.Exception){
            e.printStackTrace()
        }
    }


    fun closeKeyboard(view : View){
        try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager;
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }catch (e: java.lang.Exception){
            e.printStackTrace()
        }
    }
    var cLat = 0.0;
    var cLng = 0.0;

    fun editComment(comment : EditText){

        val commentArg = comment.text.toString()
        val currentComment = photoComment?.fileComment ?: ""

        val photoName = photoPath.substringAfterLast("/")
        val newComment =
                if (photoComment == null)
                    Comment(fileName = photoName, fileComment = commentArg)
                else
                    photoComment!!.apply { fileComment = commentArg }
        saveCommentExif(commentArg)
        this@CameraGalleryActivity.getSharedPreferences("DEFAULT", Context.MODE_PRIVATE)
                ?.edit()
                ?.putString(newComment.fileName, commentArg)
                ?.apply()
        photoComment = newComment

        comment.setText(photoComment?.fileComment ?: "")

        loadComment()

        /*CommentingDialog.getInstance(
                currentComment,
                object : CommentingDialog.DlgCallback {
                    override fun onConfirm(commentArg: String) {

                    }

                    override fun onCancel() {
//                            editComment.setImageResource(R.drawable.ic_edit)
                    }
                })
                .show(supportFragmentManager, "COMMENT_DIALOG")*/
    }

    fun onDelete(){

        deletePhoto()
      /*
        ConfirmationDialog.newInstance(
                object : ConfirmationDialog.DlgCallback {
                    override fun onConfirm() {
                        deletePhoto()
                    }

                    override fun onCancel() {
//                            deletePhoto.setImageResource(R.drawable.ic_delete)
                    }
                })
                .show(supportFragmentManager, "CONFIRMATION_DIALOG")*/
    }


    protected fun setListeners(currentLat: Double?, currentLon: Double?) {
        val exif = ExifInterface(photoPath)
        val lat = exif.latLong?.get(0)
        val lon = exif.latLong?.get(1)
        var toggleOptions = true;

        cLat = currentLat!!
        cLng = currentLon!!

        newPhoto.setOnClickListener {
            openOptions()
            if (toggleOptions){
                //buttonContainer.setVisibility(View.VISIBLE)
                //topPhotoOptionsContainer.setVisibility(View.VISIBLE)
//            showLocation.setVisibility(View.VISIBLE)
            }else{
                //buttonContainer.setVisibility(View.INVISIBLE)
                //topPhotoOptionsContainer.setVisibility(View.INVISIBLE)
//            showLocation.setVisibility(View.INVISIBLE)
            }
            toggleOptions = !toggleOptions
        }

        /*showLocation.setOnClickListener {
            if (lat != null && lon != null) {
                val i = PhotoLocationActivity.getIntent(
                        this,
                        lat,
                        lon,
                        currentLat,
                        currentLon)
                startActivity(i)
            }
        }
        deletePhoto.setOnClickListener {
            ConfirmationDialog.newInstance(
                    object : ConfirmationDialog.DlgCallback {
                        override fun onConfirm() {
                            deletePhoto()
                        }

                        override fun onCancel() {
//                            deletePhoto.setImageResource(R.drawable.ic_delete)
                        }
                    })
                    .show(supportFragmentManager, "CONFIRMATION_DIALOG")
        }*/
//        showGallery.setOnClickListener {
//            val i = GalleryActivity.getIntent(this)
//            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//            startActivity(i)
//        }
//        showLocation.setOnClickListener {
//            if (lat != null && lon != null) {
//                val i = PhotoLocationActivity.getIntent(
//                        this,
//                        lat,
//                        lon,
//                        currentLat,
//                        currentLon)
//                startActivity(i)
//            }
//        }
        /* shareLink.setOnClickListener {
             shareLink.isEnabled = false
             generatedUrl?.let {
                 showShareView()
                 shareLink.isEnabled = true
                 return@setOnClickListener
             }
             awaitingDialog = AwaitingDialog.newInstance("Uploading...")
             awaitingDialog.show(supportFragmentManager, UPLOADING_DIALOG)
             GetSampledImage(this, this).execute(photoPath, photoPath, resources.getDimension(R.dimen.user_image_downsample_size).toInt().toString())
         }*/

        /*editComment.setOnClickListener {
            val currentComment = photoComment?.fileComment ?: ""
            CommentingDialog.getInstance(
                    currentComment,
                    object : CommentingDialog.DlgCallback {
                        override fun onConfirm(commentArg: String) {
                            val photoName = photoPath.substringAfterLast("/")
                            val newComment =
                                    if (photoComment == null)
                                        Comment(fileName = photoName, fileComment = commentArg)
                                    else
                                        photoComment!!.apply { fileComment = commentArg }
                            saveCommentExif(commentArg)
                            this@PhotoViewActivity.getSharedPreferences("DEFAULT", Context.MODE_PRIVATE)
                                    ?.edit()
                                    ?.putString(newComment.fileName, commentArg)
                                    ?.apply()
                            photoComment = newComment

                            comment.setText(photoComment?.fileComment ?: "")
                        }

                        override fun onCancel() {
//                            editComment.setImageResource(R.drawable.ic_edit)
                        }
                    })
                    .show(supportFragmentManager, "COMMENT_DIALOG")
        }*/
    }

    protected fun saveCommentExif(comment: String) {
        Single.just(comment)
                .map {
                    val exif = ExifInterface(photoPath)
                    exif.setAttribute(ExifInterface.TAG_USER_COMMENT, comment.toByteArray(Charsets.UTF_8).joinToString(",") { it.toString() })
                    exif.saveAttributes()
                    0
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()
    }

    protected fun deletePhoto() {
        File(photoPath).delete()
        MediaScannerConnection.scanFile(this, arrayOf(photoPath), null, null)
        finish()
    }

    protected fun showFile() {
        loadComment()
        GlideApp.with(this)
                .load(photoPath)
                .into(newPhoto)
    }


    private fun shareLinkUsingFirebase(imageName: String, imageUrl: String, comment: String){
        val shortLinkTask = FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLink(Uri.parse(imageUrl))
                .setDomainUriPrefix("https://qjxx9.app.goo.gl")
                .setSocialMetaTagParameters(
                        DynamicLink.SocialMetaTagParameters.Builder()
                                .setTitle("Check out this spot and how to get Dere")
                                .setDescription("Directions to where you've been and where you want to go")
                                .setImageUrl(Uri.parse(imageUrl))
                                .build())
                .setAndroidParameters(
                        DynamicLink.AndroidParameters.Builder("ru.crew.motley.dere")
                                .setMinimumVersion(125)
                                .setFallbackUrl(Uri.parse("https://play.google.com/store/apps/details?id=ru.crew.motley.dere"))
                                .build())

                .buildShortDynamicLink()
                .addOnSuccessListener { result ->
                    // Short link created
                    val shortLink = result.shortLink
                    val flowchartLink = result.previewLink
                    if (!awaitingDialog.isHidden) awaitingDialog.dismiss()
                    generatedUrl = shortLink.toString()//.substring(0,18)
                    showShareView()
                }.addOnFailureListener {

                    // Error
                    // ...
                    Log.d("Errorooror","errr"+it.message);
                    if (!awaitingDialog.isHidden) awaitingDialog.dismiss()
                }

    }

    private fun showShareView1(generatedUrl : String,comment: String) {

        if (!awaitingDialog.isHidden) awaitingDialog.dismiss()

        var  newurl = generatedUrl.substringAfterLast("/")
        //newurl = newurl.substring(0,newurl.indexOf(".jpg"))

        Log.d("dddddddnew",newurl);

        //var wurl = "https://getdere.co/dere_app/pInfo.php?image="
        var wurl = "https://getdere.co/pInfo.php?image="
        //var wurl = "https://getdere.co/dere_app/index.php/Dere_controller/Image_view?image="
        //generatedUrl = generatedUrl.substringAfterLast("/")

        val i = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Subject Here");
            putExtra(Intent.EXTRA_TEXT, comment+"\r\n"+wurl+newurl)
            //putExtra(Intent.EXTRA_TEXT, "https://getdere.co/")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        Log.d("dddddd",wurl+newurl)
        startActivity(Intent.createChooser(i, "Share link..."))
        Handler().postDelayed({ /*shareLink.setImageResource(R.drawable.ic_share)*/ }, 1000)

    }

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
                .setContentImageUrl(imageUrl)
                .setContentMetadata(buoMeta)
                .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)
        val linkProperties = LinkProperties()
        generateLinkAndShareView(linkProperties, buo)

    }

    private fun generateLinkAndShareView(linkProperties: LinkProperties, branchUniversalObject: BranchUniversalObject) {
        branchUniversalObject.generateShortUrl(
                this,
                linkProperties,
                { url, error ->
                    if (!awaitingDialog.isHidden) awaitingDialog.dismiss()
                    if (error == null) {
                        generatedUrl = url
                        showShareView()
                    } else {
                        Log.e(TAG, "link creation: Branch error: " + error.message)
                    }
                    //shareLink.isEnabled = true
                })
    }

    private fun showShareView() {
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, generatedUrl)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(Intent.createChooser(i, "Share link..."))
        Handler().postDelayed({ /*shareLink.setImageResource(R.drawable.ic_share)*/ }, 1000)
    }

    override fun onSampledImageAsyncPostExecute(file: File) {
        share(file)
    }

    private fun share(file: File) {
//        val imageFile = RequestBody.create(MediaType.parse("multipart/form-data"), File(imagePath))
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
                                var comment = this@CameraGalleryActivity.getSharedPreferences("DEFAULT", Context.MODE_PRIVATE)
                                        ?.getString(file.name, null)
                                if (TextUtils.isEmpty(comment))
                                    comment = stub


                                if (comment==null) {
                                    comment = ""
                                }

                                showShareView1(imageName.image, comment!!)

                                /*shareLink(
                                        file.absolutePath.substringAfterLast("/"),
                                        imageName.image,
                                        comment!!)*/

                               /* shareLinkUsingFirebase(
                                        file.absolutePath.substringAfterLast("/"),
                                        imageName.image,
                                        comment!!)*/
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

                        if ( awaitingDialog != null){
                            if (awaitingDialog.isVisible){
                                awaitingDialog.dismiss()
                            }
                        }

                        t.printStackTrace()
                    }

                    override fun onComplete() {

                    }
                })
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase))
    }

    inner class PhotoGalleryAdapter(private val context: Context, private val photoFiles: List<Pair<String?, File>>) : PagerAdapter() {


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
                photoPath = getFilteredPhotoFiles()[position].absolutePath
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

    inner class WPhotoGalleryAdapter(private val context: Context, private val datalist: ArrayList<MyRollResponse.Response>) : PagerAdapter() {


        private val inflater: LayoutInflater
        private var commentHas: String? = null


        init {
            inflater = LayoutInflater.from(context)
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }

        override fun getCount(): Int {

            return datalist.size ?: 0
        }

        override fun instantiateItem(view: ViewGroup, position: Int): Any {
            val imageLayout = inflater.inflate(R.layout.fullimage_layout, view, false)!!

            var imageView = imageLayout.findViewById(R.id.newAPhoto) as ImageView

            GlideApp.with(context)
                    .load("http://getdere.co/dere_app/public/assets/uploads/"+datalist.get(position).image)
                    //.override(photoWidth, photoWidth)
                    .into(imageView)

            imageView.setOnClickListener {
                //openOptions()
                //photoPath = getFilteredPhotoFiles()[position].absolutePath
                //Log.d("selectedImageA",getFilteredPhotoFiles()[position].absolutePath)
                //loadCommentNew()

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
