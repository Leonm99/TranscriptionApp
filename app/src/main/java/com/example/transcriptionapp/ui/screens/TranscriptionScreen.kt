package com.example.transcriptionapp.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.transcriptionapp.ui.components.BottomSheet
import com.example.transcriptionapp.ui.components.TranscriptionCard
import com.example.transcriptionapp.viewmodel.BottomSheetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptionScreen(onSettingsClick: () -> Unit, viewModel: BottomSheetViewModel) {

  val transcriptionListState = viewModel.transcriptionList.collectAsState()
  val transcriptionList = transcriptionListState.value
  val isLoadingState = viewModel.isLoading.collectAsState()
  val isBottomSheetVisibleState = viewModel.isBottomSheetVisible.collectAsState()

  val activity = LocalContext.current
  val launcher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        result.data?.data?.let { uri -> viewModel.onAudioSelected(uri, activity) }
      }
    }

  Column(modifier = Modifier.fillMaxSize()) {
    TopAppBar(
      title = { Text("TranscriptionApp") },
      colors =
        TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          titleContentColor = MaterialTheme.colorScheme.primary,
        ),
      actions = {
        IconButton(onClick = { onSettingsClick() }) {
          Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings")
        }
      },
    )

    LazyColumn(modifier = Modifier.padding(3.dp)) {
      items(transcriptionList) { transcription ->
        TranscriptionCard(
          transcription = transcription,
          onCopyClicked = { viewModel.copyToClipboard(activity, it) },
        )
      }
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    if (isLoadingState.value && !isBottomSheetVisibleState.value) {
      CircularProgressIndicator(modifier = Modifier.size(50.dp).align(Alignment.Center))
    }
    FloatingActionButton(
      elevation = FloatingActionButtonDefaults.elevation(),
      onClick = { viewModel.buttonOnClick(launcher) },
      modifier = Modifier.padding(40.dp).align(Alignment.BottomEnd),
    ) {
      Icon(imageVector = Icons.Filled.Add, contentDescription = "Transcribe File")
    }
  }

  BottomSheet(viewModel)
}
