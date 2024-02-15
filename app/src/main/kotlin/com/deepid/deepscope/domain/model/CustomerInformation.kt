package com.deepid.deepscope.domain.model

import com.deepid.deepscope.data.repository.local.entity.CustomerInformationEntity
import java.time.LocalDateTime

data class CustomerInformation(
    val name: String,
    val description: String,
    val address: String,
    val issueDate: LocalDateTime,
    val birthDate: LocalDateTime,
    var images: List<DataImage> = emptyList(),
) {
    var id: String? = null

    constructor(
        id: String?,
        name: String,
        description: String,
        address: String,
        issueDate: LocalDateTime,
        birthDate: LocalDateTime,
        images: List<DataImage> = emptyList()
    ) : this(name, description, address, issueDate, birthDate, images) {
        this.id = id
    }

    fun mapToEntity(): CustomerInformationEntity {
        return CustomerInformationEntity(
            id = id ?: "0",
            name = name,
            description = description,
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
        "MALE, AGE: 23",
        "Seoul",
        LocalDateTime.now(),
        LocalDateTime.now(),
    ), CustomerInformation(
        "2",
        "Doe",
        "MALE, AGE: 23",
        "Seoul",
        LocalDateTime.now(),
        LocalDateTime.now(),
    )
)