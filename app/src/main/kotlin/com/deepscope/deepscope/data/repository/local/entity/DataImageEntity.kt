package com.deepscope.deepscope.data.repository.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Relation
import com.deepscope.deepscope.domain.model.DataImage


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

data class DataImageWithCustomer(
    @Embedded
    val dataImage: DataImageEntity,
    @Relation(
        parentColumn = "customerId",
        entityColumn = "id"
    )
    val customer: CustomerInformationEntity
)