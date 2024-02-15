package com.deepid.deepscope.data.repository.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import com.deepid.deepscope.domain.model.DataImage


@Entity(
    tableName = "data_image", primaryKeys = ["id"], foreignKeys = [
        ForeignKey(
            entity = CustomerInformationEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DataImageEntity(
    val id: String = "0",
    val path: String,
    val type: Int = 1,
    val customerId: String
){
    fun mapToModel(): DataImage {
        return DataImage(
            id = id,
            bitmap = null,
            type = type,
            path = path
        )
    }
}
