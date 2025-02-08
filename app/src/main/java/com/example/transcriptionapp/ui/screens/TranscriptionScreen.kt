package com.example.transcriptionapp.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Addchart
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
  val selectedItems = remember { mutableStateListOf<Int>() }
  val isSelectionMode = remember { mutableStateOf<Boolean>(false) }
  val isSelectAll = remember { mutableStateOf<Boolean>(false) }

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
        if (isSelectionMode.value) {

          IconButton(
            onClick = {
              isSelectionMode.value = false
              selectedItems.clear()
            }
          ) {
            Icon(imageVector = Icons.Filled.Cancel, contentDescription = "Cancel")
          }
          IconButton(
            onClick = {
              viewModel.onDeleteSelectedClick(selectedItems)
              isSelectionMode.value = false
            }
          ) {
            Icon(imageVector = Icons.Filled.DeleteSweep, contentDescription = "Delete")
          }
        } else {
          IconButton(onClick = { onSettingsClick() }) {
            Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings")
          }
        }
      },
    )

    if (isSelectionMode.value) {
      Row(
        modifier = Modifier.padding(10.dp).align(Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (isSelectAll.value) {
          Text("Unselect All")
        } else {
          Text("Select All")
        }

        Checkbox(
          checked = isSelectAll.value,
          onCheckedChange = {
            isSelectAll.value = !isSelectAll.value

            if (isSelectAll.value) {
              for (transcription in transcriptionList) {
                selectedItems.add(transcription.id)
              }
            } else {
              selectedItems.clear()
            }
          },
        )
      }
    }
    LazyColumn(modifier = Modifier.padding(3.dp)) {
      items(transcriptionList) { transcription ->
        val isSelected = selectedItems.contains(transcription.id)
        TranscriptionCard(
          transcription = transcription,
          onCopyClicked = { viewModel.copyToClipboard(activity, it) },
          isSelected = isSelected,
          isSelectionMode = isSelectionMode.value,
          onSelected = {
            if (isSelected) {
              selectedItems.remove(transcription.id)
            } else {
              selectedItems.add(transcription.id)
              isSelectionMode.value = true
            }
          },
        )
      }
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    if (isLoadingState.value && !isBottomSheetVisibleState.value) {
      CircularProgressIndicator(modifier = Modifier.size(50.dp).align(Alignment.Center))
    }
    Row(Modifier.padding(40.dp).align(Alignment.BottomEnd)) {
      FloatingActionButton(
        elevation = FloatingActionButtonDefaults.elevation(),
        onClick = { viewModel.onSampleClick() },
        modifier = Modifier.padding(end = 10.dp),
      ) {
        Icon(imageVector = Icons.Filled.Addchart, contentDescription = "Add Sample Data")
      }
      FloatingActionButton(
        elevation = FloatingActionButtonDefaults.elevation(),
        onClick = { viewModel.buttonOnClick(launcher) },
      ) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = "Transcribe File")
      }
    }
  }

  BottomSheet(viewModel)
}
