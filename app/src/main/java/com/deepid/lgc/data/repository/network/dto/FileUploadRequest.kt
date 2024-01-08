package com.deepid.lgc.data.repository.network.dto

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

//    val file: RequestBody
)