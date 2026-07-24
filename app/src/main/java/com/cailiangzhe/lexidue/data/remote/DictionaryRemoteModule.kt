package com.cailiangzhe.lexidue.data.remote

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DictionaryRemoteModule {
    @Provides
    @Singleton
    fun provideDictionaryRemoteDataSource(retrofit: Retrofit): DictionaryRemoteDataSource =
        RetrofitDictionaryRemoteDataSource(retrofit.create(DictionaryApiService::class.java))
}
