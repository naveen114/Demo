package ru.crew.motley.dere.photo.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.annotation.RequiresApi
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.facebook.*
import ru.crew.motley.dere.R
import ru.crew.motley.dere.photo.MyAdapter
import org.json.JSONArray
import org.json.JSONObject
import ru.crew.motley.dere.gallery.activity.GalleryActivity
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.widget.LoginButton
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.facebook.FacebookCallback
import com.facebook.login.LoginManager
import java.util.*
import java.util.Arrays.asList
import com.facebook.AccessToken
import com.google.gson.Gson
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_our_partners.view.*

import ru.crew.motley.dere.adapter.FeedAdapter
import ru.crew.motley.dere.networkrequest.RestClient
import ru.crew.motley.dere.networkrequest.feedmodels.FacebookPageId
import ru.crew.motley.dere.networkrequest.feedmodels.Feed
import ru.crew.motley.dere.networkrequest.feedmodels.FeedItem
import ru.crew.motley.dere.photo.fragment.MyBucketFragment
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper


class OurPartnersActivity() : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var webView: WebView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var bucketSearch : EditText
    var sp: SharedPreferences? = null
    var myDataset = JSONArray()
    private var callbackManager: CallbackManager? = null
    private var fbToken: AccessToken? = null
    companion object {
        const val EMAIL: String = "email"
    }
    val listdata = ArrayList<String>()
    lateinit var fbButtonContainer : LinearLayout
    lateinit var recyclerOurPartners : LinearLayout

