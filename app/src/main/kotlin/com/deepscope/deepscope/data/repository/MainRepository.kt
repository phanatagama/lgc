package com.deepscope.deepscope.data.repository

import com.deepscope.deepscope.data.common.BaseResult
import com.deepscope.deepscope.data.common.Failure
import com.deepscope.deepscope.data.common.serializeToMap
import com.deepscope.deepscope.data.repository.local.dao.CustomerInformationDao
import com.deepscope.deepscope.data.repository.local.dao.DataImageDao
import com.deepscope.deepscope.data.repository.remote.MainNetwork
import com.deepscope.deepscope.data.repository.remote.dto.FileUploadRequest
import com.deepscope.deepscope.data.repository.remote.dto.FileUploadResponse
import com.deepscope.deepscope.data.repository.remote.dto.ImageUploadResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import timber.log.Timber
import java.io.File

class MainRepository(
    private val ioDispatcher: CoroutineDispatcher,
    private val mainNetwork: MainNetwork,
    private val customerInformationDao: CustomerInformationDao,
    private val dataImageDao: DataImageDao
) : IMainRepository {
    override fun getUploadUrls(
        fileUploadRequest: FileUploadRequest,
        fileRequestBody: RequestBody
    ): Flow<BaseResult<FileUploadResponse, Failure>> =
        flow {
            try {
                val response =
                    mainNetwork.getUploadUrls(fileUploadRequest.serializeToMap() as Map<String, String>)
                if (response.isSuccessful) {
                    Timber.d("getUploadUrls: ${response.body()?.first()?.path}")
                    response.body()?.first()?.url?.let {
                        Timber.d(
                            "getUploadUrls:${response.body()?.first()?.path?.substring(22)} "
                        )
                        mainNetwork.uploadFile(
                            uploadUrl = it,
                            headers = mapOf(
                                "Content-Type" to fileUploadRequest.mimeType,
                                "Content-Length" to fileUploadRequest.fileLength.toString()
                            ),
                            file = fileRequestBody
                        )
                        it
                    }
                    emit(BaseResult.Success(FileUploadResponse(response.body())))
                } else {
                    Timber.e(
                        TAG,
                        "getUploadUrls: ${response.code()},${response.body()},${response}"
                    )
                    emit(BaseResult.Error(Failure(response.code(), response.message())))
                }
            } catch (e: Exception) {
                Timber.e( "getUploadUrls: $e")
                emit(BaseResult.Error(Failure(-1, e.message.toString())))
            }

        }.flowOn(ioDispatcher)

    override fun uploadFile(file: File): Flow<BaseResult<ImageUploadResponse, Failure>> =
        flow {
            Timber.d("[DEBUGX] uploadFile: ")
            val requestBody: RequestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "image",
                    file.name,
                    file.asRequestBody("image/*".toMediaTypeOrNull())
                )
                .build()
            try {
                val result = mainNetwork.uploadImage(
                    "https://logichain-image-server-002a7a2fcab2.herokuapp.com/image",
                    requestBody
                )
                if (result.isSuccessful) {
                    Timber.d("[DEBUGX] uploadFile: Success upload")
                    emit(BaseResult.Success(result.body() as ImageUploadResponse))
                } else {
                    Timber.e( "[DEBUGX] uploadFile: Failed upload from server")
                    emit(BaseResult.Error(Failure(result.code(), result.message())))
                }
            } catch (e: Exception) {
                Timber.e( "[DEBUGX] uploadFile: Failed upload")
                emit(BaseResult.Error(Failure(-1, e.message.toString())))
            }

        }.flowOn(ioDispatcher)


    companion object {
        const val TAG = "MainRepository"
    }

}

interface IMainRepository {
    fun getUploadUrls(
        fileUploadRequest: FileUploadRequest,
        fileRequestBody: RequestBody
    ): Flow<BaseResult<FileUploadResponse, Failure>>

    fun uploadFile(file: File): Flow<BaseResult<ImageUploadResponse, Failure>>
}