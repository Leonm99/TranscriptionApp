package com.example.transcriptionapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transcriptionapp.com.example.transcriptionapp.model.TranscriptionRepository
import com.example.transcriptionapp.model.ProviderType
import com.example.transcriptionapp.model.SettingsRepository
import com.example.transcriptionapp.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SettingsViewModel"

enum class DialogType {
  LANGUAGE,
  TRANSCRIPTION_PROVIDER,
  SUMMARIZATION_PROVIDER,
  DELETE,
}

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
  private val settingsRepository: SettingsRepository,
  private val transcriptionRepository: TranscriptionRepository,
) : ViewModel() {

  val settings: StateFlow<UserPreferences> =
    settingsRepository.userPreferencesFlow.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000),
      UserPreferences(),
    )

  private val _showDialog = MutableStateFlow(false)
  var showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

  private val _dialogType = MutableStateFlow(DialogType.LANGUAGE)
  var dialogType: StateFlow<DialogType> = _dialogType.asStateFlow()

  fun showDialog(type: DialogType = DialogType.LANGUAGE) {
    _dialogType.value = type
    _showDialog.value = true
  }

  fun hideDialog() {
    _showDialog.value = false
  }

  fun deleteDatabase() {
    viewModelScope.launch(Dispatchers.IO) { transcriptionRepository.deleteAllTranscriptions() }
  }

  fun setSelectedLanguage(language: String) {
    viewModelScope.launch(Dispatchers.IO) { settingsRepository.setLanguage(language) }
  }

  fun setSelectedTranscriptionProvider(provider: ProviderType) {
    viewModelScope.launch(Dispatchers.IO) { settingsRepository.setTranscriptionProvider(provider) }
  }

  fun setSelectedSummaryProvider(provider: ProviderType) {
    viewModelScope.launch(Dispatchers.IO) { settingsRepository.setSummaryProvider(provider) }
  }

  fun setAutoSave(autoSave: Boolean) {
    viewModelScope.launch(Dispatchers.IO) { settingsRepository.setAutoSave(autoSave) }
  }
  fun setMockApi(mockApi: Boolean) {
    viewModelScope.launch(Dispatchers.IO) { settingsRepository.setMockApi(mockApi) }
  }

  fun setDynamicColor(dynamicColor: Boolean) {
    viewModelScope.launch(Dispatchers.IO) { settingsRepository.setDynamicColor(dynamicColor) }
  }
}
