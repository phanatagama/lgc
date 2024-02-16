package com.deepscope.deepscope.data.repository.remote.dto

import com.google.gson.annotations.SerializedName

data class FileUploadRequest(
    @SerializedName("type")
    val type: String = "image",

    @SerializedName("mimeType")
    val mimeType: String = "image/jpeg",

    @SerializedName("imageUploadTarget")
    val imageUploadTarget: String = "profile",

    @SerializedName("num")
    val num: String = "1",

    val fileLength: Long
)