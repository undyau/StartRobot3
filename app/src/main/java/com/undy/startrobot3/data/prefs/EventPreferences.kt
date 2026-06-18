package com.undy.startrobot3.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "event_prefs")

class EventPreferences(private val context: Context) {

    companion object {
        private val KEY_INTERVAL_SECONDS = intPreferencesKey("interval_seconds")
        private val KEY_DELAY_MINUTES = intPreferencesKey("delay_minutes")
        private val KEY_START_LIST_PATH = stringPreferencesKey("start_list_path")
        private val KEY_START_LIST_IS_URL = booleanPreferencesKey("start_list_is_url")
        private val KEY_USE_RECORDED_TIME_SOUNDS = booleanPreferencesKey("use_recorded_time_sounds")
    }

    val intervalSeconds: Flow<Int> = context.dataStore.data.map { it[KEY_INTERVAL_SECONDS] ?: 60 }
    val delayMinutes: Flow<Int> = context.dataStore.data.map { it[KEY_DELAY_MINUTES] ?: 0 }
    val startListPath: Flow<String> = context.dataStore.data.map { it[KEY_START_LIST_PATH] ?: "" }
    val startListIsUrl: Flow<Boolean> = context.dataStore.data.map { it[KEY_START_LIST_IS_URL] ?: false }
    val useRecordedTimeSounds: Flow<Boolean> =
        context.dataStore.data.map { it[KEY_USE_RECORDED_TIME_SOUNDS] ?: false }

    suspend fun setIntervalSeconds(seconds: Int) {
        context.dataStore.edit { it[KEY_INTERVAL_SECONDS] = seconds }
    }

    suspend fun setDelayMinutes(minutes: Int) {
        context.dataStore.edit { it[KEY_DELAY_MINUTES] = minutes }
    }

    suspend fun setStartList(path: String, isUrl: Boolean) {
        context.dataStore.edit {
            it[KEY_START_LIST_PATH] = path
            it[KEY_START_LIST_IS_URL] = isUrl
        }
    }

    suspend fun setUseRecordedTimeSounds(value: Boolean) {
        context.dataStore.edit { it[KEY_USE_RECORDED_TIME_SOUNDS] = value }
    }
}
