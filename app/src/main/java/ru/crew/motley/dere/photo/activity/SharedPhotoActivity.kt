package ru.crew.motley.dere.photo.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.graphics.Bitmap

import io.branch.referral.Branch
import io.reactivex.schedulers.Schedulers
import io.reactivex.android.schedulers.AndroidSchedulers
import okhttp3.ResponseBody
import retrofit2.Response

import ru.crew.motley.dere.R
import ru.crew.motley.dere.db.DBLab
import ru.crew.motley.dere.db.Comment
import ru.crew.motley.dere.photo.PhotoSaver
import ru.crew.motley.dere.networkrequest.RestClient
import ru.crew.motley.dere.photo.fragment.AwaitingDialog

import android.graphics.BitmapFactory
import android.R.attr.path
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.support.design.widget.BottomSheetDialog
import android.support.media.ExifInterface
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import io.reactivex.Single
import kotlinx.android.synthetic.main.activity_share_photo.*
import kotlinx.android.synthetic.main.top_photo_options.*
import ru.crew.motley.dere.GlideApp
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper
import java.io.*

class SharedPhotoActivity : AppCompatActivity() {


    companion object {
        private const val TAG = "SharedPhotoActivity"
    }

    private lateinit var photoName: String
    private lateinit var photoComment: String
    private lateinit var awaitingDialog: AwaitingDialog
    protected lateinit var photoPath: String
    var click = 0;
    var picComment = "";


    private var photoComment1: Comment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideBars()
        setContentView(R.layout.activity_share_photo)
//        setDynamicLinkHandler()
        newPhoto.setOnClickListener {
            openOptions()
        }
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

