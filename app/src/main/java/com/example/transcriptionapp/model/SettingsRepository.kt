package com.example.transcriptionapp.model

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.io.IOException


class SettingsRepository(val dataStore: DataStore<UserPreferences>) {

    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(UserPreferences())
            } else {
                throw exception
            }
        }

    suspend fun setUserApiKey(apiKey: String) {
        dataStore.updateData { currentSettings ->
          currentSettings.copy(userApiKey = apiKey)
        }
    }

    suspend fun setLanguage(language: String) {
        dataStore.updateData { currentSettings ->
            currentSettings.copy(selectedLanguage = language)
        }
    }

    suspend fun setModel(model: String) {
        dataStore.updateData { currentSettings ->
            currentSettings.copy(selectedModel = model)
        }
    }

    suspend fun setFormatSwitchState(switchState: Boolean) {
        dataStore.updateData { currentSettings ->
            currentSettings.copy(formatSwitchState = switchState)
        }
    }

}


