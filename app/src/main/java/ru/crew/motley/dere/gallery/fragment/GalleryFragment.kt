package ru.crew.motley.dere.gallery.fragment

import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.os.Bundle
import android.support.media.ExifInterface
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.google.gson.Gson
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.bottom_nav_bar.*
import kotlinx.android.synthetic.main.fragment_gallery.*
import kotlinx.android.synthetic.main.fragment_gallery.view.*
import kotlinx.android.synthetic.main.item_photo.view.*
import ru.crew.motley.dere.GlideApp
import ru.crew.motley.dere.R
import ru.crew.motley.dere.db.DBLab
import ru.crew.motley.dere.networkrequest.RestClient
import ru.crew.motley.dere.networkrequest.feedmodels.MyRollResponse
import ru.crew.motley.dere.photo.PhotoSaver
import ru.crew.motley.dere.photo.activity.NewPhotoActivity
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper
import java.io.File

class GalleryFragment : Fragment() {

    companion object {
        private const val TAG = "GalleryFragment"
        private const val SPAN_COUNT = 4

        fun newInstance() = GalleryFragment()
    }

    val response: ArrayList<MyRollResponse.Response>? = null

    private val comments by lazy { DBLab.getInstance(activity!!).readAllComments() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_gallery, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //cameraRoll.setImageResource(R.drawable.gray_film_roll_button)
        //greyBucket.setEnabled(true)
        //cameraRoll.setEnabled(false)

        querySearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val wordFilter = s.toString().toLowerCase().trim()
                photoRecycler.adapter?.let {
                    //                    if (wordFilter.isEmpty()) {
//                        (it as PhotoGalleryAdapter).filterBy(null)
//                    } else {
//                        val filteredNames = comments.filter {
//                            it.fileComment.contains(wordFilter)
//                        }
//                                .map { it.fileName }
                    (it as GalleryAdapter).filterBy(wordFilter)
//                    }

                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })



        querySearch.clearFocus()
        querySearch.isCursorVisible = false
        querySearch.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            querySearch.isCursorVisible = hasFocus
            if (!hasFocus){
                closeKeyboard(querySearch)
            }
        }
//        showThumbnails(view)
        //fetchMyRolls()
        /*rollText.setOnClickListener {
            closeKeyboard(querySearch)
            querySearch.clearFocus()
        }

        constraintLayout.setOnClickListener {
            querySearch.clearFocus()
            closeKeyboard(constraintLayout)
        }*/
    }

    fun closeKeyboard(view : View){
        try {
            val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager;
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        }catch (e: java.lang.Exception){
            e.printStackTrace()
        }
    }

    fun fetchMyRolls(){
        var sp = context!!.getSharedPreferences("sp",Context.MODE_PRIVATE)
        var user_id = sp.getString("user_id","")
        RestClient.get().getMyRolls(user_id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it!=null){
                        if (it.status.equals("true",true)){
                            Log.d("myrollresponse",""+Gson().toJson(it,MyRollResponse::class.java))
                            showImages(it.response!!)
                            //response!!.addAll(it.response!!)
                        }
                    }
                },{
                    Log.d("ddddddddddd", it.localizedMessage ?: "error")
                    Toast.makeText(requireActivity(), "Error ${it.localizedMessage ?: "error" }", Toast.LENGTH_SHORT).show()
                })
    }

    fun showImages(datalist : ArrayList<MyRollResponse.Response>){
        val photoWidth = getColumnWidth()
        val adapter = WebGalleryAdapter(datalist, photoWidth)
        //adapter.filterBy(querySearch.text.toString())
        val layoutManager = GridLayoutManager(activity, SPAN_COUNT)
        view?.photoRecycler?.apply {
            this.adapter = adapter
            this.layoutManager = layoutManager
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let { showThumbnails(it) }


    }

    private fun showThumbnails(view: View) {
        val photoFiles = File(PhotoSaver.APP_FOLDER_PATH).listFiles()

        if (photoFiles!=null) {
            photoFiles.sortByDescending {
                it.name.substringAfter("_")
            }
            readCommentsExif(photoFiles)
        }

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
        val photoWidth = getColumnWidth()
        val adapter = GalleryAdapter(commentsWithFiles, photoWidth)
        adapter.filterBy(querySearch.text.toString())
        val layoutManager = GridLayoutManager(activity, SPAN_COUNT)
        view?.photoRecycler?.apply {
            this.adapter = adapter
            this.layoutManager = layoutManager
        }
    }

    private fun getColumnWidth(): Int {
        val wm = context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val point = Point()
        display.getSize(point)
        val screenWidth = point.x
        val columnGap = resources.getDimension(R.dimen.photo_column_padding).toInt()
        return screenWidth / SPAN_COUNT - 2 * columnGap
    }

    inner class GalleryAdapter(
            private val photoFiles: List<Pair<String?, File>>,
            private val photoWidth: Int)
        : RecyclerView.Adapter<PhotoHolder>() {
//        private var fileNames: List<String>? = null

        private var commentHas: String? = null

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_photo, parent, false)
            return PhotoHolder(view)
        }

        override fun getItemCount() = getFilteredPhotoFiles().size

        override fun onBindViewHolder(holder: PhotoHolder, position: Int) {
            holder.bind(getFilteredPhotoFiles()[position], photoWidth,position)
        }

        private fun getFilteredPhotoFiles(): List<File> {
            commentHas?.let { value -> Log.d("BLANK", ""  + value.isBlank()); }
            commentHas?.let { value ->
                return photoFiles.filter { value.isBlank() || it.first?.toLowerCase()?.contains(value.toLowerCase()) ?: false }
                        .map { it.second }
            }
            return photoFiles.map { it.second }
        }

        fun filterBy(commentHas: String?) {
            this.commentHas = commentHas
            notifyDataSetChanged()
        }
    }

    inner class PhotoHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bind(photoFile: File, photoWidth: Int,position: Int) {
            GlideApp.with(this@GalleryFragment)
                    .load(photoFile)
                    .override(photoWidth, photoWidth)
                    .centerCrop()
                    .into(itemView.thumbnail)
            itemView.setOnClickListener {
                querySearch.clearFocus()
                closeKeyboard(querySearch)
                val i = NewPhotoActivity.getIntent(context!!, photoFile.absolutePath,position)
                i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                itemView.context.startActivity(i)
            }
        }
    }


    inner class WebGalleryAdapter(val datalist : ArrayList<MyRollResponse.Response>,val photoWidth : Int) : RecyclerView.Adapter<WPhotoHolder>(){

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WPhotoHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_photo, parent, false)
            return WPhotoHolder(view)
        }

        override fun getItemCount(): Int {
            return datalist.size
        }

        override fun onBindViewHolder(holder: WPhotoHolder, position: Int) {
            //http://getdere.co/dere_app/public/assets/uploads/
            holder.bind("http://getdere.co/dere_app/public/assets/uploads/"+datalist.get(position).image,photoWidth,position)
        }

    }

    inner class WPhotoHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bind(photoFile: String, photoWidth: Int,position: Int) {
            GlideApp.with(this@GalleryFragment)
                    .load(photoFile)
                    .override(photoWidth, photoWidth)
                    .centerCrop()
                    .into(itemView.thumbnail)
            itemView.setOnClickListener {
                /*val i = NewPhotoActivity.getIntent(itemView.context, photoFile.absolutePath,position)
                i.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                itemView.context.startActivity(i)*/
            }
        }
    }



    override fun onAttach(context: Context?) {
        super.onAttach(CalligraphyContextWrapper.wrap(context))
    }

}