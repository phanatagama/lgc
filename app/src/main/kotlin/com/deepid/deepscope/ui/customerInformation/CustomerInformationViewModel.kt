package com.deepid.deepscope.ui.customerInformation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepid.deepscope.data.repository.local.provider.CustomerInformationProvider
import com.deepid.deepscope.data.repository.local.provider.DataImageProvider
import com.deepid.deepscope.domain.model.CustomerInformation
import com.deepid.deepscope.domain.model.DataImage
import com.deepid.deepscope.util.IdProviderImpl
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class CustomerInformationViewModel(
    private val idProvider: IdProviderImpl,
    private val customerInformationProvider: CustomerInformationProvider,
    private val dataImageProvider: DataImageProvider
) : ViewModel() {
    private var dataImages: MutableList<DataImage> = mutableListOf()
    private val _customerInformation = MutableLiveData<CustomerInformation>()
    val customerInformation: LiveData<CustomerInformation> = _customerInformation

    fun getCustomerInformationById(id: String) {
        viewModelScope.launch {
            customerInformationProvider.getCustomerInformationById(id).onStart {
                // TODO: set state loading here
            }.catch {
                // TODO: set state error here
            }.collect {
                _customerInformation.value = it.customerInformation.mapToModel()
                    .copy(images = it.images.map { entity -> entity.mapToModel() })
            }

        }
    }

    fun insertCustomerInformation(
        name: String,
        description: String,
        address: String,
        issueDateTime: LocalDateTime,
        birthDateTime: LocalDateTime
    ) {
        val customerInformation = CustomerInformation(
            id = idProvider.generate(),
            name = name,
            description = description,
            address = address,
            issueDate = issueDateTime,
            birthDate = birthDateTime,
            images = dataImages
        )
        _customerInformation.value = customerInformation
        viewModelScope.launch {
            val dataSaved = customerInformationProvider.getCustomerInformation(name).first()
                .filter { it.name == name && it.birthDate == birthDateTime }
            if (dataSaved.isEmpty()) {
                customerInformationProvider.insertCustomerInformation(customerInformation)
                dataImageProvider.insertDataImage(dataImages, customerInformation.id!!)
            } else {
                dataSaved.first().id?.let {
                    dataImageProvider.insertDataImage(
                        dataImages,
                        it
                    )
                }
            }
        }
    }

    fun addImage(bitmap: List<DataImage>) {
        dataImages.clear()
        dataImages.addAll(bitmap)
    }
}