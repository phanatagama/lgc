package com.deepscope.deepscope.data.repository.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Relation
import com.deepscope.deepscope.domain.model.CustomerInformation
import java.time.LocalDateTime
import java.util.UUID

@Entity(tableName = "customer_information", primaryKeys = ["id"])
data class CustomerInformationEntity(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val address: String,
    val issueDate: LocalDateTime = LocalDateTime.now(),
    val birthDate: LocalDateTime = LocalDateTime.now(),
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
}

data class CustomerInformationWithImages(
    @Embedded val customerInformation: CustomerInformationEntity,
    @Relation(
        entity = DataImageEntity::class,
        parentColumn = "id",
        entityColumn = "customerId"
    )
    val images: List<DataImageEntity>
){
    fun mapToModel(): CustomerInformation {
        return customerInformation.mapToModel().copy(
            images = images.map { it.mapToModel() }
        )
    }
}