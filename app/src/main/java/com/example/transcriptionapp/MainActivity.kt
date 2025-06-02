package com.example.transcriptionapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.transcriptionapp.model.GoogleAuthClient
import com.example.transcriptionapp.model.SettingsRepository
import com.example.transcriptionapp.ui.components.AudioPermissionTextProvider
import com.example.transcriptionapp.ui.components.PermissionDialog
import com.example.transcriptionapp.ui.screens.SettingsScreen
import com.example.transcriptionapp.ui.screens.TranscriptionScreen
import com.example.transcriptionapp.ui.theme.TranscriptionAppTheme
import com.example.transcriptionapp.viewmodel.BottomSheetViewModel
import com.example.transcriptionapp.viewmodel.SettingsViewModel
import com.google.firebase.auth.FirebaseAuth
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
  @Inject lateinit var googleAuthClient: GoogleAuthClient

  private val permissionsToRequest =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
      } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
      }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    CoroutineScope(Dispatchers.IO).launch {
      settingsRepository.userPreferencesFlow.collect { userPreferences ->
        dynamicColor = userPreferences.dynamicColor
      }
    }
    enableEdgeToEdge()
    setContent {
      // Get Firebase Auth instance
      val auth = remember { FirebaseAuth.getInstance() }

      // State to hold the current signed-in status
      // Use rememberSaveable if you want to preserve this across configuration changes,
      // though auth state is usually fetched fresh anyway.
      var isSignedIn by remember { mutableStateOf(auth.currentUser != null) }

      // Effect to listen to auth state changes
      DisposableEffect(auth) { // Keyed on 'auth' instance
        val authStateListener =
            FirebaseAuth.AuthStateListener { firebaseAuth ->
              val user = firebaseAuth.currentUser
              isSignedIn = user != null
              Log.d(
                  "MainActivityAuth",
                  "Auth State Changed. User: ${user?.uid}, IsSignedIn: $isSignedIn")
            }
        auth.addAuthStateListener(authStateListener)

        // Don't forget to remove the listener when the composable leaves the screen
        onDispose { auth.removeAuthStateListener(authStateListener) }
      }

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
            SettingsScreen(
                onBackClick = { navController.popBackStack() }, settingsViewModel, isSignedIn)
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
