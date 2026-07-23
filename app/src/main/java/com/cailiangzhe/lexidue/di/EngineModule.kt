package com.cailiangzhe.lexidue.di

import com.cailiangzhe.lexidue.domain.usecase.IdProvider
import com.cailiangzhe.lexidue.domain.usecase.SystemTimeProvider
import com.cailiangzhe.lexidue.domain.usecase.TimeProvider
import com.cailiangzhe.lexidue.domain.usecase.UuidIdProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EngineModule {
    @Provides
    @Singleton
    fun provideTimeProvider(): TimeProvider = SystemTimeProvider

    @Provides
    @Singleton
    fun provideIdProvider(): IdProvider = UuidIdProvider
}
