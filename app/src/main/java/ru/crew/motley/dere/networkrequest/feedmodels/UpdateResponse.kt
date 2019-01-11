package ru.crew.motley.dere.networkrequest.feedmodels

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class UpdateResponse {
    @SerializedName("status")
    @Expose
    val status : String? = null
    @SerializedName("message")
    @Expose
    val message : String? = null
}