package ru.crew.motley.dere.networkrequest.feedmodels

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class LoginResponse {

    @Expose
    val status: String? = null
    @SerializedName("message")
    @Expose
    val message: String? = null
    @SerializedName("response")
    @Expose
    val response: List<Response>? = null

    inner class Response{
        @SerializedName("user_id")
        @Expose
        val userId: String? = null
        @SerializedName("device_id")
        @Expose
        val deviceId: String? = null
        @SerializedName("created")
        @Expose
        val created: String? = null
    }

}