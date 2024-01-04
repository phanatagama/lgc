package com.deepid.lgc.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import com.regula.common.utils.CameraUtil
import java.io.FileNotFoundException
import java.io.InputStream

class Helpers {
    companion object {
        const val PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 22
        const val PERMISSIONS_REQUEST_CAMERA = 100
        fun drawable(
            @DrawableRes
            id: Int, context: Context): Drawable =
            ResourcesCompat.getDrawable(context.resources, id, context.theme)!!

        fun getColor(
            @ColorRes
            id: Int, context: Context): Int =
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

    }
}
