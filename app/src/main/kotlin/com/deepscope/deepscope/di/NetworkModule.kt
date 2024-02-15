package com.deepscope.deepscope.di

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.deepscope.deepscope.BuildConfig
import com.deepscope.deepscope.data.common.NetworkInterceptor
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


val networkModule = module {
    single { provideInterceptor(androidContext()) }
    single { provideHttpClient(get(), androidContext()) }
    single { provideRetrofit(get()) }
}

fun provideRetrofit(client: OkHttpClient): Retrofit {
    return Retrofit.Builder().apply {
        baseUrl(BuildConfig.BASE_URL)
        addConverterFactory(GsonConverterFactory.create(GsonBuilder().serializeNulls().create()))
        client(client)
    }.build()
}

fun provideHttpClient(networkInterceptor: NetworkInterceptor, applicationContext: Context): OkHttpClient {
//    val hostname = "beta-be.aizi.kr/api/mobile"
//    val certificate = CertificatePinner.Builder()
//        .add(hostname, "sha256/KrPP8OwXCuMt+NE42RM7btRgXsFF6ps8ynA0Rj62j0k=")
//        .add(hostname, "sha256/jQJTbIh0grw0/1TkHSumWb+Fs0Ggogr621gT3PvPKG0=")
//        .add(hostname, "sha256/C5+lpZ7tcVwmwQIMcRtPbsQtWLABXhQzejna0wHFr8M=")
//        .build()
    return OkHttpClient.Builder().apply {
        addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        addInterceptor(
            ChuckerInterceptor.Builder(applicationContext)
                .collector(ChuckerCollector(applicationContext))
                .maxContentLength(250000L)
                .redactHeaders(emptySet())
                .alwaysReadResponseBody(false)
                .build()
        )
        readTimeout(60, TimeUnit.SECONDS)
        writeTimeout(60, TimeUnit.SECONDS)
        connectTimeout(60, TimeUnit.SECONDS)
        addInterceptor(networkInterceptor)
//        certificatePinner(certificate)
    }.build()
}


fun provideInterceptor(applicationContext: Context): NetworkInterceptor {
    return NetworkInterceptor(applicationContext)
}