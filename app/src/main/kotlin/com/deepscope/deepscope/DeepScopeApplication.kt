package com.deepscope.deepscope

import android.app.Application
import com.deepscope.deepscope.di.dataModule
import com.deepscope.deepscope.di.networkModule
import com.deepscope.deepscope.di.repositoryModule
import com.deepscope.deepscope.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level


open class DeepScopeApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin{
            androidLogger(Level.DEBUG)
            androidContext(this@DeepScopeApplication)
            modules(listOf(
                networkModule,
                repositoryModule,
                dataModule,
                viewModelModule
            ))
        }
    }
}