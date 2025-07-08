package com.example.transcriptionapp.model

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import javax.inject.Inject

class SettingsRepository @Inject constructor(private val dataStore: DataStore<UserPreferences>) {

  val userPreferencesFlow: Flow<UserPreferences> =
    dataStore.data.catch { exception ->
      if (exception is IOException) {
        emit(UserPreferences())
      } else {
        throw exception
      }
    }



  suspend fun setLanguage(language: String) {
    dataStore.updateData { currentSettings -> currentSettings.copy(selectedLanguage = language) }
  }

  suspend fun setTranscriptionProvider(provider: ProviderType) {
    dataStore.updateData { currentSettings -> currentSettings.copy(selectedTranscriptionProvider = provider) }
  }

  suspend fun setSummaryProvider(provider: ProviderType) {
    dataStore.updateData { currentSettings -> currentSettings.copy(selectedSummaryProvider = provider) }
  }

  suspend fun setAutoSave(autoSave: Boolean) {
    dataStore.updateData { currentSettings -> currentSettings.copy(autoSave = autoSave) }
  }

  suspend fun setMockApi(mockApi: Boolean) {
    dataStore.updateData { currentSettings -> currentSettings.copy(mockApi = mockApi) }
  }

  suspend fun setDynamicColor(dynamicColor: Boolean) {
    dataStore.updateData { currentSettings -> currentSettings.copy(dynamicColor = dynamicColor) }
  }

  suspend fun setSilenceTrimming(enableSilenceTrimming: Boolean) {
    dataStore.updateData { currentSettings -> currentSettings.copy(enableSilenceTrimming = enableSilenceTrimming) }
  }

  suspend fun setSilenceThreshold(silenceThresholdDb: Int) {
    dataStore.updateData { currentSettings -> currentSettings.copy(silenceThresholdDb = silenceThresholdDb) }
  }

  suspend fun setSilenceDuration(silenceDurationSeconds: Float) {
    dataStore.updateData { currentSettings -> currentSettings.copy(silenceDurationSeconds = silenceDurationSeconds) }
  }
}
