package com.deepscope.deepscope

import android.app.Application
import com.deepscope.deepscope.di.dataModule
import com.deepscope.deepscope.di.networkModule
import com.deepscope.deepscope.di.repositoryModule
import com.deepscope.deepscope.di.viewModelModule
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber


open class DeepScopeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Custom logger
        setupLogger()
        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@DeepScopeApplication)
            modules(
                listOf(
                    networkModule,
                    repositoryModule,
                    dataModule,
                    viewModelModule
                )
            )
        }
    }

    private fun setupLogger() {
        if (BuildConfig.DEBUG) {
            Logger.addLogAdapter(AndroidLogAdapter())
            Timber.plant(
                object : Timber.DebugTree() {
                    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                        Logger.log(priority, tag, message, t)
                    }
                }
            )
        }
    }
}