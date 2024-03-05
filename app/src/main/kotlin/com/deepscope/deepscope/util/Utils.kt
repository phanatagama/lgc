package com.deepscope.deepscope.util

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.deepscope.deepscope.BuildConfig
import com.deepscope.deepscope.R
import com.deepscope.deepscope.domain.model.TextFieldAttribute
import com.orhanobut.logger.Logger
import com.regula.common.utils.CameraUtil
import com.regula.common.utils.RegulaLog
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.params.Functionality
import kotlinx.parcelize.Parcelize
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
    // Network status
    const val TYPE_WIFI = 1
    const val TYPE_MOBILE = 2
    const val TYPE_NOT_CONNECTED = 3

    // Debounce Query
    const val DEBOUNCE_PERIOD = 500L

    const val REGULA_0326 = "Regula 0326"
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

    fun Bitmap?.resizeBitmap(): Bitmap? {
        if (this != null) {
            val aspectRatio = width.toDouble() / height.toDouble()
            return Bitmap.createScaledBitmap(this, (480 * aspectRatio).toInt(), 480, false)
        }
        return null
    }

    @SuppressLint("Range")
    fun Bitmap.rotateBitmap(contentResolver: ContentResolver, uri: Uri): Bitmap? {
        val orientationColumn =
            arrayOf(MediaStore.Images.Media.ORIENTATION)
        val cur: Cursor? =
            contentResolver.query(uri, orientationColumn, null, null, null)
        var orientation = -1
        if (cur != null && cur.moveToFirst()) {
            orientation = cur.getInt(cur.getColumnIndex(orientationColumn[0]))
        }
        Logger.d("tryOrientation: $orientation")
        val rotationMatrix = Matrix()
        rotationMatrix.setRotate(orientation.toFloat())
        cur?.close()
        return Bitmap.createBitmap(this, 0, 0, width, height, rotationMatrix, true)
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
            Timber.d("File Size : Size " + file.length())
            inputStream.close()
            outputStream.close()
            Timber.d("File Path : Path " + file.path)
            Timber.d("File Size : Size " + file.length())
        } catch (e: Exception) {
            Timber.e("Exception ${e.message!!}")
        }
        return file.path
    }

    fun resetFunctionality(from: Functionality = Functionality()) {
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