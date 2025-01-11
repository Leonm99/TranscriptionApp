package com.example.transcriptionapp

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.example.transcriptionapp.model.UserPreferences
import com.example.transcriptionapp.model.UserPreferencesSerializer

class TranscriptionApp : Application() {
    companion object {
        private const val USER_PREFERENCES_NAME = "user-preferences"
    }

    val dataStore: DataStore<UserPreferences> by dataStore(
        fileName = USER_PREFERENCES_NAME,
        serializer = UserPreferencesSerializer
    )
}