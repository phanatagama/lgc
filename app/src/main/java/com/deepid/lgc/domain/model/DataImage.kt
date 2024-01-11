package com.deepid.lgc.domain.model

import android.content.Context
import android.graphics.Bitmap
import com.deepid.lgc.data.repository.local.entity.DataImageEntity
import com.deepid.lgc.util.IdProviderImpl
import com.deepid.lgc.util.Utils.saveToGallery

data class DataImage(
    val id: String,
    val bitmap: Bitmap?,
    val path: String?
) {
    constructor(id: String) : this(id, null, null)
    fun mapToEntity(customerId: String): DataImageEntity {
        return DataImageEntity(
            id = id,
            customerId = customerId,
            path = path ?: ""
        )
    }
}

val generateImagePlaceholder: List<DataImage> =
    (1..10).map { DataImage(IdProviderImpl().generate()) }