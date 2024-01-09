package com.deepid.lgc.domain.model

data class CustomerInformation(
    val id: Int,
    val name: String,
    val address: String,
    val issueDate: String,
    val birthDate: String,
    var images: List<DataImage>?
)

val generateCustomerInformation = listOf(
    CustomerInformation(
        1,
        "John",
        "Seoul",
        "11/01/2024",
        "09/01/2024",
        generateImagePlaceholder
    ), CustomerInformation(
        2,
        "Doe",
        "Seoul",
        "11/01/2024",
        "09/01/2024",
        generateImagePlaceholder
    )
)