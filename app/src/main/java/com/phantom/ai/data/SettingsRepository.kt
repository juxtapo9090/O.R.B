package com.phantom.ai.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension val for DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "phantom_settings")

data class AppSettings(
    val streamerUrl: String = "http://10.0.0.1:8200",
    val activeBackend: String = "custom"
)

object SettingsKeys {
    val STREAMER_URL = stringPreferencesKey("streamer_url")
    val ACTIVE_BACKEND = stringPreferencesKey("active_backend")
}

class SettingsRepository(private val context: Context) {

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            streamerUrl = prefs[SettingsKeys.STREAMER_URL] ?: "http://10.0.0.1:8200",
            activeBackend = prefs[SettingsKeys.ACTIVE_BACKEND] ?: "custom"
        )
    }

    suspend fun saveStreamerUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.STREAMER_URL] = url
        }
    }

    suspend fun saveActiveBackend(backend: String) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.ACTIVE_BACKEND] = backend
        }
    }
}
