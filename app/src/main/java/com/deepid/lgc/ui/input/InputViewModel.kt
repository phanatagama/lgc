package com.deepid.lgc.ui.input

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class InputViewModel : ViewModel() {
    private var _count = 0
    val count = MutableLiveData(_count)

    private val dataImages = mutableListOf<Bitmap>()
    private val _images = MutableLiveData<MutableList<Bitmap>>()
    val images: LiveData<MutableList<Bitmap>> = _images

    fun addImage(bitmap: Bitmap){
        dataImages.add(bitmap)
        _images.value = dataImages
        _count++
        count.value = _count
    }

    companion object{
        const val TAG: String = "InputViewModel"
    }
}