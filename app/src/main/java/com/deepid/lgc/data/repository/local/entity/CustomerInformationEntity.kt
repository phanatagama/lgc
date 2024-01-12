package com.deepid.lgc.data.repository.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Relation
import com.deepid.lgc.domain.model.CustomerInformation
import com.deepid.lgc.domain.model.DataImage
import java.time.LocalDateTime

@Entity(tableName = "customer_information", primaryKeys = ["id"])
data class CustomerInformationEntity(
    val id: String = "0",
    val name: String,
    val description: String,
    val address: String,
    val issueDate: LocalDateTime,
    val birthDate: LocalDateTime
) {
    fun mapToModel(): CustomerInformation {
        return CustomerInformation(
            id = id,
            name = name,
            description = description,
            address = address,
            issueDate = issueDate,
            birthDate = birthDate
        )
    }

    companion object {
        const val DEFAULT_ID = "0"
    }
}

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

data class CustomerInformationWithImages(
    @Embedded val customerInformation: CustomerInformationEntity,
    @Relation(
        entity = DataImageEntity::class,
        parentColumn = "id",
        entityColumn = "customerId"
    )
    val images: List<DataImageEntity>
)