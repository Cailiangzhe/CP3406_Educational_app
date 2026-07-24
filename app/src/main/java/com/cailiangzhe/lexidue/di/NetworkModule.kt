package com.cailiangzhe.lexidue.di

import com.cailiangzhe.lexidue.data.remote.dictionaryResponseSizeLimitInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient
            .Builder()
            .callTimeout(DICTIONARY_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(dictionaryResponseSizeLimitInterceptor())
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        json: Json,
        okHttpClient: OkHttpClient,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(DICTIONARY_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE))
            .build()

    private const val DICTIONARY_API_BASE_URL = "https://api.dictionaryapi.dev/"
    private const val DICTIONARY_CALL_TIMEOUT_SECONDS = 15L
    private val JSON_MEDIA_TYPE = "application/json".toMediaType()
}
