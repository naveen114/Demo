package ru.crew.motley.dere.networkrequest

import com.facebook.GraphResponse
import io.reactivex.Observable
import okhttp3.MultipartBody
import retrofit2.Response
import ru.crew.motley.dere.networkrequest.models.ImageUrl
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*
import okhttp3.RequestBody
import ru.crew.motley.dere.networkrequest.feedmodels.*


/**
 * Created by user24 on 18/4/18.
 */
interface API {

    @Multipart
    @POST("index.php/Dere_controller/upload_image")
    fun placesList(@Part image: MultipartBody.Part?): Observable<Response<ImageUrl>>

    //public/assets/uploads/{imagename}" old
    @GET("public/{imagename}")
    fun downloadImage(@Path("imagename") imagename: String): Observable<ResponseBody>

//    fun downloadImage(@Path("str", encoded = false) ): Observable<ResponseBody>
//    fun downloadImage(@Path("imagename") imagename: String): Observable<ResponseBody>


    //http://18.222.103.49/dere/dere_app/index.php/Dere_controller/user_login?device_id=
    @GET("index.php/Dere_controller/user_login")
    fun onLogin(@Query("device_id") device_id : String) : Observable<LoginResponse>

    //index.php/Dere_controller/upload_user_image?user_id=&image=&latitude=&longitude=
    @Multipart
    @POST("index.php/Dere_controller/upload_user_image")
    fun uploadImage(@Part("user_id") user_id: RequestBody, @Part image: MultipartBody.Part?, @Part("latitude") latitude: RequestBody,@Part("longitude") longitude: RequestBody) : Observable<UploadImageResponse>

  /*  fun add_event(@Part("user_id") user_id: RequestBody, @Part("event_description") event_description: RequestBody,
                  @Part("title") title: RequestBody, @Part("location") location: RequestBody,
                  @Part("date") date: RequestBody, @Part("end_date") end_date: RequestBody, @Part("lat") lat: RequestBody,
                  @Part("lng") lng: RequestBody, @Part("country") country: RequestBody,
                  @Part("state") state: RequestBody, @Part file: MultipartBody.Part): Call<SimpleResponse>*/

    //http://18.222.103.49/dere/dere_app/index.php/Dere_controller/all_user_images?user_id=1

    @GET("index.php/Dere_controller/all_user_images")
    fun getMyRolls(@Query("user_id") user_id : String) : Observable<MyRollResponse>

    //http://18.222.103.49/dere/dere_app/index.php/Dere_controller/bucket_Images?user_id=1
    @GET("index.php/Dere_controller/bucket_Images")
    fun getMyBucket(@Query("user_id") user_id : String) : Observable<MyBucketResponse>

    //index.php/Dere_controller/update_image_data?user_id=&image_id=&edit_link=&image_comment=

    @GET("index.php/Dere_controller/update_image_data")
    fun updateDetail(@Query("user_id") user_id : String, @Query("image_id") image_id : String, @Query("edit_link") edit_link : String ,@Query("image_comment") image_comment :String) : Observable<UpdateResponse>



}