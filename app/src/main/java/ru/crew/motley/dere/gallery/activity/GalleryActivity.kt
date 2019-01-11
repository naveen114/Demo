package ru.crew.motley.dere.gallery.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.bottom_nav_bar.*
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import ru.crew.motley.dere.R
import ru.crew.motley.dere.gallery.fragment.GalleryFragment
import ru.crew.motley.dere.photo.activity.OurPartnersActivity
import ru.crew.motley.dere.photo.activity.Photo2Activity
import android.widget.Toast
import ru.crew.motley.dere.photo.activity.BucketView
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper


class GalleryActivity : AppCompatActivity() {

    companion object {
        private val TAG = "GalleryActivity"

        fun getIntent(context: Context?) = Intent(context, GalleryActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_container)

        showFragment()
    }

    private fun showFragment() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.container)
        if (currentFragment == null) {
            val fragment = GalleryFragment.newInstance()
            supportFragmentManager.beginTransaction()
                    .add(R.id.container, fragment)
                    .commit()
        }
    }

    fun navClick(v: View) {
        if(!v.isEnabled()) return
        when (v.id) {
            R.id.greyBucket -> startActivity(Intent(this, BucketView::class.java))
            R.id.cameraRoll -> startActivity(Intent( this, GalleryActivity::class.java))
            R.id.openCameraButton -> startActivity(Intent( this, Photo2Activity::class.java))
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase))
    }

}