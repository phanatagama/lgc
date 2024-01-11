package com.deepid.lgc.domain.model

import com.deepid.lgc.data.repository.local.entity.CustomerInformationEntity
import java.time.LocalDateTime

data class CustomerInformation(
    val name: String,
    val address: String,
    val issueDate: LocalDateTime,
    val birthDate: LocalDateTime,
    var images: List<DataImage> = emptyList(),
) {
    var id: String? = null

    constructor(
        id: String?,
        name: String,
        address: String,
        issueDate: LocalDateTime,
        birthDate: LocalDateTime,
        images: List<DataImage> = emptyList()
    ) : this(name, address, issueDate, birthDate, images) {
        this.id = id
    }

    fun mapToEntity(): CustomerInformationEntity {
        return CustomerInformationEntity(
            id = id ?: "0",
            name = name,
            address = address,
            issueDate = issueDate,
            birthDate = birthDate
        )
    }
}

val generateCustomerInformation = listOf(
    CustomerInformation(
        "1",
        "John",
        "Seoul",
        LocalDateTime.now(),
        LocalDateTime.now(),
        generateImagePlaceholder
    ), CustomerInformation(
        "2",
        "Doe",
        "Seoul",
        LocalDateTime.now(),
        LocalDateTime.now(),
        generateImagePlaceholder
    )
)