package ru.crew.motley.dere.networkrequest

import io.reactivex.Observable
import retrofit2.http.Field
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import ru.crew.motley.dere.networkrequest.feedmodels.FacebookPageId
import ru.crew.motley.dere.networkrequest.feedmodels.Feed

interface FacebookGraphAPI {

    @GET("/{event_name}")
    fun getPageId(@Path("event_name") eventName : String, @Query("access_token") accessToken : String) : Observable<FacebookPageId>

    @GET("/{page_id}/feed")
    fun getPosts(@Path("page_id") pageId : String , @Query("fields") fields: String, @Query("access_token") accessToken: String) : Observable<Feed>

    //@GET(“/{event_name}”)
    //Observable<FacebookPageId> getPageId(@Path(“event_name”) String eventName, @Query(“access_token”) String accessToken);

    //@GET(“/{page_id}/feed”)
    //Observable<Feed> getPosts(@Path(“page_id”) String pageId, @Query(“fields”) String fields, @Query(“access_token”) String accessToken);
}