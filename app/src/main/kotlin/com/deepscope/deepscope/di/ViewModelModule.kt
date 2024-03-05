package com.deepscope.deepscope.di

import com.deepscope.deepscope.ui.customerInformation.CustomerInformationViewModel
import com.deepscope.deepscope.ui.customerInformation.search.SearchCustomerInformationViewModel
import com.deepscope.deepscope.ui.scanner.ScannerViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { ScannerViewModel(get()) }
    viewModel { CustomerInformationViewModel(get(), get()) }
    viewModel { SearchCustomerInformationViewModel(get()) }
}