package ru.crew.motley.dere.photo.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import ru.crew.motley.dere.R
import ru.crew.motley.dere.photo.fragment.Photo2Fragment
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper


class Photo2Activity : AppCompatActivity() {

    companion object {
        fun getIntent(context: Context): Intent {
            val i = Intent(context, Photo2Activity::class.java)
            return i
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        setContentView(R.layout.activity_container)
       if (null == savedInstanceState) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, Photo2Fragment.newInstance())
                    .commit()
        }
    }

    public override fun onNewIntent(intent: Intent) {
        this.intent = intent
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("Photo2Activity", "$requestCode, $resultCode")
        supportFragmentManager.findFragmentById(R.id.container)?.let {
            it.onActivityResult(requestCode, resultCode, data)
        }

    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase))
    }

}