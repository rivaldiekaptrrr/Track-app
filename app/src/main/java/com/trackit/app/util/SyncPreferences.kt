package com.trackit.app.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_preferences")

@Singleton
class SyncPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val KEY_ONLINE_MODE = booleanPreferencesKey("is_online_mode")
        val KEY_USER_ID = stringPreferencesKey("firebase_user_id")
    }

    val isOnlineMode: Flow<Boolean> = context.syncDataStore.data.map { prefs ->
        prefs[KEY_ONLINE_MODE] ?: false
    }

    val userId: Flow<String?> = context.syncDataStore.data.map { prefs ->
        prefs[KEY_USER_ID]
    }

    suspend fun setOnlineMode(enabled: Boolean) {
        context.syncDataStore.edit { prefs ->
            prefs[KEY_ONLINE_MODE] = enabled
        }
    }

    suspend fun setUserId(uid: String?) {
        context.syncDataStore.edit { prefs ->
            if (uid != null) prefs[KEY_USER_ID] = uid
            else prefs.remove(KEY_USER_ID)
        }
    }
}
