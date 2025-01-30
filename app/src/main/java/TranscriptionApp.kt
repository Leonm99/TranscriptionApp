package com.example.transcriptionapp

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.room.Room
import com.example.transcriptionapp.com.example.transcriptionapp.model.database.TranscriptionDatabase
import com.example.transcriptionapp.model.UserPreferences
import com.example.transcriptionapp.model.UserPreferencesSerializer

class TranscriptionApp : Application() {
  companion object {
    private const val USER_PREFERENCES_NAME = "user-preferences"
  }

  val dataStore: DataStore<UserPreferences> by
    dataStore(fileName = USER_PREFERENCES_NAME, serializer = UserPreferencesSerializer)

  object DatabaseProvider {
    private var database: TranscriptionDatabase? = null

    fun getDatabase(context: Context): TranscriptionDatabase {
      if (database == null) {
        synchronized(this) {
          if (database == null) {
            database =
              Room.databaseBuilder(
                  context.applicationContext,
                  TranscriptionDatabase::class.java,
                  "transcription_database",
                )
                .build()
          }
        }
      }
      return database!!
    }
  }
}
