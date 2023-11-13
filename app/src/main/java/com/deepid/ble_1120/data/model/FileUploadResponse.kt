package com.deepid.lgc.data.model

import com.google.gson.annotations.SerializedName


data class FileUploadResponse(

    @field:SerializedName("FileUploadResponse")
    val fileUploadResponse: List<FileUploadResponseItem?>? = null
)


data class FileUploadResponseItem(

    @field:SerializedName("path")
    val path: String? = null,

    @field:SerializedName("url")
    val url: String? = null
)