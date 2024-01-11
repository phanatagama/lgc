package com.deepid.lgc.data.repository.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.deepid.lgc.domain.model.CustomerInformation
import java.time.LocalDateTime

@Entity(tableName = "customer_information", primaryKeys = ["id"])
data class CustomerInformationEntity(
    val id: String = "0",
    val name: String,
    val address: String,
    val issueDate: LocalDateTime,
    val birthDate: LocalDateTime
) {
    fun mapToModel(): CustomerInformation {
        return CustomerInformation(
            id = id,
            name = name,
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
)

data class CustomerInformationWithImages(
    @Embedded val customerInformation: CustomerInformationEntity,
    @Relation(
        entity = DataImageEntity::class,
        parentColumn = "id",
        entityColumn = "customerId"
    )
    val images: List<DataImageEntity>
)