//    var myDataset = jsonArray as Iterable<JSONObject>


    fun getFBPhotos() {
//        val accesstoken = "321147251797228" + "|" + "6751d8bd94b29b36b2c09811779b4bac"
//        val token = AccessToken(accesstoken,"321147251797228","728344120560861",null,null,null,null,null,null)
        val request = GraphRequest.newGraphPathRequest(AccessToken.getCurrentAccessToken(), "/dereapp?fields="+getString(R.string.fields),
                GraphRequest.Callback {
                    fun onCompleted(response: GraphResponse){
                        myDataset = response.jsonArray
                        var json : JSONObject  = response.getJSONObject();
                        Log.e("Facebook Response", ":" + json);
                    }
                })
        val parameters = Bundle()
        parameters.putString("fields", getString(R.string.fields))
        request.setParameters(parameters)
        request.parameters = parameters
        request.executeAsync()


        if (myDataset != null) {
            for (i in 0 until myDataset.length()) {
                listdata.add(myDataset.getString(i))
            }
        }


    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view =  inflater.inflate(R.layout.activity_our_partners, container, false)
        sp = context?.getSharedPreferences("sp",Context.MODE_PRIVATE)

        val accessToken = AccessToken.getCurrentAccessToken()
        val isLoggedIn = accessToken != null && !accessToken.isExpired
        recyclerView = view.findViewById(R.id.my_recycler_view)
        bucketSearch = view.findViewById(R.id.bucketSearch)
        webView = view.findViewById(R.id.webview)

        fbButtonContainer = view.findViewById(R.id.fbButtonContainer)
        recyclerOurPartners = view.findViewById(R.id.recyclerOurPartners)

        bucketSearch.setOnFocusChangeListener(View.OnFocusChangeListener { view, b ->
            if (!b){
                bucketSearch.clearFocus()
                closeKeyboard(bucketSearch)
            }
        })

        //webview.settings.setJavaScriptEnabled(true);
            webView.settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
            webView.setWebViewClient( MyWebViewClient());
            webView.setWebChromeClient( WebChromeClient() );

            webView.settings.setJavaScriptEnabled(true);
            //webView.getSettings().setPluginState(WebSettings.PluginState.ON);
            //webView.getSettings().setBuiltInZoomControls(true);
            //webView.getSettings().setSupportZoom(true);

            webView.loadUrl("http://getdere.co/communities-app/");

/*
        view.findViewById<LinearLayout>(R.id.constraintLayout).setOnClickListener {
            bucketSearch.clearFocus()
            closeKeyboard(view.findViewById<LinearLayout>(R.id.constraintLayout))
        }

        fbButtonContainer.setOnClickListener {
            bucketSearch.clearFocus()
            closeKeyboard(fbButtonContainer)
        }

        recyclerOurPartners.setOnClickListener {
            bucketSearch.clearFocus()
            closeKeyboard(recyclerOurPartners)
        }*/


        //LoginManager.getInstance().logOut()

//        val listdata = arrayOf("Vietnam Backpacker","Vietnam Backpacker","Vietnam Backpacker","Backpacking Central America","Backpacking Central America","Backpacking Central America","Backpacking Central America","Backpacking Central America","Backpacking Central America")

        //LoginManager.getInstance().logOut()

        //openFunctions()

       /* if(isLoggedIn){
            Log.d("facebooook","login")
            getFBPhotos()
            view.fbButtonContainer.visibility = GONE
            view.recyclerOurPartners.visibility = VISIBLE
            Log.d("fbToken",sp!!.getString("access_token",""))
        }else{
            Log.d("facebooook","notlogin")
            AccessToken.setCurrentAccessToken(null)
            LoginManager.getInstance().logOut()
            //fbButtonContainer.visibility = VISIBLE
            //mychangedss comment above
            view.fbButtonContainer.visibility = VISIBLE
            view.recyclerOurPartners.visibility = GONE
            //return
        }*/
        recyclerOurPartners.visibility = VISIBLE
        recyclerView.visibility = GONE

        viewManager = LinearLayoutManager(requireActivity(),LinearLayoutManager.VERTICAL,false)
        recyclerView.layoutManager = viewManager

        //viewAdapter = MyAdapter(listdata)

        /* recyclerView = findViewById<RecyclerView>(R.id.my_recycler_view).apply {
             // use this setting to improve performance if you know that changes
             // in content do not change the layout size of the RecyclerView
             setHasFixedSize(true)

             // use a linear layout manager
             layoutManager = viewManager

             // specify an viewAdapter (see also next example)
             adapter = viewAdapter

         }*/

        var loginButton: LoginButton? = view.findViewById<LoginButton>(R.id.login_button)
//        loginButton?.setReadPermissions(Arrays.asList(EMAIL))
        // If you are using in a fragment, call loginButton.setFragment(this);

        // Callback registration
        loginButton?.setOnClickListener(View.OnClickListener {
            Log.d("ddddddddd","call")
            callbackManager = CallbackManager.Factory.create()
            LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile","email"))
            LoginManager.getInstance().registerCallback(callbackManager,
                    object : FacebookCallback<LoginResult>{
                        override fun onSuccess(loginResult: LoginResult){
                            Log.d("ddddddddd","ss"+loginResult.accessToken)
                            Log.d("accesstoken","fb token" + loginResult.accessToken.token)
                            Log.d("Our Partners Activity","FB Token" + loginResult.accessToken.token)
                            fbToken = loginResult.accessToken
                            sp!!.edit().putString("access_token",loginResult.accessToken.token).apply()
                            openFunctions()
                        }
                        override fun onCancel(){
                            Log.d("ddddddddd","oncancel")
                        }
                        override fun onError(error: FacebookException){
                            Log.d("ddddddddd",error.localizedMessage)
                        }
                    })
        })


        view.myBucketText.setOnClickListener { loadHome() }


        return view
    }

     inner class MyWebViewClient : WebViewClient() {
         override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
             if( url!!.startsWith("getdere://:") || url!!.startsWith("https:") ) {
                 return false;
             }
             return true
         }

         override fun onPageFinished(view: WebView?, url: String?) {
             super.onPageFinished(view, url)
         }
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

    fun openFunctions(){
        val accessToken = AccessToken.getCurrentAccessToken()
        val isLoggedIn = accessToken != null && !accessToken.isExpired

        if(isLoggedIn){
            Log.d("facebooook","login")
            getFBPhotos()
            fbButtonContainer.visibility = GONE
            recyclerOurPartners.visibility = VISIBLE
            Log.d("fbToken",sp!!.getString("access_token",""))
            getCall()
        }else{
            Log.d("facebooook","notlogin")
            AccessToken.setCurrentAccessToken(null)
            LoginManager.getInstance().logOut()
            //fbButtonContainer.visibility = VISIBLE
            //mychangedss comment above
            fbButtonContainer.visibility = VISIBLE
            recyclerOurPartners.visibility = GONE
            //return
        }
    }



    fun loadHome(){
        //(BucketView).loadHome()
        requireActivity().supportFragmentManager.beginTransaction().replace(R.id.fcontainer, MyBucketFragment()).commitAllowingStateLoss()
    }

    /*
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_our_partners)
        val accessToken = AccessToken.getCurrentAccessToken()
        val isLoggedIn = accessToken != null && !accessToken.isExpired
        recyclerView = findViewById(R.id.my_recycler_view)

//        val listdata = arrayOf("Vietnam Backpacker","Vietnam Backpacker","Vietnam Backpacker","Backpacking Central America","Backpacking Central America","Backpacking Central America","Backpacking Central America","Backpacking Central America","Backpacking Central America")

        if(isLoggedIn){
            Log.d("facebooook","login")
            getFBPhotos()
            fbButtonContainer.visibility = GONE
            recyclerOurPartners.visibility = VISIBLE
        }else{
            Log.d("facebooook","notlogin")
            AccessToken.setCurrentAccessToken(null)
            LoginManager.getInstance().logOut()
            //fbButtonContainer.visibility = VISIBLE
            //mychangedss comment above
            fbButtonContainer.visibility = GONE
            recyclerOurPartners.visibility = VISIBLE
            //return
        }
        viewManager = LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false)
        recyclerView.layoutManager = viewManager

        //viewAdapter = MyAdapter(listdata)

       /* recyclerView = findViewById<RecyclerView>(R.id.my_recycler_view).apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter

        }*/

        var loginButton: Button? = findViewById<Button>(R.id.login_button)
