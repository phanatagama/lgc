package com.deepid.lgc.domain.model

import android.graphics.Bitmap

data class DataImage(
    val id: Int,
    val bitmap: Bitmap?
)

val generateImagePlaceholder: List<DataImage> = (1..10).map { DataImage(it, null) }