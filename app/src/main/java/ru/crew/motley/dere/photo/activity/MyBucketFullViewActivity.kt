package ru.crew.motley.dere.photo.activity

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.BottomSheetDialog
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_my_bucket_full_view.*
import ru.crew.motley.dere.R
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper

class MyBucketFullViewActivity : AppCompatActivity() {

    var click = 0;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_bucket_full_view)
        ivFullView.setOnClickListener {
            openOptions();
        }
    }


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
        }

        linVShare.setOnClickListener {
            //dialog.dismiss()
        }

        linVisit.setOnClickListener {
            //dialog.dismiss()
        }


        dialog.show()
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase))
    }



}
