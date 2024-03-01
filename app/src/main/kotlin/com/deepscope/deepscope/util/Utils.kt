package com.deepscope.deepscope.util

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.deepscope.deepscope.BuildConfig
import com.deepscope.deepscope.R
import com.deepscope.deepscope.data.repository.local.entity.CustomerInformationEntity
import com.deepscope.deepscope.domain.model.CustomerInformation
import com.deepscope.deepscope.domain.model.TextFieldAttribute
import com.deepscope.deepscope.util.Utils.getDrawable
import com.deepscope.deepscope.util.Utils.resizeBitmap
import com.regula.common.utils.CameraUtil
import com.regula.common.utils.RegulaLog
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.enums.eCheckResult
import com.regula.documentreader.api.enums.eGraphicFieldType
import com.regula.documentreader.api.enums.eRPRM_Lights
import com.regula.documentreader.api.enums.eRPRM_ResultType
import com.regula.documentreader.api.enums.eVisualFieldType
import com.regula.documentreader.api.params.Functionality
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.documentreader.api.results.DocumentReaderValidity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import timber.log.Timber
import java.io.File
import java.io.FileDescriptor
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID


object Utils {
    const val TYPE_WIFI = 1
    const val TYPE_MOBILE = 2
    const val TYPE_NOT_CONNECTED = 3
    const val PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 22
    private const val FILENAME_FORMAT = "yyyyMMdd_HHmmss"
    private val timeStamp: String = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(Date())


