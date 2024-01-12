package com.deepid.lgc.ui.customerInformation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepid.lgc.data.repository.local.provider.CustomerInformationProvider
import com.deepid.lgc.domain.model.CustomerInformation
import com.deepid.lgc.domain.model.DataImage
import com.deepid.lgc.util.IdProviderImpl
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class CustomerInformationViewModel(
    private val customerInformationProvider: CustomerInformationProvider,
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
        val customerId = IdProviderImpl().generate()
        val customerInformation = CustomerInformation(
            id = customerId,
            name = name,
            description = description,
            address = address,
            issueDate = issueDateTime,
            birthDate = birthDateTime,
            images = dataImages
        )
        _customerInformation.value = customerInformation
        Log.d(TAG, "insertCustomerInformation: ${customerInformation.id}")
        viewModelScope.launch {
            customerInformationProvider.insertCustomerInformation(customerInformation)
            customerInformationProvider.insertDataImage(dataImages, customerInformation.id!!)
        }
    }

    fun addImage(bitmap: List<DataImage>) {
        dataImages.clear()
        dataImages.addAll(bitmap)
    }

    companion object {
        const val TAG: String = "InputViewModel"
    }
}