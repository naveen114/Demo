package ru.crew.motley.dere.utils

import android.os.Build
import android.app.Activity
import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager


class KeyboardUtil(act: Activity, private val contentView: View) {
    private var decorView: View? = null
    //a small helper to allow showing the editText focus
    internal var onGlobalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        val r = Rect()
        //r will be populated with the coordinates of your view that area still visible.
        decorView!!.getWindowVisibleDisplayFrame(r)

        //get screen height and calculate the difference with the useable area from the r
        val height = decorView!!.getContext().getResources().getDisplayMetrics().heightPixels
        val diff = height - r.bottom

        //if it could be a keyboard add the padding to the view
        if (diff != 0) {
            // if the use-able screen height differs from the total screen height we assume that it shows a keyboard now
            //check if the padding is 0 (if yes set the padding for the keyboard)
            if (contentView.getPaddingBottom() !== diff) {
                //set the padding of the contentView for the keyboard
                contentView.setPadding(0, 0, 0, diff)
            }
        } else {
            //check if the padding is != 0 (if yes reset the padding)
            if (contentView.getPaddingBottom() !== 0) {
                //reset the padding of the contentView
                contentView.setPadding(0, 0, 0, 0)
            }
        }
    }

    init {
        this.decorView = act.window.decorView

        //only required on newer android versions. it was working on API level 19
        if (Build.VERSION.SDK_INT >= 19) {
            decorView!!.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener)
        }
    }

    fun enable() {
        if (Build.VERSION.SDK_INT >= 19) {
            decorView!!.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener)
        }
    }

    fun disable() {
        if (Build.VERSION.SDK_INT >= 19) {
            decorView!!.getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener)
        }
    }

    companion object {

        /**
         * Helper to hide the keyboard
         *
         * @param act
         */
        fun hideKeyboard(act: Activity?) {
            if (act != null && act.currentFocus != null) {
                val inputMethodManager = act.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager!!.hideSoftInputFromWindow(act.currentFocus!!.windowToken, 0)
            }
        }
    }
}