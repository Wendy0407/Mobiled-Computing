package com.morningrefresh.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "morning_refresh_settings")

data class AppSettings(
    val snoozeMinutes: Int = 10,
    val onboardingComplete: Boolean = true,
)

class SettingsStore(private val context: Context) {
    private object Keys {
        val snoozeMinutes = intPreferencesKey("snooze_minutes")
        val onboardingComplete = booleanPreferencesKey("onboarding_complete")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            snoozeMinutes = prefs[Keys.snoozeMinutes] ?: 10,
            onboardingComplete = prefs[Keys.onboardingComplete] ?: true,
        )
    }

    suspend fun setSnoozeMinutes(value: Int) {
        context.settingsDataStore.edit { it[Keys.snoozeMinutes] = value.coerceIn(5, 30) }
    }
}
