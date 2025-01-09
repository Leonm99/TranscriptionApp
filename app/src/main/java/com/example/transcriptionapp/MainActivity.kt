package com.example.transcriptionapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.transcriptionapp.model.SettingsRepository
import com.example.transcriptionapp.model.UserPreferences
import com.example.transcriptionapp.model.UserPreferencesSerializer
import com.example.transcriptionapp.ui.screens.SettingsScreen
import com.example.transcriptionapp.ui.screens.TranscriptionScreen
import com.example.transcriptionapp.ui.theme.TranscriptionAppTheme
import com.example.transcriptionapp.viewmodel.SettingsViewModel
import com.example.transcriptionapp.viewmodel.TranscriptionViewModel
import kotlinx.serialization.Serializable

val Context.dataStore: DataStore<UserPreferences> by dataStore(
  fileName = "user-preferences",
  serializer = UserPreferencesSerializer
)

class MainActivity : ComponentActivity() {


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)


    val settingsRepository = SettingsRepository(dataStore)
    val settingsViewModel = SettingsViewModel(settingsRepository)
    val transcriptionViewModel = TranscriptionViewModel(settingsRepository)
    enableEdgeToEdge()
    setContent {
      TranscriptionAppTheme {

        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = Screen1) {

          composable<Screen1> {
            TranscriptionScreen(onSettingsClick = { navController.navigate(SettingsRoute) },transcriptionViewModel)
          }

          composable<SettingsRoute> {
            SettingsScreen(onBackClick = { navController.popBackStack() }, settingsViewModel)
          }

        }
      }


    }
  }
}

@Serializable
object Screen1

@Serializable
object SettingsRoute