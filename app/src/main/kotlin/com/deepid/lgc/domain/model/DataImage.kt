package com.deepid.lgc.domain.model

import android.graphics.Bitmap
import com.deepid.lgc.data.repository.local.entity.DataImageEntity

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