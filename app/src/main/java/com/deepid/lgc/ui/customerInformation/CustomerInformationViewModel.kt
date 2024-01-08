package com.deepid.lgc.ui.customerInformation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.deepid.lgc.domain.model.DataImage
import com.deepid.lgc.domain.model.generateImagePlaceholder

class CustomerInformationViewModel : ViewModel() {
    private var _count = 0
    val count = MutableLiveData(_count)

    private var dataImages = mutableListOf<DataImage>()
    private val _images = MutableLiveData<MutableList<DataImage>>()
    val images: LiveData<MutableList<DataImage>> = _images

    init {
        dataImages = generateImagePlaceholder.toMutableList()
        _images.value = dataImages
    }

    fun updateImage(dataImage: DataImage) {
        dataImages.removeAt(dataImage.id - 1)
        Log.d(TAG, "DEBUGX TOTAL data b4: ${dataImages.size}")
        dataImages.add(dataImage.id - 1, dataImage)
        Log.d(TAG, "DEBUGX TOTAL data after: ${dataImages.size}")
        _images.postValue(dataImages)
        Log.d(TAG, "DEBUGX live data update: ${_images.value?.size}")
        Log.d(TAG, "DEBUGX dataImages size: ${dataImages.size}")
    }

    fun addImage(bitmap: DataImage) {
        dataImages.add(bitmap)
        _images.value = dataImages
        _count++
        count.value = _count
    }

    companion object {
        const val TAG: String = "InputViewModel"
    }
}