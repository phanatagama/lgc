package com.deepscope.deepscope.domain.model

import android.graphics.Bitmap
import com.deepscope.deepscope.data.repository.local.entity.DataImageEntity

data class DataImage(
    val id: String,
    val bitmap: Bitmap?,
    val type: Int,
    val path: String?
) {
    constructor(id: String) : this(id, null, 1, null)
    fun mapToEntity(customerId: String): DataImageEntity {
        return DataImageEntity(
            id = id,
            customerId = customerId,
            type = type,
            path = path ?: ""
        )
    }

}