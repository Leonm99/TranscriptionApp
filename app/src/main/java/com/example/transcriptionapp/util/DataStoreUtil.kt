package com.example.transcriptionapp.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreUtil(private val context: Context) {

    suspend fun putString(key: Preferences.Key<String>, value: String?) {
        context.dataStore.edit { preferences ->
            preferences[key] = value ?: ""
        }
    }

    fun getString(key: Preferences.Key<String>): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[key]
        }
    }

    suspend fun putInt(key: Preferences.Key<Int>, value: Int) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    fun getInt(key: Preferences.Key<Int>): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: 0
        }
    }

    suspend fun putBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    fun getBoolean(key: Preferences.Key<Boolean>): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[key] == true
        }
    }

    suspend fun putLong(key: Preferences.Key<Long>, value: Long) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    fun getLong(key: Preferences.Key<Long>): Flow<Long> {
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: 0L
        }
    }

    suspend fun putFloat(key: Preferences.Key<Float>, value: Float) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    fun getFloat(key: Preferences.Key<Float>): Flow<Float> {
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: 0f
        }
    }

    suspend fun putDouble(key: Preferences.Key<Double>, value: Double) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    fun getDouble(key: Preferences.Key<Double>): Flow<Double> {
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: 0.0
        }
    }

    suspend fun putStringSet(key: Preferences.Key<Set<String>>, value: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    fun getStringSet(key: Preferences.Key<Set<String>>): Flow<Set<String>> {
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: emptySet()
        }
    }
}