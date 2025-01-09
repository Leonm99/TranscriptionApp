package com.example.transcriptionapp.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.transcriptionapp.ui.components.BottomSheet
import com.example.transcriptionapp.viewmodel.TranscriptionViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptionScreen(onSettingsClick: () -> Unit, viewModel: TranscriptionViewModel) {

  val activity = LocalContext.current
  val launcher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
              result.data?.data?.let { uri -> viewModel.onAudioSelected(uri, activity) }
            }
          }

  TopAppBar(
      title = { Text("TranscriptionApp") },
      colors =
          TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              titleContentColor = MaterialTheme.colorScheme.primary,
          ),
      actions = {
        IconButton(
            onClick = {
                 onSettingsClick()
            }) {
              Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings")
            }
      })

  Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Button(onClick = { viewModel.buttonOnClick(launcher) }) { Text("Pick Audio") }

  }
    BottomSheet(viewModel)
}
