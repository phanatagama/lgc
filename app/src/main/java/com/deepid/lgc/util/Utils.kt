package com.deepid.lgc.util

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.deepid.lgc.R
import com.deepid.lgc.data.model.TextFieldAttribute
import com.deepid.lgc.ui.defaultscanner.DefaultScannerActivity
import com.regula.documentreader.api.enums.eCheckResult
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.documentreader.api.results.DocumentReaderValidity
import kotlinx.parcelize.Parcelize
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.UUID


object Utils {

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

    fun saveToGallery(context: Context?, bitmap: Bitmap) {
        val filename = "${UUID.randomUUID()}.jpg"
        //Output stream
        var fos: OutputStream? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //getting the contentResolver
            context?.contentResolver?.also { resolver ->

                //Content resolver will process the contentvalues
                val contentValues = ContentValues().apply {

                    //putting file information in content values
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }

                //Inserting the contentValues to contentResolver and getting the Uri
                val imageUri: Uri? =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                //Opening an outputstream with the Uri that we got
                fos = imageUri?.let { resolver.openOutputStream(it) }
            }
        } else {
            //These for devices running on android < Q
            //So I don't think an explanation is needed here
            val imagesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)
        }

        fos?.use {
            //Finally writing the bitmap to the output stream that we opened
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            Toast.makeText(
                context,
                "Image Save to Gallery",
                Toast.LENGTH_LONG
            ).show()
        }
//        val wrapper = ContextWrapper(context)
//        var file = wrapper.getDir("Images", Context.MODE_PRIVATE)
//        file = File(file, "${UUID.randomUUID()}.jpg")
//        val stream: OutputStream = FileOutputStream(file)
//        bitmap.compress(Bitmap.CompressFormat.JPEG, 25, stream)
//        stream.flush()
//        stream.close()
//        return file

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
        val sizeIndex = returnCursor!!.getColumnIndex(OpenableColumns.SIZE)
        returnCursor!!.moveToFirst()
        val name = returnCursor!!.getString(nameIndex)
        val size = java.lang.Long.toString(returnCursor!!.getLong(sizeIndex))
        val file = File(context.filesDir, name)
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            var read = 0
            val maxBufferSize = 1 * 1024 * 1024
            val bytesAvailable = inputStream!!.available()

            //int bufferSize = 1024;
            val bufferSize = Math.min(bytesAvailable, maxBufferSize)
            val buffers = ByteArray(bufferSize)
            while (inputStream!!.read(buffers).also { read = it } != -1) {
                outputStream.write(buffers, 0, read)
            }
            Log.e("File Size", "Size " + file.length())
            inputStream!!.close()
            outputStream.close()
            Log.e("File Path", "Path " + file.path)
            Log.e("File Size", "Size " + file.length())
        } catch (e: Exception) {
            Log.e("Exception", e.message!!)
        }
        return file.path
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

    return eCheckResult.CH_CHECK_WAS_NOT_DONE;
}

fun DocumentReaderResults.toParcelable(context: Context): Parcelable {
    val attributes = mutableListOf<TextFieldAttribute>()
    this.textResult?.fields?.forEach {
        val name = it.getFieldName(context)
        Log.d(DefaultScannerActivity.TAG, "[DEBUGX] fieldname ${it.getFieldName(context)} ")
        for (value in it.values) {
            Log.d(DefaultScannerActivity.TAG, "[DEBUGX] fieldtype ${value.field.fieldType} ")
            Log.d(DefaultScannerActivity.TAG, "[DEBUGX] source ${value.sourceType} ")
            Log.d(DefaultScannerActivity.TAG, "[DEBUGX] value ${value.value} ")
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
    return DocumentReaderResultsParcel(attributes)
}

@Parcelize
data class DocumentReaderResultsParcel(val textField: List<TextFieldAttribute>) :
    Parcelable