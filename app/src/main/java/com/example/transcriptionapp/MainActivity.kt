package com.example.transcriptionapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.transcriptionapp.ui.screens.SettingsScreen
import com.example.transcriptionapp.ui.screens.TranscriptionScreen
import com.example.transcriptionapp.ui.theme.TranscriptionAppTheme
import com.example.transcriptionapp.viewmodel.BottomSheetViewModel
import com.example.transcriptionapp.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  val bottomSheetViewModel: BottomSheetViewModel by viewModels<BottomSheetViewModel>()
  val settingsViewModel: SettingsViewModel by viewModels<SettingsViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      TranscriptionAppTheme {
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = Screen1) {
          composable<Screen1> {
            TranscriptionScreen(
              onSettingsClick = { navController.navigate(SettingsRoute) },
              bottomSheetViewModel,
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
