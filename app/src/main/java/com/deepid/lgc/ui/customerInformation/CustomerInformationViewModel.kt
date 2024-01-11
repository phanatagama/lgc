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
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class CustomerInformationViewModel(
    private val customerInformationProvider: CustomerInformationProvider,
) : ViewModel() {
    private var dataImages: MutableList<DataImage> = mutableListOf()
    private val _customerInformation = MutableLiveData<CustomerInformation>()
    val customerInformation: LiveData<CustomerInformation> = _customerInformation

    init {
//        val customerId = IdProviderImpl().generate()
//        _customerInformation.value = CustomerInformation(
//            id = customerId,
//            name = "name",
//            address = "",
//            issueDate = LocalDateTime.now(),
//            birthDate = LocalDateTime.now(),
//            images = dataImages
//        )
//        Log.d(TAG, "DEBUGX: INITIALIZE")
    }

    fun insertCustomerInformation(
        name: String,
        address: String,
        issueDateTime: LocalDateTime,
        birthDateTime: LocalDateTime
    ) {
        val customerId = IdProviderImpl().generate()
        val customerInformation = CustomerInformation(
            id = customerId,
            name = name,
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

    fun updateImage(dataImage: DataImage) {
//        dataImages.removeAt(dataImage.id - 1)
//        Log.d(TAG, "DEBUGX TOTAL data b4: ${dataImages.size}")
//        dataImages.add(dataImage.id - 1, dataImage)
//        Log.d(TAG, "DEBUGX TOTAL data after: ${dataImages.size}")
//        _images.postValue(dataImages)
//        Log.d(TAG, "DEBUGX live data update: ${_images.value?.size}")
//        Log.d(TAG, "DEBUGX dataImages size: ${dataImages.size}")
    }

    fun addImage(bitmap: List<DataImage>) {
        dataImages.clear()
        dataImages.addAll(bitmap)
        Log.d(TAG, "addImage: ${dataImages.size}")
//        _customerInformation.value = _customerInformation.value!!.copy(images = dataImages)
//        Log.d(TAG, "addImage: ${_customerInformation.value!!.images.size}")
    }

    companion object {
        const val TAG: String = "InputViewModel"
    }
}