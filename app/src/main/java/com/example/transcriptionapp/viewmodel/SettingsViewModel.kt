package com.example.transcriptionapp.viewmodel

import android.app.Application
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.transcriptionapp.util.DataStoreUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    val dataStoreUtil = DataStoreUtil(getApplication<Application>().applicationContext)

    private val _showDialog = MutableStateFlow(false)
    var showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    private val _dialogType = MutableStateFlow(0)
    var dialogType = _dialogType.asStateFlow()

    private val _userApiKey = MutableStateFlow("")
    var userApiKey: StateFlow<String> = _userApiKey.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("")
    var selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    init {
        viewModelScope.launch {
            dataStoreUtil.getString(stringPreferencesKey("userApiKey")).collect { storedApiKey ->
                _userApiKey.value = storedApiKey ?: ""
            }
        }
    }

        fun showDialog(type: String = "") {
            when (type) {
                "API" -> _dialogType.value = 0
                "LANGUAGE" -> _dialogType.value = 1
                "MODEL" -> _dialogType.value = 2
                "DELETE" -> _dialogType.value = 3
                else -> true
            }
            _showDialog.value = true
        }

        fun hideDialog() {
            _showDialog.value = false
        }

        fun setUserApiKey(key: String) {
            _userApiKey.value = key
            viewModelScope.launch {
                dataStoreUtil.putString(stringPreferencesKey("userApiKey"), key)

            }


        }

    fun setSelectedLanguage(key: String) {
        _selectedLanguage.value = key
        viewModelScope.launch {
            dataStoreUtil.putString(stringPreferencesKey("selectedLanguage"), key)

        }


    }




}