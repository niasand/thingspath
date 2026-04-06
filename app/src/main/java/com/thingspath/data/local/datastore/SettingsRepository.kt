package com.thingspath.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val API_KEY = stringPreferencesKey("silicon_flow_api_key")
    private val PAGE_SIZE = intPreferencesKey("page_size")
    private val INFINITE_SCROLL = booleanPreferencesKey("infinite_scroll")
    private val LAST_PULL_UPDATED_AT = longPreferencesKey("last_pull_updated_at")
    private val LAST_PUSH_UPDATED_AT = longPreferencesKey("last_push_updated_at")

    val apiKey: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[API_KEY]
        }

    val pageSize: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PAGE_SIZE] ?: 10
        }

    val infiniteScroll: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[INFINITE_SCROLL] ?: true
        }

    suspend fun saveApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[API_KEY] = key
        }
    }

    suspend fun savePageSize(size: Int) {
        context.dataStore.edit { preferences ->
            preferences[PAGE_SIZE] = size
        }
    }

    suspend fun saveInfiniteScroll(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[INFINITE_SCROLL] = enabled
        }
    }

    // ========== Sync watermarks ==========

    val lastPullUpdatedAt: Flow<Long> = context.dataStore.data
        .map { preferences -> preferences[LAST_PULL_UPDATED_AT] ?: 0L }

    val lastPushUpdatedAt: Flow<Long> = context.dataStore.data
        .map { preferences -> preferences[LAST_PUSH_UPDATED_AT] ?: 0L }

    suspend fun setLastPullUpdatedAt(timestamp: Long) {
        context.dataStore.edit { it[LAST_PULL_UPDATED_AT] = timestamp }
    }

    suspend fun setLastPushUpdatedAt(timestamp: Long) {
        context.dataStore.edit { it[LAST_PUSH_UPDATED_AT] = timestamp }
    }
}