    fun getImageUri(context: Context): Uri {
        var uri: Uri? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$timeStamp.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/MyCamera/")
            }
            uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
        }
        return uri ?: getImageUriForPreQ(context)
    }

    private fun getImageUriForPreQ(context: Context): Uri {
        val filesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File(filesDir, "/MyCamera/$timeStamp.jpg")
        if (imageFile.parentFile?.exists() == false) imageFile.parentFile?.mkdir()
        return FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            imageFile
        )
    }

    fun uriToBitmap(context: Context, selectedFileUri: Uri): Bitmap? {
        try {
            val parcelFileDescriptor =
                context.contentResolver.openFileDescriptor(selectedFileUri, "r")
            val fileDescriptor: FileDescriptor = parcelFileDescriptor!!.fileDescriptor
            val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            parcelFileDescriptor.close()
            return image
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    fun getDrawable(
        @DrawableRes
        id: Int, context: Context
    ): Drawable =
        ResourcesCompat.getDrawable(context.resources, id, context.theme)!!

    fun getColorResource(
        @ColorRes
        id: Int, context: Context
    ): Int =
        ResourcesCompat.getColor(context.resources, id, context.theme)

    fun getBitmap(
        selectedImage: Uri?,
        targetWidth: Int,
        targetHeight: Int,
        context: Context
    ): Bitmap? {
        val resolver = context.contentResolver
        var inputStream: InputStream? = null
        try {
            inputStream = resolver.openInputStream(selectedImage!!)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(inputStream, null, options)

        try {
            inputStream = resolver.openInputStream(selectedImage!!)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }

        options.inSampleSize =
            CameraUtil.calculateInSampleSize(options, targetWidth, targetHeight)
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeStream(inputStream, null, options)
    }

    fun getConnectivityStatus(context: Context): Int {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager.getNetworkCapabilities(manager.activeNetwork)?.run {
                when {
                    hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> TYPE_WIFI
                    hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> TYPE_MOBILE
                    else -> TYPE_NOT_CONNECTED
                }
            }
        } else {
            manager.activeNetworkInfo?.run {
                when (type) {
                    ConnectivityManager.TYPE_WIFI -> TYPE_WIFI
                    ConnectivityManager.TYPE_MOBILE -> TYPE_MOBILE
                    else -> TYPE_NOT_CONNECTED
                }
            }
        } //연결 X
        return networkInfo ?: TYPE_NOT_CONNECTED
    }

    fun getLicense(context: Context?): ByteArray? {
        if (context == null) return null
        val licInput = context.resources.openRawResource(R.raw.regula)
        val available: Int = try {
            licInput.available()
        } catch (e: IOException) {
            return null
        }
        val license = ByteArray(available)
        try {
            licInput.read(license)
        } catch (e: IOException) {
            return null
        }
        return license
    }

    fun Bitmap.saveBitmap(context: Context): String {
        val wrapper = ContextWrapper(context)
        var file = wrapper.getDir("Images", Context.MODE_PRIVATE)
        file = File(file, "${UUID.randomUUID()}.jpg")
        val stream: OutputStream = FileOutputStream(file)
        this.compress(Bitmap.CompressFormat.JPEG, 25, stream)
        stream.flush()
        stream.close()
        return file.path
    }

    fun resizeBitmap(bitmap: Bitmap?): Bitmap? {
        if (bitmap != null) {
            val aspectRatio = bitmap.width.toDouble() / bitmap.height.toDouble()
            return Bitmap.createScaledBitmap(bitmap, (480 * aspectRatio).toInt(), 480, false)
        }
        return null
    }

    fun getRealPathFromURI(uri: Uri, context: Context): String {
        val returnCursor = context.contentResolver.query(uri, null, null, null, null)
        val nameIndex = returnCursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE)
        returnCursor.moveToFirst()
        val name = returnCursor.getString(nameIndex)
        val size = returnCursor.getLong(sizeIndex).toString()
        val file = File(context.filesDir, name)
        returnCursor.close()
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            var read = 0
            val maxBufferSize = 1 * 1024 * 1024
            val bytesAvailable = inputStream!!.available()

            //int bufferSize = 1024;
            val bufferSize = Math.min(bytesAvailable, maxBufferSize)
            val buffers = ByteArray(bufferSize)
            while (inputStream.read(buffers).also { read = it } != -1) {
                outputStream.write(buffers, 0, read)
            }
            Timber.e("File Size : Size " + file.length())
            inputStream.close()
            outputStream.close()
            Timber.e("File Path : Path " + file.path)
            Timber.e("File Size : Size " + file.length())
        } catch (e: Exception) {
            Timber.e("Exception ${e.message!!}")
        }
        return file.path
    }

    fun setFunctionality(from: Functionality) {
        val to = DocumentReader.Instance().functionality().edit()
        to.setShowChangeFrameButton(from.isShowChangeFrameButton)
        to.setBtDeviceName(from.btDeviceName)
        to.setCameraFrame(from.cameraFrame)
        to.setDatabaseAutoupdate(from.isDatabaseAutoupdate)
        to.setOrientation(from.orientation)
        to.setPictureOnBoundsReady(from.isPictureOnBoundsReady)
        to.setShowCameraSwitchButton(from.isShowCameraSwitchButton)
        to.setShowCaptureButton(from.isShowCaptureButton)
        to.setShowCaptureButtonDelayFromDetect(from.showCaptureButtonDelayFromDetect)
        to.setShowCaptureButtonDelayFromStart(from.showCaptureButtonDelayFromStart)
        to.setShowCloseButton(from.isShowCloseButton)
        to.setShowSkipNextPageButton(from.isShowSkipNextPageButton)
        to.setShowTorchButton(from.isShowTorchButton)
        to.setSkipFocusingFrames(from.isSkipFocusingFrames)
        to.setStartDocReaderForResult(from.startDocReaderForResult)
        try {
            to.setUseAuthenticator(from.isUseAuthenticator)
        } catch (var4: Exception) {
            RegulaLog.e(var4)
        }
        to.setVideoCaptureMotionControl(from.isVideoCaptureMotionControl)
        to.setCaptureMode(from.captureMode)
        to.setDisplayMetadata(from.isDisplayMetaData)
        to.setCameraSize(from.cameraWidth, from.cameraHeight)
        to.setZoomEnabled(from.isZoomEnabled)
        to.setZoomFactor(from.zoomFactor)
        to.setCameraMode(from.cameraMode)
        to.setExcludedCamera2Models(from.excludedCamera2Models)
        to.setIsCameraTorchCheckDisabled(from.isCameraTorchCheckDisabled)
        to.apply()
    }
}

fun List<CustomerInformationEntity>.mapToModel(): List<CustomerInformation> {
    return map {
        it.mapToModel()
    }
}

fun File.mimeType(): String? =
    MimeTypeMap.getSingleton().getMimeTypeFromExtension(this.extension)

fun ContentResolver.readAsRequestBody(uri: Uri): RequestBody =
    object : RequestBody() {
        override fun contentType(): MediaType? =
            this@readAsRequestBody.getType(uri)?.toMediaTypeOrNull()

        override fun writeTo(sink: BufferedSink) {
            this@readAsRequestBody.openInputStream(uri)?.source()?.use(sink::writeAll)
        }

        override fun contentLength(): Long =
            this@readAsRequestBody.query(uri, null, null, null, null)?.use { cursor ->
                val sizeColumnIndex: Int = cursor.getColumnIndex(OpenableColumns.SIZE)
                cursor.moveToFirst()
                cursor.getLong(sizeColumnIndex)
            } ?: super.contentLength()
    }

