package com.deepid.lgc.data.repository.network

import com.deepid.lgc.data.repository.network.dto.FileUploadResponseItem
import com.deepid.lgc.data.repository.network.dto.ImageUploadResponse
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.QueryMap
import retrofit2.http.Url

interface MainNetwork {
    @GET("file/upload")
    suspend fun getUploadUrls(@QueryMap params: Map<String, String>): Response<List<FileUploadResponseItem>>

    @PUT
    suspend fun uploadFile(
        @HeaderMap headers: Map<String, String>,
        @Url uploadUrl: String, // it comes from our backend server
        @Body file: RequestBody
    ): Response<Unit>

    @POST
    @Headers("device-address: abcd")
    suspend fun uploadImage(
        @Url uploadUrl: String, // it comes from our backend server
        @Body file: RequestBody
    ): Response<ImageUploadResponse>
}