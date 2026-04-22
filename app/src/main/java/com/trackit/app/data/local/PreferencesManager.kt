package com.trackit.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

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
}