       /* window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        //window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)*/
    }


    protected fun showFile() {
        GlideApp.with(this)
                .load(photoPath)
                .into(newPhoto)
        readCommentExifNew(photoPath)
    }


    override fun onStart() {
        super.onStart()

       /* val action: String? = intent?.action
        val data: Uri? = intent?.data

        photoName = data.toString()

        Log.d("reeeeeeeeeee",photoName)

        if(!photoName.contains(".jpg")){
            photoName = photoName+".jpg"
        }

        photoPath = PhotoSaver.APP_FOLDER_PATH + "/" + photoName.substringAfterLast("=")
        Log.d("reeeeeeeeeee",photoPath)
//                                    photoComment = it["comment"]!! //mychanges

        val imageFile = File(photoPath)
        if (!imageFile.exists()) {
            downloadFile(data.toString().substringAfterLast("="))
        } else {
            showFile()
            //setListeners(1.0, 1.0)
            //setListeners(null, null)
        }
        */


        /*try {
            FirebaseDynamicLinks.getInstance()
                    .getDynamicLink(intent)
                    .addOnSuccessListener(this) { pendingDynamicLinkData ->
                        // Get deep link from result (may be null if no link is found)
                        var deepLink: Uri? = null
                        if (pendingDynamicLinkData != null) {
                            deepLink = pendingDynamicLinkData.link
                            Log.d("reeeeeeeeeee",deepLink.toString())
                            awaitingDialog = AwaitingDialog.newInstance("Downloading...")
                            photoName = deepLink.toString()
                            photoPath = PhotoSaver.APP_FOLDER_PATH + "/" + photoName.substringAfterLast("/")
                            Log.d("reeeeeeeeeee",photoPath)
//                                    photoComment = it["comment"]!! //mychanges

                            val imageFile = File(photoPath)
                            if (!imageFile.exists()) {
                                downloadFile(deepLink.toString().substringAfterLast("/"))
                            } else {
                                showFile()
                                //setListeners(1.0, 1.0)
                                //setListeners(null, null)
                            }
                        }else{
                            Log.d("reeeeeeeeeee","nuull")
                        }

                        // Handle the deep link. For example, open the linked
                        // content, or apply promotional credit to the user's
                        // account.
                        // ...

                        // ...
                    }
                    .addOnFailureListener(this) { e -> Log.w(TAG, "getDynamicLink:onFailure", e) }
        }catch (e :Exception){

        }*/




        // Branch init
//        Branch.getInstance().initSession({ referringParams, error ->
//            if (error == null) {
//                Log.d(TAG, referringParams.toString())
//
//            } else {
//                Log.e("BRANCH SDK", error.message)
//            }
//        }, this.intent.data, this)
        try {
            Branch.getInstance().initSession(
                    { branchUniversalObject, linkProperties, error ->
                        if (error == null) {
                            /*if (branchUniversalObject == null) {
                                val i = Photo2Activity.getIntent(this.applicationContext)
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(i)
                                finish()
                            }*/
                            if (branchUniversalObject == null) {
                                val action: String? = intent?.action
                                val data: Uri? = intent?.data

                                photoName = data.toString()

                                Log.d("dddddddd", photoName)

                                if (photoName.contains("=")) {
                                    photoName = photoName.replace("=","/")
                                }

                                photoPath = PhotoSaver.APP_FOLDER_PATH + "/" + photoName.substringAfterLast("/")
                                Log.d("reeeeeeeeeee", photoPath)
//                                    photoComment = it["comment"]!! //mychanges

                                val imageFile = File(photoPath)
                                if (!imageFile.exists()) {
                                    awaitingDialog = AwaitingDialog.newInstance("Downloading...")
                                    Log.d("dddddddeee",photoPath.substringAfterLast("/"))
                                    downloadFile(photoPath.substringAfterLast("/"))
                                } else {
                                    showFile()
                                    //setListeners(1.0, 1.0)
                                    //setListeners(null, null)
                                }
                            }

                                branchUniversalObject.contentMetadata
                                        ?.customMetadata
                                        ?.let {
                                            awaitingDialog = AwaitingDialog.newInstance("Downloading...")
                                            photoName = it["id"]!!
                                            photoPath = PhotoSaver.APP_FOLDER_PATH + "/" + photoName
//                                    photoComment = it["comment"]!! //mychanges

                                            val imageFile = File(photoPath)
                                            if (!imageFile.exists()) {
                                                downloadFile(it["image_url"]!!.substringAfterLast("/"))
                                            } else {
                                                showFile()
                                                //setListeners(null, null)
                                            }
                                        }

                        }else {
                            toast("Photo fetching failed. Try again later...")
                            Log.e(TAG, error.message)
                        }
                    },
                    this.intent.data,
                    this)
        }catch (e : Exception){
            e.printStackTrace()
        }

    }


    public fun getLocation(photoPath : String){

    }




    private fun readCommentExifNew(photoPath: String) {
        Single.just(photoPath)
                .observeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .map {
                    val exif = ExifInterface(it)
                    Optional(exif.getAttribute(ExifInterface.TAG_USER_COMMENT))
                }
                .subscribe ({ opt ->
                    opt?.value?.let {
                        photoComment1 = if (it.split(",")[0].toByteOrNull() != null) {
                            Comment(
                                    fileName = photoPath.substringAfterLast("/"),
                                    fileComment = String(it.split(",").map { it.toByte() }.toByteArray()))
                        } else {
                           Comment(
                                    fileName = photoPath.substringAfterLast("/"),
                                    fileComment = it)
                        }
                    }
                    picComment = photoComment1?.fileComment ?: ""
                    Log.d("picComment",picComment+"\\")
                    //comment.setText(photoComment1?.fileComment ?: "")
                   /* runOnUiThread(Runnable {
                        openOptions()
                    })*/

                },{
                    Log.d("eeeeeeee",it!!.localizedMessage ?: "errr")
                })
    }

    public override fun onNewIntent(intent: Intent) {
        this.intent = intent
    }

    private fun downloadFile(newPhotoName: String) {
        if (!File(PhotoSaver.APP_FOLDER_PATH).exists()) {
            File(PhotoSaver.APP_FOLDER_PATH).mkdir()
        }
        awaitingDialog.show(supportFragmentManager, "AWAITING_DIALOG")
        RestClient.get().downloadImage(newPhotoName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { res -> showResult(res) },
                        { error -> showError() }
                )
    }

    fun showResult(res: ResponseBody){
        awaitingDialog.dismiss()
        val imageFile = File(photoPath)
        try {

            var inputStream: InputStream? = null
            var outputStream: OutputStream? = null

            try {
                val fileReader = ByteArray(4096)
                //long fileSize = body.contentLength();
                //long fileSizeDownloaded = 0;

                inputStream = res.byteStream()
                outputStream = FileOutputStream(imageFile)

                while (true) {
                    val read = inputStream!!.read(fileReader)

                    if (read == -1) {
                        break
                    }
                    outputStream!!.write(fileReader, 0, read)
                    //fileSizeDownloaded += read;
                }

                outputStream!!.flush()
            } catch (e: IOException) {
                Log.e(TAG, e.message)
            } finally {
                inputStream?.close()

                if (outputStream != null) {
                    outputStream!!.close()
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, e.message)
        }

        /*photoComment.let {
            saveCommentExif(it)
        }*/
        showFile()
        //setListeners(null, null)
    }

    fun showError(){
        awaitingDialog.dismiss()
        toast("Loading failed")
    }
    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase))
    }

    var cLat = 0.0;
    var cLng = 0.0;

    fun openOptions(){
        val dialog = BottomSheetDialog(this,R.style.SheetDialog)
        //dialog.window.decorView.setBackgroundResource(android.R.color.transparent)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_dialog, null)
        dialog.setContentView(view)
        val linVLocation = view.findViewById<LinearLayout>(R.id.linVLocation);
        val linVShare = view.findViewById<LinearLayout>(R.id.linVShare);
        val linVisit = view.findViewById<LinearLayout>(R.id.linVisit);
        val linCollect = view.findViewById<LinearLayout>(R.id.linCollect);
        val ivCollect = view.findViewById<ImageView>(R.id.ivCollect);
        val tvCollect = view.findViewById<TextView>(R.id.tvCollect);
        val etText = view.findViewById<TextView>(R.id.etText)

        etText.setText(picComment)

        linCollect.setOnClickListener {
            if(click == 0 ){
                ivCollect.setImageResource(R.drawable.collectedd)
                tvCollect.text = "Collected"
                click = 1
            }else{
                ivCollect.setImageResource(R.drawable.collect)
                tvCollect.text = "Collect"
                click = 0
            }
        }

        linVLocation.setOnClickListener {
            //dialog.dismiss()

            try {
                val exif = ExifInterface(photoPath)
                val lat = exif.latLong?.get(0)/* ?: 1.0*/
                val lon = exif.latLong?.get(1)/* ?: 1.0*/
                Log.d("mmmmmmmm","lat ${lat} lng ${lon}")

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

            }catch (e : java.lang.Exception){
                e.printStackTrace()
            }
        }

        linVShare.setOnClickListener {
            //dialog.dismiss()
        }

        linVisit.setOnClickListener {
            //dialog.dismiss()
        }


        dialog.show()
    }

}
