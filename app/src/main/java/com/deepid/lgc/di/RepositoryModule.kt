package com.deepid.lgc.di

import com.deepid.lgc.data.repository.MainRepository
import com.deepid.lgc.data.repository.local.dao.AppRoomDatabase
import com.deepid.lgc.data.repository.local.provider.CustomerInformationProvider
import com.deepid.lgc.data.repository.network.MainNetwork
import com.deepid.lgc.util.IdProviderImpl
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidApplication
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
    single { CustomerInformationProvider(Dispatchers.IO, get(), get()) }
    single { IdProviderImpl() }
}

fun provideMainNetwork(retrofit: Retrofit): MainNetwork {
    return retrofit.create(MainNetwork::class.java)
}