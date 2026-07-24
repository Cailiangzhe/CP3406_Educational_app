package com.cailiangzhe.lexidue.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.cailiangzhe.lexidue.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

private const val SETTINGS_DATA_STORE_NAME = "lexidue_settings"

internal val SETTINGS_CORRUPTION_HANDLER =
    ReplaceFileCorruptionHandler<Preferences> { emptyPreferences() }

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SETTINGS_DATA_STORE_NAME,
    corruptionHandler = SETTINGS_CORRUPTION_HANDLER,
)

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SettingsPreferencesDataStore

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsPreferencesModule {
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(repository: PreferencesSettingsRepository): SettingsRepository

    companion object {
        @Provides
        @Singleton
        @SettingsPreferencesDataStore
        fun provideSettingsDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = context.settingsDataStore
    }
}
