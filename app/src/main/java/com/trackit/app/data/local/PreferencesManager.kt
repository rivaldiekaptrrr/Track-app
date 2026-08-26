package com.trackit.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        val ACTIVE_PROFILE_ID = androidx.datastore.preferences.core.longPreferencesKey("active_profile_id")
        val PENDING_RESTORE = booleanPreferencesKey("pending_restore")
        val BYPASS_BIOMETRIC_ONCE = booleanPreferencesKey("bypass_biometric_once")
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val DAILY_REMINDER_TIME = androidx.datastore.preferences.core.stringPreferencesKey("daily_reminder_time")
        val EXPENSE_ONLY_MODE = booleanPreferencesKey("expense_only_mode")
        val THEME_MODE = intPreferencesKey("theme_mode")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val HAS_SKIPPED_LOGIN = booleanPreferencesKey("has_skipped_login")
        val HAS_SEEN_WELCOME = booleanPreferencesKey("has_seen_welcome")
    }

    val isTtsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[TTS_ENABLED] ?: true // Default is true
        }
        
    val activeProfileId: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[ACTIVE_PROFILE_ID] ?: 1L // Default to profile ID 1
        }
        
    val pendingRestore: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PENDING_RESTORE] ?: false
        }
        
    val bypassBiometricOnce: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[BYPASS_BIOMETRIC_ONCE] ?: false
        }
        
    val isBiometricEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[BIOMETRIC_ENABLED] ?: true // Default to true
        }

    val hasSkippedLogin: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[HAS_SKIPPED_LOGIN] ?: false
        }

    val hasSeenWelcome: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[HAS_SEEN_WELCOME] ?: false
        }

    val isDailyReminderEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DAILY_REMINDER_ENABLED] ?: false
        }

    val dailyReminderTime: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[DAILY_REMINDER_TIME] ?: "20:00"
        }

    val isExpenseOnlyMode: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[EXPENSE_ONLY_MODE] ?: false
        }

    val themeMode: Flow<ThemeMode> = context.dataStore.data
        .map { preferences ->
            val ordinal = preferences[THEME_MODE] ?: ThemeMode.SYSTEM.ordinal
            ThemeMode.values().getOrElse(ordinal) { ThemeMode.SYSTEM }
        }

    suspend fun setTtsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[TTS_ENABLED] = enabled
        }
    }
    
    suspend fun setActiveProfileId(profileId: Long) {
        context.dataStore.edit { preferences ->
            preferences[ACTIVE_PROFILE_ID] = profileId
        }
    }
    
    suspend fun setPendingRestore(pending: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PENDING_RESTORE] = pending
        }
    }
    
    suspend fun setBypassBiometricOnce(bypass: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BYPASS_BIOMETRIC_ONCE] = bypass
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setHasSkippedLogin(skipped: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAS_SKIPPED_LOGIN] = skipped
        }
    }

    suspend fun setHasSeenWelcome(seen: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAS_SEEN_WELCOME] = seen
        }
    }

    suspend fun setDailyReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DAILY_REMINDER_ENABLED] = enabled
        }
    }

    suspend fun setDailyReminderTime(time: String) {
        context.dataStore.edit { preferences ->
            preferences[DAILY_REMINDER_TIME] = time
        }
    }

    suspend fun setExpenseOnlyMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[EXPENSE_ONLY_MODE] = enabled
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode.ordinal
        }
    }
}
