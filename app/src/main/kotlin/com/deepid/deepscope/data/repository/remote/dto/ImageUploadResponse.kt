package com.deepid.deepscope.data.repository.remote.dto

import com.google.gson.annotations.SerializedName

data class ImageUploadResponse(
	@field:SerializedName("image")
	val image: Image? = null
)

data class Image(
	@field:SerializedName("createdAt")
	val createdAt: String? = null,

	@field:SerializedName("id")
	val id: Int? = null,

	@field:SerializedName("deviceId")
	val deviceId: Int? = null,

	@field:SerializedName("url")
	val url: String? = null,

	@field:SerializedName("hash")
	val hash: String? = null,

	@field:SerializedName("updatedAt")
	val updatedAt: String? = null
)
