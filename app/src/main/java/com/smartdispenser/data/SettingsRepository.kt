package com.smartdispenser.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.smartdispenser.model.DispenseLimits
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val ESP32_BASE_URL = stringPreferencesKey("esp32_base_url")
        private val SECONDS_FOR_250ML = intPreferencesKey("seconds_for_250ml")
        private const val DEFAULT_BASE_URL = "http://192.168.1.100"
    }

    val esp32BaseUrl: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[ESP32_BASE_URL] ?: DEFAULT_BASE_URL
        }

    val secondsFor250Ml: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[SECONDS_FOR_250ML] ?: DispenseLimits.DEFAULT_SECONDS_FOR_250ML
        }

    suspend fun setEsp32BaseUrl(url: String) {
        val normalizedUrl = url.trim().trimEnd('/')
        context.dataStore.edit { preferences ->
            preferences[ESP32_BASE_URL] = normalizedUrl
        }
    }

    suspend fun setSecondsFor250Ml(seconds: Int) {
        val clamped = seconds.coerceIn(DispenseLimits.MIN_SECONDS, DispenseLimits.MAX_SECONDS)
        context.dataStore.edit { preferences ->
            preferences[SECONDS_FOR_250ML] = clamped
        }
    }

    fun secondsForVolumeMl(secondsFor250Ml: Int, volumeMl: Int): Int {
        val raw = (secondsFor250Ml.toDouble() * volumeMl / 250.0)
        return raw.toInt().coerceIn(DispenseLimits.MIN_SECONDS, DispenseLimits.MAX_SECONDS)
    }
}