fun getValidity(
    list: List<DocumentReaderValidity>,
    type: Int?
): Int {
    for (validity in list) {
        if (validity.sourceType == type)
            return validity.status
    }

    return eCheckResult.CH_CHECK_WAS_NOT_DONE
}

fun DocumentReaderResults.toParcelable(
    context: Context,
    faceCaptureImage: Bitmap? = null
): Parcelable {
    val attributes = mutableListOf<TextFieldAttribute>()
    this.textResult?.fields?.forEach {
        val name = it.getFieldName(context)
        Timber.d( "[DEBUGX] fieldname ${it.getFieldName(context)} ")
        for (value in it.values) {
            Timber.d( "[DEBUGX] fieldtype ${value.field.fieldType} ")
            Timber.d( "[DEBUGX] source ${value.sourceType} ")
            Timber.d( "[DEBUGX] value ${value.value} ")
            val valid = getValidity(value.field.validityList, value.sourceType)
            val item = TextFieldAttribute(
                name!!,
                value.value,
                it.lcid,
                value.pageIndex,
                valid,
                value.sourceType
            )
            attributes.add(item)
        }
    }

    val statusDrawable = getDrawable(
        if (this.status.overallStatus == eCheckResult.CH_CHECK_OK) com.regula.documentreader.api.R.drawable.reg_ok else com.regula.documentreader.api.R.drawable.reg_fail,
        context
    )
    val name = this.getTextFieldValueByType(eVisualFieldType.FT_SURNAME_AND_GIVEN_NAMES)
    val gender = this.getTextFieldValueByType(eVisualFieldType.FT_SEX)
    val age = this.getTextFieldValueByType(eVisualFieldType.FT_AGE)
    val ageFieldName =
        this.getTextFieldByType(eVisualFieldType.FT_AGE)?.getFieldName(context)
    val userPhoto = this.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT)
        ?: this.getGraphicFieldImageByType(eGraphicFieldType.GF_DOCUMENT_IMAGE)

    val birth = this.getTextFieldValueByType(eVisualFieldType.FT_DATE_OF_BIRTH)
    val address = this.getTextFieldValueByType(eVisualFieldType.FT_ISSUING_STATE_NAME)
    val expiry = this.getTextFieldValueByType(eVisualFieldType.FT_DATE_OF_EXPIRY)
    val rawImage = this.getGraphicFieldImageByType(
        eGraphicFieldType.GF_PORTRAIT,
        eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE,
        0,
        eRPRM_Lights.RPRM_LIGHT_WHITE_FULL
    )
        ?: this.getGraphicFieldImageByType(
            eGraphicFieldType.GF_DOCUMENT_IMAGE,
            eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE,
        )
    val uvImage = this.getGraphicFieldByType(
        eGraphicFieldType.GF_DOCUMENT_IMAGE,
        eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE, 0, eRPRM_Lights.RPRM_LIGHT_UV
    )?.bitmap
    val documentName = if (this.documentType.isNotEmpty()) {
        Timber.d( "debugx document name ${this.documentType.first().name}")
        Timber.d(

            "debugx document documentid ${this.documentType.first().documentID}"
        )
        Timber.d( "debugx document dtypr ${this.documentType.first().dType}")
        Timber.d(
            "debugx document countty ${this.documentType.first().dCountryName}"
        )
        this.documentType.first().name
    } else {
        "-"
    }
    return DocumentReaderResultsParcel(
        userName = name,
        userDescription = if (ageFieldName != null) "$gender, ${ageFieldName}: $age" else "",
        userAddress = address,
        userDateOfBirth = birth,
        userDateOfIssue = expiry,
        documentName = documentName,
        rawImage = resizeBitmap(rawImage),
        photoImage = resizeBitmap(userPhoto),
        uvImage = resizeBitmap(uvImage),
        faceCaptureImage = resizeBitmap(faceCaptureImage),
        textField = attributes
    )
}

fun CoroutineScope.debounce(
    waitMs: Long = 300L,
    destinationFunction: () -> Unit
): () -> Unit {
    var debounceJob: Job? = null
    return {
        debounceJob?.cancel()
        debounceJob = launch {
            delay(waitMs)
            destinationFunction()
        }
    }
}

@Parcelize
data class DocumentReaderResultsParcel(
    val userName: String? = null,
    val userDescription: String? = null,
    val userAddress: String? = null,
    val userDateOfBirth: String? = null,
    val userDateOfIssue: String? = null,
    val documentName: String? = null,
    val rawImage: Bitmap? = null,
    val photoImage: Bitmap? = null,
    val uvImage: Bitmap? = null,
    val faceCaptureImage: Bitmap? = null,
    val textField: List<TextFieldAttribute>
) : Parcelable