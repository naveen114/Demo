package ru.crew.motley.dere.photo.activity

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.support.design.widget.BottomSheetBehavior
import android.support.media.ExifInterface
import android.support.v7.widget.GridLayoutManager
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_new_photo.*
import kotlinx.android.synthetic.main.bottomsheetoption.*
import kotlinx.android.synthetic.main.fragment_gallery.*
import kotlinx.android.synthetic.main.fragment_gallery.view.*
import ru.crew.motley.dere.GlideApp
import ru.crew.motley.dere.R
import ru.crew.motley.dere.adapter.PhotoGalleryAdapter
import ru.crew.motley.dere.gallery.fragment.GalleryFragment
import ru.crew.motley.dere.photo.PhotoSaver
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper
import ru.crew.motley.dere.utils.KeyboardUtil
import java.io.File
import java.text.FieldPosition


var bitmapSingleton: Bitmap? = null

class NewPhotoActivity : PhotoViewActivity() {

    companion object {
        private const val TAG = "NewPhotoActivity"

        private const val EXTRA_CURRENT_LAT = "latitude"
        private const val EXTRA_CURRENT_LON = "longitude"
        private const val EXTRA_PATH = "fullPhotoPath"
        private const val IMAGE_POS = "pos"

        fun getIntent(context: Context?, fullPhotoPath: String,position: Int) =
                Intent(context, NewPhotoActivity::class.java)
                        .apply {
                            putExtra(EXTRA_PATH, fullPhotoPath)
                            putExtra(IMAGE_POS,position)
//                            putExtra(EXTRA_CURRENT_LAT, currentLat)
//                            putExtra(EXTRA_CURRENT_LON, currentLon)
                        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideBars()
        setContentView(R.layout.activity_new_photo)

        photoPath = intent.getStringExtra(EXTRA_PATH)
        selectedImage = intent.getIntExtra(IMAGE_POS,0)
        Log.d("selectedImage",""+selectedImage)
        Log.d("selectedImage",""+photoPath)
        GlideApp.with(this)
                .load(intent.getStringExtra(EXTRA_PATH))
                .into(newPhoto)
        val currentLat = intent.getDoubleExtra(EXTRA_CURRENT_LAT, -1.0)
        val currentLon = intent.getDoubleExtra(EXTRA_CURRENT_LON, -1.0)
        showThumbnails()
        loadComment()
        setListeners(currentLat, currentLon)

    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase))
    }








}