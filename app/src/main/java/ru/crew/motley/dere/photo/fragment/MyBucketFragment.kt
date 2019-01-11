package ru.crew.motley.dere.photo.fragment

import android.content.Context
import android.graphics.Point
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

import ru.crew.motley.dere.R
import ru.crew.motley.dere.adapter.MyBucketAdapter
import ru.crew.motley.dere.gallery.fragment.GalleryFragment
import ru.crew.motley.dere.networkrequest.RestClient
import ru.crew.motley.dere.networkrequest.feedmodels.MyBucketResponse
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper


class MyBucketFragment : Fragment() {

    private lateinit var rvMyBucket: RecyclerView
    private lateinit var emptyMessage : LinearLayout
    private lateinit var bucketSearch : EditText
    private lateinit var layoutManager : RecyclerView.LayoutManager
    private val SPAN_COUNT = 4
    private lateinit var adapter : MyBucketAdapter


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_my_bucket, container, false)
        rvMyBucket = view.findViewById(R.id.rvMyBucket)
        emptyMessage = view.findViewById(R.id.emptyMessage)
        bucketSearch = view.findViewById(R.id.bucketSearch)
        rvMyBucket.visibility = View.GONE
        layoutManager = GridLayoutManager(requireActivity(),SPAN_COUNT)
        rvMyBucket.layoutManager = layoutManager
        val photoWidth = getColumnWidth()
        adapter = MyBucketAdapter(requireActivity(),photoWidth)
        rvMyBucket.adapter = adapter

        bucketSearch.setOnFocusChangeListener(View.OnFocusChangeListener { view, b ->
            if (!b){
                bucketSearch.clearFocus()
                closeKeyboard(bucketSearch)
            }
        })
       /* emptyMessage.setOnClickListener {
            bucketSearch.clearFocus()
            closeKeyboard(emptyMessage)
        }*/

       /* view.findViewById<LinearLayout>(R.id.constraintLayout).setOnClickListener {
            bucketSearch.clearFocus()
            closeKeyboard(emptyMessage)
        }*/

        //fetchMyBucket()

        return view;
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

    fun fetchMyBucket(){
        var sp = context!!.getSharedPreferences("sp",Context.MODE_PRIVATE)
        var user_id = sp.getString("user_id","")
        RestClient.get().getMyBucket(user_id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it!=null){
                        if (it.status.equals("true",true)){
                            Log.d("myrollresponse",""+ Gson().toJson(it,MyBucketResponse::class.java))
                        }else{
                            Log.d("myrollresponse",""+ Gson().toJson(it,MyBucketResponse::class.java))
                        }
                    }
                },{
                    Log.d("ddddddddddd", it.localizedMessage)
                    Toast.makeText(requireActivity(), "Error ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                })
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

    override fun onAttach(context: Context?) {
        super.onAttach(CalligraphyContextWrapper.wrap(context))
    }

}
