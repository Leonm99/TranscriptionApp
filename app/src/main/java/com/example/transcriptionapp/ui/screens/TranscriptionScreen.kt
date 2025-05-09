package com.example.transcriptionapp.ui.screens

import android.app.Activity
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Addchart
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Transcribe
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.transcriptionapp.R
import com.example.transcriptionapp.ui.components.ScrollableWithFixedPartsModalSheet
import com.example.transcriptionapp.ui.components.TranscriptionCard
import com.example.transcriptionapp.util.FileUtils.saveFileToCache
import com.example.transcriptionapp.util.copyToClipboard
import com.example.transcriptionapp.viewmodel.BottomSheetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptionScreen(onSettingsClick: () -> Unit, viewModel: BottomSheetViewModel) {

  val transcriptionListState = viewModel.transcriptionList.collectAsStateWithLifecycle()
  val transcriptionList = transcriptionListState.value
  viewModel.isBottomSheetVisible.collectAsStateWithLifecycle().value
  val selectedItems = remember { mutableStateListOf<Int>() }
  var isSelectionMode = remember { mutableStateOf<Boolean>(false) }
  var isSelectAll = remember { mutableStateOf<Boolean>(false) }
  val activity = LocalContext.current
  val launcher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        // Check if multiple items were selected
        result.data?.let { data ->
          if (data.clipData != null) {
            // Multiple items selected
            val itemCount = data.clipData!!.itemCount
            for (i in 0 until itemCount) {
              val uri = data.clipData!!.getItemAt(i).uri
              Log.d("TranscriptionScreen", "Selected item Uri Path $i: ${uri.path}")
              val tempfile = saveFileToCache(activity, uri)
              val tempuri = Uri.fromFile(tempfile)
              viewModel.onAudioSelected(tempuri, activity)
            }
          } else {
            // Single item selected
            data.data?.let { uri ->
              Log.d("TranscriptionScreen", "Uri Path ${uri.path}")
              val tempfile = saveFileToCache(activity, uri)
              val tempuri = Uri.fromFile(tempfile)
              viewModel.onAudioSelected(tempuri, activity)
            }
          }
        }
        viewModel.transcribeAudios()
      }
    }

  Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars)) {
    TopAppBar(
      title = { Text(stringResource(R.string.app_name)) },
      colors =
        TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          titleContentColor = MaterialTheme.colorScheme.primary,
        ),
      actions = {
        if (isSelectionMode.value) {

          IconButton(
            onClick = {
              isSelectionMode.value = false
              selectedItems.clear()
              isSelectAll.value = false
            }
          ) {
            Icon(
              imageVector = Icons.Filled.Cancel,
              contentDescription = "Cancel",
              tint = MaterialTheme.colorScheme.primary,
            )
          }
          IconButton(
            onClick = {
              viewModel.onDeleteSelectedClick(selectedItems.toList())
              selectedItems.clear()
              isSelectionMode.value = false
              isSelectAll.value = false
            }
          ) {
            Icon(
              imageVector = Icons.Filled.DeleteSweep,
              contentDescription = "Delete",
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        } else {
          IconButton(onClick = { onSettingsClick() }) {
            Icon(
              imageVector = Icons.Filled.Settings,
              contentDescription = "Settings",
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        }
      },
    )

    AnimatedVisibility(
      isSelectionMode.value,
      modifier = Modifier.align(Alignment.End).padding(end = 8.dp, top = 8.dp),
    ) {
      FilterChip(
        modifier = Modifier.animateEnterExit(),
        onClick = {
          isSelectAll.value = !isSelectAll.value
          selectedItems.clear()
          if (isSelectAll.value) {
            selectedItems.addAll(transcriptionList.map { it.id })
          }
        },
        label = { Text("Select all") },
        selected = isSelectAll.value,
        leadingIcon =
          if (isSelectAll.value) {
            {
              Icon(
                imageVector = Icons.Filled.Done,
                contentDescription = "Done icon",
                modifier = Modifier.size(FilterChipDefaults.IconSize),
              )
            }
          } else {
            null
          },
      )
    }

    if (transcriptionList.isEmpty()) {
      Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Text(
          modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(16.dp),
          style = MaterialTheme.typography.headlineLarge,
          color = MaterialTheme.colorScheme.outline,
          text = "No transcriptions found in the database. 😕",
          textAlign = TextAlign.Center,
        )
      }
    } else {
      LazyColumn(
        modifier =
          Modifier.background(MaterialTheme.colorScheme.background).padding(horizontal = 4.dp)
      ) {
        items(transcriptionList) { transcription ->
          val isSelected = selectedItems.contains(transcription.id)
          TranscriptionCard(
            transcription = transcription,
            onCopyClicked = { copyToClipboard(activity, it) },
            isSelected = isSelected,
            isSelectionMode = isSelectionMode.value,
            onSelected = {
              if (isSelected) {
                selectedItems.remove(transcription.id)
                if (selectedItems.isEmpty()) {
                  isSelectAll.value = false
                }
              } else {
                selectedItems.add(transcription.id)
                isSelectionMode.value = true
              }
            },
            modifier =
              Modifier.padding(start = 4.dp, end = 4.dp, top = 9.dp)
                .shadow(elevation = 5.dp, shape = RoundedCornerShape(12.dp)),
          )
        }
      }
    }
  }
  Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars)) {
    Column(Modifier.padding(vertical = 20.dp, horizontal = 30.dp).align(Alignment.BottomEnd)) {
      SmallFloatingActionButton(
        elevation = FloatingActionButtonDefaults.elevation(),
        onClick = { viewModel.onSampleClick() },
        modifier = Modifier.align(Alignment.End).padding(bottom = 4.dp),
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
      ) {
        Icon(imageVector = Icons.Filled.Addchart, contentDescription = "Add Sample Data")
      }
      ExtendedFloatingActionButton(
        elevation = FloatingActionButtonDefaults.elevation(),
        onClick = { viewModel.buttonOnClick(launcher) },
        modifier = Modifier.align(Alignment.End),
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        icon = {
          Icon(imageVector = Icons.Filled.Transcribe, contentDescription = "Transcribe File")
        },
        text = { Text(text = "Transcribe") },
      )
    }
  }

  //  AnimatedVisibility(isBottomSheetVisible, exit = fadeOut(), enter = fadeIn()) {
  //    Box(modifier =
  // Modifier.fillMaxSize().alpha(0.5f).animateEnterExit().background(Color.Black))
  //  }

  ScrollableWithFixedPartsModalSheet(viewModel)
}
