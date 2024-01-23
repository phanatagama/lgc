package com.deepid.lgc.data.repository.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import com.deepid.lgc.domain.model.DataImage


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
    val customerId: String
){
    fun mapToModel(): DataImage {
        return DataImage(
            id = id,
            bitmap = null,
            path = path
        )
    }
}
