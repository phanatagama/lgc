package com.deepscope.deepscope.ui.scanner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepscope.deepscope.data.common.BaseResult
import com.deepscope.deepscope.data.repository.MainRepository
import com.deepscope.deepscope.data.repository.remote.dto.ImageUploadResponse
import com.orhanobut.logger.Logger
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.facesdk.model.results.FaceCaptureResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.io.File

class ScannerViewModel(private val mainRepository: MainRepository) : ViewModel() {
    private val _documentReaderResultLiveData = MutableLiveData<DocumentReaderResults?>()
    val documentReaderResultLiveData : LiveData<DocumentReaderResults?> = _documentReaderResultLiveData

    private val _faceCaptureResponseLiveData = MutableLiveData<FaceCaptureResponse?>()
    val faceCaptureResponse: LiveData<FaceCaptureResponse?> = _faceCaptureResponseLiveData

    fun setDocumentReaderResults(results: DocumentReaderResults?){
        Logger.d( "[DEBUGX] setDocumentReaderResults: performed ")
        _documentReaderResultLiveData.value = results
    }

    fun setFaceCaptureResponse(results: FaceCaptureResponse?){
        _faceCaptureResponseLiveData.value = results
    }

    private val _state = MutableStateFlow<ScannerUiState>(ScannerUiState.Init)
    val state: StateFlow<ScannerUiState> get() = _state.asStateFlow()

    private fun showLoading() {
        _state.value = ScannerUiState.Loading(true)
    }

    private fun hideLoading() {
        _state.value = ScannerUiState.Loading(false)
    }

    private fun setError(message: String) {
        _state.value = ScannerUiState.Error(message)
    }

    private fun setSuccess(data: ImageUploadResponse) {
        _state.value = ScannerUiState.Success(data)
    }

    fun uploadImage(file: File) {
        viewModelScope.launch {
            mainRepository.uploadFile(file).onStart {
                showLoading()
            }.catch {
                hideLoading()
            }.collect { result ->
                hideLoading()
                when (result) {
                    is BaseResult.Error -> setError(result.err.message)
                    is BaseResult.Success -> setSuccess(result.data)
                }
            }
        }
    }


    companion object{
        const val TAG: String = "ScannerViewModel"
    }
}

sealed interface ScannerUiState {
    object Init : ScannerUiState
    data class Loading(val isLoading: Boolean) : ScannerUiState
    data class Error(val message: String) : ScannerUiState
    data class Success(val data: ImageUploadResponse) : ScannerUiState
}