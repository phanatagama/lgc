package com.deepscope.deepscope.domain.model

import android.graphics.Bitmap
import android.os.Parcelable
import com.deepscope.deepscope.data.repository.local.entity.DataImageEntity
import com.deepscope.deepscope.util.Empty
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class DataImage(
    val id: String = UUID.randomUUID().toString(),
    val bitmap: Bitmap? = null,
    val type: Int = 1,
    val path: String? = null,
) : Parcelable {
    fun mapToEntity(customerId: String): DataImageEntity {
        return DataImageEntity(
            id = id,
            customerId = customerId,
            type = type,
            path = path ?: String.Empty
        )
    }

}