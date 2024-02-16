package com.deepscope.deepscope.di

import com.deepscope.deepscope.data.repository.MainRepository
import com.deepscope.deepscope.data.repository.local.dao.AppRoomDatabase
import com.deepscope.deepscope.data.repository.local.provider.CustomerInformationProvider
import com.deepscope.deepscope.data.repository.local.provider.DataImageProvider
import com.deepscope.deepscope.data.repository.remote.MainNetwork
import com.deepscope.deepscope.util.IdProviderImpl
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit

val repositoryModule = module {
    single(named("IODispatcher")) {
        Dispatchers.IO
    }
    single { MainRepository(get(named("IODispatcher")), get(), get(), get()) }
}

val dataModule = module {
    single { provideMainNetwork(get()) }
    single { AppRoomDatabase.getDatabase(androidContext()).customerInformationDao() }
    single { AppRoomDatabase.getDatabase(androidContext()).dataImageDao() }
    single { CustomerInformationProvider(get(named("IODispatcher")), get()) }
    single { DataImageProvider(get(named("IODispatcher")), get()) }
    single { IdProviderImpl() }
}

fun provideMainNetwork(retrofit: Retrofit): MainNetwork {
    return retrofit.create(MainNetwork::class.java)
}