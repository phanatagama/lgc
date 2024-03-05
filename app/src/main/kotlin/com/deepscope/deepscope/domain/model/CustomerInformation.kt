package com.deepscope.deepscope.domain.model

import android.os.Parcelable
import com.deepscope.deepscope.data.repository.local.entity.CustomerInformationEntity
import com.deepscope.deepscope.util.Empty
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime
import java.util.UUID

@Parcelize
data class CustomerInformation(
    var id: String = UUID.randomUUID().toString(),
    val name: String? = null,
    val description: String? = null,
    val address: String? = null,
    val issueDate: LocalDateTime = LocalDateTime.now(),
    val birthDate: LocalDateTime = LocalDateTime.now(),
    var images: List<DataImage> = emptyList(),
) : Parcelable {
    fun mapToEntity(): CustomerInformationEntity {
        return CustomerInformationEntity(
            id = id,
            name = name ?: String.Empty,
            description = description ?: String.Empty,
            address = address ?: String.Empty,
            issueDate = issueDate,
            birthDate = birthDate
        )
    }
}


