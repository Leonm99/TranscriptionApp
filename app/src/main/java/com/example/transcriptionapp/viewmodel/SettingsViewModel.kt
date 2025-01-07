package com.example.transcriptionapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.transcriptionapp.util.DataStoreUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext

private const val TAG = "SettingsViewModel"

enum class DialogType { API, LANGUAGE, MODEL, DELETE }

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    val dataStoreUtil = DataStoreUtil(getApplication<Application>().applicationContext)

    private val _showDialog = MutableStateFlow(false)
    var showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    private val _dialogType = MutableStateFlow(DialogType.API)
    var dialogType: StateFlow<DialogType> = _dialogType.asStateFlow()


    private val _userApiKey = MutableStateFlow("")
    var userApiKey: StateFlow<String> = _userApiKey.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("English")
    var selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _selectedModel = MutableStateFlow("gpt-4o-mini")
    var selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _switchState = MutableStateFlow(false)
    var switchState: StateFlow<Boolean> = _switchState.asStateFlow()

    init {
        viewModelScope.launch{
            _userApiKey.value = dataStoreUtil.getString(stringPreferencesKey("userApiKey")).first() ?: ""
            _selectedLanguage.value = dataStoreUtil.getString(stringPreferencesKey("selectedLanguage")).first() ?: ""
            _selectedModel.value = dataStoreUtil.getString(stringPreferencesKey("selectedModel")).first() ?: ""
            _switchState.value = dataStoreUtil.getBoolean(booleanPreferencesKey("isFormattingEnabled")).first()

        }
    }

    fun showDialog(type: DialogType = DialogType.API) {
        _dialogType.value = type
        _showDialog.value = true
    }

        fun hideDialog() {
            _showDialog.value = false
        }

        fun setUserApiKey(key: String) {
            _userApiKey.value = key
            viewModelScope.launch(Dispatchers.IO) {

                    dataStoreUtil.putString(stringPreferencesKey("userApiKey"), key)


            }


        }

    fun setSelectedLanguage(key: String) {
        _selectedLanguage.value = key
        viewModelScope.launch(Dispatchers.IO) {
                dataStoreUtil.putString(stringPreferencesKey("selectedLanguage"), key)

        }
        Log.d(TAG, _selectedLanguage.value)
    }

    fun getSelectedLanguage(): String {
        viewModelScope.launch(Dispatchers.IO) {

            dataStoreUtil.getString(stringPreferencesKey("selectedLanguage")).collect { storedLanguage ->
                _selectedLanguage.value = storedLanguage ?: ""

            }

        }

        Log.d(TAG, _selectedLanguage.value)
        return _selectedLanguage.value
    }

    fun setSelectedModel(key: String) {

        viewModelScope.launch(Dispatchers.IO) {
                dataStoreUtil.putString(stringPreferencesKey("selectedModel"), key)
                _selectedModel.value = key

        }
    }

    fun getSelectedmodel(): String {
        viewModelScope.launch(Dispatchers.IO) {

                dataStoreUtil.getString(stringPreferencesKey("selectedModel"))
                    .collect { storedmodel ->
                        _selectedModel.value = storedmodel ?: ""

            }
        }
        return _selectedModel.value
    }

    fun updateSwitchState(newState: Boolean) {
        _switchState.value = newState
        viewModelScope.launch(Dispatchers.IO) {
                dataStoreUtil.putBoolean(booleanPreferencesKey("isFormattingEnabled"), newState)

        }

    }

}