//        loginButton?.setReadPermissions(Arrays.asList(EMAIL))
        // If you are using in a fragment, call loginButton.setFragment(this);

        // Callback registration
        loginButton?.setOnClickListener(View.OnClickListener {
            callbackManager = CallbackManager.Factory.create()
            LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile", "email"))
            LoginManager.getInstance().registerCallback(callbackManager,
                    object : FacebookCallback<LoginResult>{
                        override fun onSuccess(loginResult: LoginResult){
                            Log.d("accesstoken","fb token" + loginResult.accessToken.token)
                            Log.d("Our Partners Activity","FB Token" + loginResult.accessToken.token)
                            fbToken = loginResult.accessToken
                        }
                        override fun onCancel(){
                            Log.d("cancel","oncancel")
                        }
                        override fun onError(error: FacebookException){
                            Log.d("errrr",error.message)
                        }
                    })
        })

        getCall()
    }
    */

    //https://graph.facebook.com/210522059724743/feed?fields=id,full_picture,message,story,created_time,link,comments,attachments,picture,shares,likes.summary(true)&access_token=1786614831603417|DWqP48Lm77hBqCQDWVsq4lFixuo


    //mychanges
    fun getCall() {
        try {
            //AndroidHive

            RestClient.getFB().getPageId("dereapp",
                    /*resources.getString(R.string.facebook_access_token)*/sp!!.getString("access_token",""))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    /*.subscribe({ facebookPageId ->
                        val id = facebookPageId.id
                        Log.d("fffffbbbb",id);
                        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
                        getFeed(id);
                        //sharedPreferences.edit().putString(ConstantStrings.FACEBOOK_PAGE_ID, id).apply()
                    })*/

                    .subscribe(this:: getRespId, this :: handleError)



        }catch(e : Exception){
            e.printStackTrace()
        }
    }


    fun getRespId(facebookPageId : FacebookPageId){
        val id = facebookPageId.id
        Log.d("fffffbbbb",id);
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity())
        getFeed(id);
    }

    fun getFeed(pageId : String?){
        try {

            RestClient.getFB().getPosts(pageId!!,getString(R.string.fields),/*getString(R.string.facebook_access_token)*/sp!!.getString("access_token",""))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this:: handleResponse, this :: handleError)

        }catch (e : Exception) {
            e.printStackTrace()
        }
    }

    val alldata = ArrayList<FeedItem>()

    fun handleResponse(feed : Feed){
        Log.d("ddddddddddd", feed.data.toString());
        Log.d("ddddddddddd", "" + (feed.data?.size ?: 0));
        if (feed.data != null) {
            if (feed.data.size>0){
                recyclerView.adapter = FeedAdapter(requireActivity(),feed.data)
            }
            for (i in 0 until feed.data.size) {
                Log.d("ddddddddddd", Gson().toJson(feed.data.get(i)))
                //Log.d("dddddddddddImage",feed.data.get(i).full_picture)
            }

        }

    }

    fun handleError(error: Throwable){
        Log.d("ddddddddddd", error.localizedMessage)
        Toast.makeText(requireActivity(), "Error ${error.localizedMessage}", Toast.LENGTH_SHORT).show()
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager?.onActivityResult(requestCode, resultCode, data)

    }

   /* fun onClick(v: View) {
        when (v.id) {
            R.id.greyBucket -> startActivity(Intent(this, BucketView::class.java))
            R.id.cameraRoll -> startActivity(Intent( this, GalleryActivity::class.java))
            R.id.openCameraButton -> startActivity(Intent( this, Photo2Activity::class.java))
        }
    }*/

    override fun onAttach(context: Context?) {
        super.onAttach(CalligraphyContextWrapper.wrap(context))
    }

}
