package ru.crew.motley.dere.adapter

import android.content.Context
import android.os.Parcelable
import android.text.Spannable
import android.graphics.Color.parseColor
import android.text.style.ForegroundColorSpan
import android.text.SpannableStringBuilder
import android.widget.TextView
import android.view.ViewGroup
import android.view.LayoutInflater
import android.support.v4.view.PagerAdapter
import android.util.Log
import android.view.View
import android.widget.ImageView
import kotlinx.android.synthetic.main.item_photo.view.*
import ru.crew.motley.dere.GlideApp
import ru.crew.motley.dere.R
import ru.crew.motley.dere.photo.activity.PhotoViewActivity
import java.io.File


class PhotoGalleryAdapter(private val context: Context,private val photoFiles: List<Pair<String?, File>>) : PagerAdapter() {


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