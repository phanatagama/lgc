package com.deepscope.deepscope.ui.customerInformation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepscope.deepscope.data.repository.local.provider.CustomerInformationProvider
import com.deepscope.deepscope.data.repository.local.provider.DataImageProvider
import com.deepscope.deepscope.domain.model.CustomerInformation
import com.deepscope.deepscope.domain.model.DataImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import java.util.UUID

class CustomerInformationViewModel(
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
                Timber.d("Start coroutine to getCustomerInformationById")
            }.catch {
                // TODO: set state error here
                Timber.e("Error:", it)
            }.collect {
                _customerInformation.value = it
            }

        }
    }

    /**
     * Insert customer information to database
     * @param name customer name
     * @param description customer description
     * @param address customer address
     * @param issueDateTime customer issue date
     * @param birthDateTime customer birth date
     * Step to insert
     * 1. check if customer information is exist in database
     * 2. if exist, update customer information
     * 3. if not exist, insert customer information
     */
    fun insertCustomerInformation(
        name: String,
        description: String,
        address: String,
        issueDateTime: LocalDateTime,
        birthDateTime: LocalDateTime
    ): Job {
        val customerInformation = CustomerInformation(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            address = address,
            issueDate = issueDateTime,
            birthDate = birthDateTime,
            images = dataImages
        )
        _customerInformation.value = customerInformation
        return viewModelScope.launch {
            val dataSaved = customerInformationProvider.getCustomerInformation(name).first()
                .filter { it.name == name && it.birthDate == birthDateTime }
            if (dataSaved.isEmpty()) {
                Timber.d("Store New Customer Information")
                customerInformationProvider.insertCustomerInformation(customerInformation)
                dataImageProvider.insertDataImage(dataImages, customerInformation.id)
            } else {
                Timber.d("Update Customer Information (add image)")
                dataImageProvider.insertDataImage(
                    dataImages,
                    dataSaved.first().id
                )
            }
        }
    }

    fun addImage(bitmap: List<DataImage>) {
        dataImages.clear()
        dataImages.addAll(bitmap)
    }

    fun deleteImage(customerId: String, dataImage: DataImage) {
        dataImages.remove(dataImage)
        viewModelScope.launch {
            dataImageProvider.deleteDataImage(customerId, dataImage)
        }
    }
}