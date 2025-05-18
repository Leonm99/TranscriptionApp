package com.example.transcriptionapp.ui.screens

import android.app.Activity
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Addchart
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.transcriptionapp.R
import com.example.transcriptionapp.com.example.transcriptionapp.model.database.Transcription
import com.example.transcriptionapp.ui.components.ScrollableWithFixedPartsModalSheet
import com.example.transcriptionapp.ui.components.TranscriptionDetailDialog
import com.example.transcriptionapp.ui.components.TranscriptionListItem
import com.example.transcriptionapp.util.FileUtils.saveFileToCache
import com.example.transcriptionapp.viewmodel.BottomSheetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptionScreen(onSettingsClick: () -> Unit, viewModel: BottomSheetViewModel) {

  // val uiState by viewModel.uiState.collectAsStateWithLifecycle() // Example if using a UiState object
  // val transcriptionList = uiState.transcriptions
  // val isLoading = uiState.isLoading

  val transcriptionListState = viewModel.transcriptionList.collectAsStateWithLifecycle()
  val transcriptionList = transcriptionListState.value
  viewModel.isBottomSheetVisible.collectAsStateWithLifecycle().value // Ensure this state is used if needed

  val selectedItems = remember { mutableStateListOf<Int>() }
  var isSelectionMode by remember { mutableStateOf(false) }
  var isSelectAll by remember { mutableStateOf(false) }
  val activity = LocalContext.current

  var showDetailDialog by remember { mutableStateOf(false) }
  var selectedTranscriptionForDialog by remember { mutableStateOf<Transcription?>(null) }

  val launcher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        result.data?.let { data ->
          if (data.clipData != null) {
            val itemCount = data.clipData!!.itemCount
            for (i in 0 until itemCount) {
              val uri = data.clipData!!.getItemAt(i).uri
              Log.d("TranscriptionScreen", "Selected item Uri Path $i: ${uri.path}")
              saveFileToCache(activity, uri)?.let { tempFile ->
                val tempUri = Uri.fromFile(tempFile)
                viewModel.onAudioSelected(tempUri)
              }
            }
          } else {
            data.data?.let { uri ->
              Log.d("TranscriptionScreen", "Uri Path ${uri.path}")
              saveFileToCache(activity, uri)?.let { tempFile ->
                val tempUri = Uri.fromFile(tempFile)
                viewModel.onAudioSelected(tempUri)
              }
            }
          }
        }
        // Consider showing a loading indicator before this call or manage within ViewModel
        viewModel.transcribeAudios()
      }
    }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.navigationBars)
  ) {
    TopAppBar(
      title = { Text(stringResource(R.string.app_name)) },
      colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.primary,
      ),
      actions = {
        Crossfade(targetState = isSelectionMode, label = "topBarActions") { inSelectionMode ->
          if (inSelectionMode) {
            Row {
              IconButton(
                onClick = {
                  isSelectionMode = false
                  selectedItems.clear()
                  isSelectAll = false
                }
              ) {
                Icon(
                  imageVector = Icons.Filled.Cancel,
                  contentDescription = "Cancel Selection",
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
              IconButton(
                onClick = {
                  viewModel.onDeleteSelectedClick(selectedItems.toList())
                  selectedItems.clear()
                  isSelectionMode = false
                  isSelectAll = false
                }
              ) {
                Icon(
                  imageVector = Icons.Filled.DeleteSweep,
                  contentDescription = "Delete Selected",
                  tint = MaterialTheme.colorScheme.error, // Use error color
                )
              }
            }
          } else {
            // IconButton(onClick = { viewModel.onSampleClick() }) { // Example: Move "Add Sample" here
            // Icon(Icons.Filled.Addchart, "Add Sample Data", tint = MaterialTheme.colorScheme.primary)
            // }
            IconButton(onClick = { onSettingsClick() }) {
              Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      },
      modifier = Modifier.shadow(4.dp) // Add subtle shadow for elevation
    )

    AnimatedVisibility(
      visible = isSelectionMode,
      modifier = Modifier
        .align(Alignment.End)
        .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
      FilterChip(
        onClick = {
          isSelectAll = !isSelectAll
          selectedItems.clear()
          if (isSelectAll) {
            selectedItems.addAll(transcriptionList.map { it.id })
          }
        },
        label = { Text("Select all") },
        selected = isSelectAll,
        leadingIcon = if (isSelectAll) { {
          Icon(
            imageVector = Icons.Filled.Done,
            contentDescription = "Selected all",
            modifier = Modifier.size(FilterChipDefaults.IconSize)
          )
        } } else null,
        border = FilterChipDefaults.filterChipBorder( // Add a border for better definition
          borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
          selectedBorderColor = MaterialTheme.colorScheme.primary,
          selectedBorderWidth = 1.dp,
          enabled = true,
          selected = isSelectAll

        )
      )
    }

    // // Example: Handling a general loading state
    // if (isLoading) {
    // Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    // CircularProgressIndicator()
    // }
    // } else
    if (transcriptionList.isEmpty() && !isSelectionMode) { // Show empty state only if not in selection mode
      EmptyState()
    } else {
      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .background(MaterialTheme.colorScheme.background)
          .padding(horizontal = 8.dp) // Consistent horizontal padding for the list
      ) {
        items(transcriptionList, key = { it.id }) { transcription ->
          val isSelected = selectedItems.contains(transcription.id)
          TranscriptionListItem(
            transcription = transcription,
            isSelected = isSelected,
            isSelectionMode = isSelectionMode,
            onItemClick = {
              if (isSelectionMode) {
                if (isSelected) selectedItems.remove(transcription.id)
                else selectedItems.add(transcription.id)
                isSelectAll = selectedItems.size == transcriptionList.size && transcriptionList.isNotEmpty()
              } else {
                selectedTranscriptionForDialog = transcription
                showDetailDialog = true
              }
            },
            onItemLongClick = {
              if (!isSelectionMode) {
                isSelectionMode = true
              }
              if (isSelected) selectedItems.remove(transcription.id)
              else selectedItems.add(transcription.id)
              isSelectAll = selectedItems.size == transcriptionList.size && transcriptionList.isNotEmpty()
            },
            modifier = Modifier
              .padding(vertical = 4.dp) // Spacing between items

          )
          if (transcriptionList.lastOrNull() != transcription) {
            HorizontalDivider(
              thickness = 0.5.dp,
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
              modifier = Modifier.padding(horizontal = 8.dp) // Indent divider slightly
            )
          }
        }
      }
    }
  } // End of main Column

  Box(
    modifier = Modifier
      .fillMaxSize()
      .windowInsetsPadding(WindowInsets.navigationBars)
      .padding(16.dp), // General padding for FAB alignment from screen edges
    contentAlignment = Alignment.BottomEnd
  ) {
    Column(
      horizontalAlignment = Alignment.End,
      verticalArrangement = Arrangement.spacedBy(8.dp) // Space between FABs
    ) {
      // Consider if "Add Sample" is essential as a FAB.
      // If it's a developer tool or rarely used, it could be in a settings menu or debug build.
      if (!isSelectionMode) { // Hide FABs when in selection mode for a cleaner look
        SmallFloatingActionButton(
          onClick = { viewModel.onSampleClick() },
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
          elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp, pressedElevation = 8.dp)
        ) {
          Icon(imageVector = Icons.Filled.Addchart, contentDescription = "Add Sample Data")
        }

        ExtendedFloatingActionButton(
          text = { Text("New Transcription") },
          icon = { Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = "New Transcription Icon") },
          onClick = { viewModel.buttonOnClick(launcher) },
          containerColor = MaterialTheme.colorScheme.primary, // Prominent color for primary action
          contentColor = MaterialTheme.colorScheme.onPrimary,
          elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp, pressedElevation = 12.dp)
        )
      }
    }
  }

  if (showDetailDialog && selectedTranscriptionForDialog != null) {
    TranscriptionDetailDialog(
      transcription = selectedTranscriptionForDialog!!,
      onDismissRequest = {
        showDetailDialog = false
        selectedTranscriptionForDialog = null
      }
    )
  }

  if (viewModel.isBottomSheetVisible.collectAsStateWithLifecycle().value) {
    ScrollableWithFixedPartsModalSheet(viewModel)
  }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .padding(16.dp), // Padding around the content of the empty state
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.fillMaxHeight(0.6f) // Adjust how much vertical space it occupies
    ) {
      Icon(
        imageVector = Icons.Outlined.Info,
        contentDescription = null, // Decorative icon
        modifier = Modifier.size(64.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
      )
      Spacer(modifier = Modifier.height(20.dp))
      Text(
        text = "No Transcriptions Yet",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = "Tap the 'New Transcription' button below to create your first one.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp) // Prevent text from hitting edges
      )
    }
  }
}