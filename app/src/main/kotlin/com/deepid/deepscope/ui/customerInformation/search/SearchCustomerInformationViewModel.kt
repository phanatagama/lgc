package com.deepid.deepscope.ui.customerInformation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deepid.deepscope.data.repository.local.provider.CustomerInformationProvider
import com.deepid.deepscope.domain.model.CustomerInformation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchCustomerInformationViewModel(private val customerInformationProvider: CustomerInformationProvider) :
    ViewModel() {
    private var _state: MutableStateFlow<SearchCustomerInformationState> =
        MutableStateFlow(SearchCustomerInformationState())
    val state: StateFlow<SearchCustomerInformationState> = _state.asStateFlow()

    init {
        getCustomerInformation()
    }

    fun getCustomerInformation() {
        viewModelScope.launch {
            customerInformationProvider.getCustomerInformation().onStart {
                _state.update { it.copy(loading = true) }
            }.catch {
                _state.update { currentState ->
                    currentState.copy(
                        message = it.message,
                        loading = false
                    )
                }
            }.collect {
                _state.update { currentState ->
                    currentState.copy(
                        customerInformation = it,
                        loading = false
                    )
                }
            }
        }
    }

    fun getCustomerInformation(name: String) {
        viewModelScope.launch {
            customerInformationProvider.getCustomerInformation(name).onStart {
                _state.update { it.copy(loading = true) }
            }.catch {
                _state.update { currentState ->
                    currentState.copy(
                        message = it.message,
                        loading = false
                    )
                }
            }.collect {
                _state.update { currentState ->
                    currentState.copy(
                        customerInformation = it,
                        loading = false
                    )
                }
            }
        }
    }
    fun deleteCustomerInformation(customerInformation: CustomerInformation) {
        viewModelScope.launch {
            customerInformationProvider.deleteCustomerInformation(customerInformation)
        }
    }

    fun removeMessage() {
        _state.update { currentState -> currentState.copy(message = null) }
    }
}

data class SearchCustomerInformationState(
    val customerInformation: List<CustomerInformation> = mutableListOf(),
    val message: String? = null,
    val loading: Boolean = true
)
