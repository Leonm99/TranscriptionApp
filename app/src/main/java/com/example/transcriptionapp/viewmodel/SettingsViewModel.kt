package com.example.transcriptionapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transcriptionapp.model.SettingsRepository
import com.example.transcriptionapp.model.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "SettingsViewModel"

enum class DialogType { API, LANGUAGE, MODEL, DELETE }

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<UserPreferences> = settingsRepository.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())


    private val _showDialog = MutableStateFlow(false)
    var showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    private val _dialogType = MutableStateFlow(DialogType.API)
    var dialogType: StateFlow<DialogType> = _dialogType.asStateFlow()




    fun showDialog(type: DialogType = DialogType.API) {
        _dialogType.value = type
        _showDialog.value = true
    }

        fun hideDialog() {
            _showDialog.value = false
        }

        fun setUserApiKey(key: String) {

            viewModelScope.launch(Dispatchers.IO) {
                 settingsRepository.setUserApiKey(key)
            }


        }

    fun setSelectedLanguage(language: String) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.setLanguage(language)
        }
    }

    fun setSelectedModel(model: String) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsRepository.setModel(model)
        }
    }

    fun updateSwitchState(newState: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
              settingsRepository.setFormatSwitchState(newState)
        }
    }


}