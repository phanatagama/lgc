package com.deepid.deepscope.di

import com.deepid.deepscope.ui.customerInformation.CustomerInformationViewModel
import com.deepid.deepscope.ui.customerInformation.search.SearchCustomerInformationViewModel
import com.deepid.deepscope.ui.scanner.ScannerViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { ScannerViewModel(get()) }
    viewModel { CustomerInformationViewModel(get(), get(), get()) }
    viewModel { SearchCustomerInformationViewModel(get()) }
}