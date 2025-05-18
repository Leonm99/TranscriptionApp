package com.example.transcriptionapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.transcriptionapp.model.SettingsRepository
import com.example.transcriptionapp.ui.components.AudioPermissionTextProvider
import com.example.transcriptionapp.ui.components.PermissionDialog
import com.example.transcriptionapp.ui.screens.SettingsScreen
import com.example.transcriptionapp.ui.screens.TranscriptionScreen
import com.example.transcriptionapp.ui.theme.TranscriptionAppTheme
import com.example.transcriptionapp.viewmodel.BottomSheetViewModel
import com.example.transcriptionapp.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
  val bottomSheetViewModel: BottomSheetViewModel by viewModels<BottomSheetViewModel>()
  val settingsViewModel: SettingsViewModel by viewModels<SettingsViewModel>()
  @Inject lateinit var settingsRepository: SettingsRepository
  var dynamicColor: Boolean = true
  val context = this

  private val permissionsToRequest = arrayOf(Manifest.permission.READ_MEDIA_AUDIO)

  override fun onCreate(savedInstanceState: Bundle?) {

    super.onCreate(savedInstanceState)
    CoroutineScope(Dispatchers.IO).launch {
      settingsRepository.userPreferencesFlow.collect { userPreferences ->
        dynamicColor = userPreferences.dynamicColor

      }

    }
    enableEdgeToEdge()
    setContent {
      TranscriptionAppTheme(dynamicColor = dynamicColor) {
        val navController = rememberNavController()
        val dialogQueue = bottomSheetViewModel.visiblePermissionDialogQueue

        val multiplePermissionResultLauncher =
          rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
            onResult = { perms ->
              permissionsToRequest.forEach { permission ->
                bottomSheetViewModel.onPermissionResult(
                  permission = permission,
                  isGranted = perms[permission] == true,
                )
              }
            },
          )
        LaunchedEffect(key1 = Unit) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            multiplePermissionResultLauncher.launch(permissionsToRequest)
          }
        }

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

        dialogQueue.reversed().forEach { permission ->
          PermissionDialog(
            permissionTextProvider =
              when (permission) {
                Manifest.permission.READ_MEDIA_AUDIO -> {

                  AudioPermissionTextProvider()
                }
                else -> return@forEach
              },
            isPermanentlyDeclined = !shouldShowRequestPermissionRationale(permission),
            onDismiss = bottomSheetViewModel::dismissDialog,
            onOkClick = {
              bottomSheetViewModel.dismissDialog()
              multiplePermissionResultLauncher.launch(arrayOf(permission))
            },
            onGoToAppSettingsClick = ::openAppSettings,
          )
        }
      }
    }
  }
}

fun Activity.openAppSettings() {
  Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
    .also(::startActivity)
}

@Serializable object Screen1

@Serializable object SettingsRoute
