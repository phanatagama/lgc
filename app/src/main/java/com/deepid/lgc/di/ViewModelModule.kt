package com.deepid.lgc.di

import com.deepid.lgc.ui.customerInformation.CustomerInformationViewModel
import com.deepid.lgc.ui.scanner.ScannerViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { ScannerViewModel(get()) }
    viewModel { CustomerInformationViewModel() }
}