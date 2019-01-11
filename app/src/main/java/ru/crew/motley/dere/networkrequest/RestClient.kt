package ru.crew.motley.dere.networkrequest

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Created by user24 on 18/4/18.
 */
object RestClient {

    private var REST_CLIENT: API? = null
    var retrofitInstance: Retrofit? = null
    var retrofitInstance1 : Retrofit? = null
    private var FB_REST: FacebookGraphAPI? = null

    init {
        setUpRestClient()
        setUpFacebookClient()
    }

    fun get(): API {
        return REST_CLIENT!!
    }

    //http://18.222.103.49/dere/dere_app/index.php/Dere_controller/upload_image

    //http://18.222.103.49/dere/public/


    fun getFB() : FacebookGraphAPI{
        return FB_REST!!
    }

    private fun setUpRestClient() {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY
        val okHttpClient = OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

        //https://getdere.co/dere_app/i
        retrofitInstance = Retrofit.Builder()
                .baseUrl("https://getdere.co/dere_app/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(okHttpClient)
                .build()
        REST_CLIENT = retrofitInstance!!.create(API::class.java)
    }

    private fun setUpFacebookClient(){
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY
        val okHttpClient = OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

        retrofitInstance1 = Retrofit.Builder()
                .baseUrl("https://graph.facebook.com")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(okHttpClient)
                .build()
        FB_REST = retrofitInstance1!!.create(FacebookGraphAPI::class.java)

    }
}