package com.example.transcriptionapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.transcriptionapp.TranscriptionApp.DatabaseProvider
import com.example.transcriptionapp.com.example.transcriptionapp.model.database.TranscriptionDao
import com.example.transcriptionapp.model.SettingsRepository
import com.example.transcriptionapp.ui.screens.SettingsScreen
import com.example.transcriptionapp.ui.screens.TranscriptionScreen
import com.example.transcriptionapp.ui.theme.TranscriptionAppTheme
import com.example.transcriptionapp.viewmodel.SettingsViewModel
import com.example.transcriptionapp.viewmodel.TranscriptionViewModel
import kotlinx.serialization.Serializable

class MainActivity : ComponentActivity() {
  private lateinit var dao: TranscriptionDao
  private lateinit var settingsRepository: SettingsRepository
  private val settingsViewModel: SettingsViewModel by viewModels {
    object : ViewModelProvider.Factory {
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SettingsViewModel(settingsRepository, dao) as T
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    dao = DatabaseProvider.getDatabase(application).transcriptionDao()
    settingsRepository = SettingsRepository((application as TranscriptionApp).dataStore)

    val transcriptionViewModel = TranscriptionViewModel(settingsRepository, dao)
    enableEdgeToEdge()
    setContent {
      TranscriptionAppTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = Screen1) {
          composable<Screen1> {
            TranscriptionScreen(
              onSettingsClick = { navController.navigate(SettingsRoute) },
              transcriptionViewModel,
            )
          }

          composable<SettingsRoute> {
            SettingsScreen(onBackClick = { navController.popBackStack() }, settingsViewModel)
          }
        }
      }
    }
  }
}

@Serializable object Screen1

@Serializable object SettingsRoute
