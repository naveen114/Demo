package ru.crew.motley.dere.networkrequest.feedmodels

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class MyBucketResponse {

    @SerializedName("status")
    @Expose
    val  status : String? = null
    @SerializedName("message")
    @Expose
    val message : String? = null
    @SerializedName("response")
    @Expose
    val response: List<Response>?= null;

    inner class Response{
        @SerializedName("image_id")
        @Expose
        val imageId: String? = null
        @SerializedName("user_id")
        @Expose
        val userId: String? = null
        @SerializedName("image")
        @Expose
        val image: String? = null
        @SerializedName("image_comment")
        @Expose
        val imageComment: String? = null
        @SerializedName("latitude")
        @Expose
        val latitude: String? = null
        @SerializedName("longitude")
        @Expose
        val longitude: String? = null
        @SerializedName("edit_link")
        @Expose
        val editLink: String? = null
        @SerializedName("created")
        @Expose
        val created: String? = null
    }
}