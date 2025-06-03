package com.example.transcriptionapp.ui.screens

import android.app.Activity
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Addchart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.transcriptionapp.model.database.Transcription
import com.example.transcriptionapp.ui.components.ScrollableWithFixedPartsModalSheet
import com.example.transcriptionapp.ui.components.TranscriptionDetailDialog
import com.example.transcriptionapp.ui.components.TranscriptionListItem
import com.example.transcriptionapp.util.FileUtils.saveFileToCache
import com.example.transcriptionapp.viewmodel.BottomSheetViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscriptionScreen(onSettingsClick: () -> Unit, viewModel: BottomSheetViewModel) {

    val transcriptionListState by viewModel.transcriptionList.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle() // Assuming you have an isLoading state in your ViewModel

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val activity = LocalContext.current

    val selectedItems = remember { mutableStateListOf<Int>() }
    var isSelectionMode by remember { mutableStateOf(false) }
    var isSelectAll by remember { mutableStateOf(false) }

    var showDetailDialog by remember { mutableStateOf(false) }
    var selectedTranscriptionForDialog by remember { mutableStateOf<Transcription?>(null) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    // Reset selection mode when transcription list changes (e.g., items deleted)
    LaunchedEffect(transcriptionListState) {
        if (selectedItems.isNotEmpty() && !transcriptionListState.any { it.id in selectedItems }) {
            isSelectionMode = false
            selectedItems.clear()
            isSelectAll = false
        }
        // If selection mode is active but all selected items are removed from the list
        // (e.g., after deletion)
        if (isSelectionMode && selectedItems.isEmpty() && transcriptionListState.isNotEmpty()) {
            isSelectionMode = false
        }
    }


    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.let { data ->
                    val uris = mutableListOf<Uri>()
                    if (data.clipData != null) {
                        val itemCount = data.clipData!!.itemCount
                        for (i in 0 until itemCount) {
                            val uri = data.clipData!!.getItemAt(i).uri
                            Log.d("TranscriptionScreen", "Selected item Uri Path $i: ${uri.path}")
                            saveFileToCache(activity, uri)?.let { tempFile ->
                                uris.add(Uri.fromFile(tempFile))
                            }
                        }
                    } else {
                        data.data?.let { uri ->
                            Log.d("TranscriptionScreen", "Uri Path ${uri.path}")
                            saveFileToCache(activity, uri)?.let { tempFile ->
                                uris.add(Uri.fromFile(tempFile))
                            }
                        }
                    }
                    if (uris.isNotEmpty()) {
                        viewModel.onAudioSelected(uris)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Processing audio files...")
                        }
                        viewModel.transcribeAudios()
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("No audio files selected or supported.")
                        }
                    }
                }
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("File selection cancelled.")
                }
            }
        }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Crossfade(
                targetState = isSelectionMode,
                animationSpec = tween(durationMillis = 300),
                label = "topBarCrossfade"
            ) { inSelectionMode ->
                if (inSelectionMode) {
                    TopAppBar(
                        title = {
                            Text(
                                text = "${selectedItems.size} selected",
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    isSelectionMode = false
                                    selectedItems.clear()
                                    isSelectAll = false
                                }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Cancel Selection",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { showDeleteConfirmationDialog = true }) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteSweep,
                                    contentDescription = "Delete Selected",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        },
                        modifier = Modifier.shadow(4.dp)
                    )
                } else {
                    TopAppBar(
                        title = { Text(stringResource(R.string.app_name)) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.primary,
                        ),
                        actions = {
                            IconButton(onClick = { onSettingsClick() }) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                        modifier = Modifier.shadow(4.dp)
                    )
                }
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp) // Space between FABs
            ) {
                // Hide FABs when in selection mode for a cleaner look
                AnimatedVisibility(
                    visible = !isSelectionMode,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SmallFloatingActionButton(
                            onClick = { viewModel.onSampleClick() },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 4.dp, pressedElevation = 8.dp
                            )
                        ) {
                            Icon(imageVector = Icons.Filled.Addchart, contentDescription = "Add Sample Data")
                        }

                        ExtendedFloatingActionButton(
                            text = { Text("New Transcription") },
                            icon = {
                                Icon(
                                    Icons.AutoMirrored.Filled.NoteAdd,
                                    contentDescription = "New Transcription Icon"
                                )
                            },
                            onClick = { viewModel.buttonOnClick(launcher) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 6.dp, pressedElevation = 12.dp
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues -> // Scaffold provides content padding here
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply Scaffold's content padding
        ) {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.End)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilterChip(
                        onClick = {
                            isSelectAll = !isSelectAll
                            selectedItems.clear()
                            if (isSelectAll) {
                                selectedItems.addAll(transcriptionListState.map { it.id })
                            }
                        },
                        label = { Text("Select all") },
                        selected = isSelectAll,
                        leadingIcon =
                            if (isSelectAll) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Done,
                                        contentDescription = "Selected all",
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            } else null,
                        border =
                            FilterChipDefaults.filterChipBorder( // Add a border for better definition
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                                selectedBorderWidth = 1.dp,
                                enabled = true,
                                selected = isSelectAll
                            )
                    )
                }
            }


            if (transcriptionListState.isEmpty() && !isLoading && !isSelectionMode) {
                EmptyState(modifier = Modifier.weight(1f)) // Let EmptyState take remaining space
            } else if (isLoading && transcriptionListState.isEmpty()) {
                // Show a loading indicator if the initial list is empty and still loading
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f) // Fill remaining space
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 8.dp)
                ) {
                    items(transcriptionListState, key = { it.id }) { transcription ->
                        val isSelected = selectedItems.contains(transcription.id)
                        TranscriptionListItem(
                            transcription = transcription,
                            isSelected = isSelected,
                            isSelectionMode = isSelectionMode,
                            onItemClick = {
                                if (isSelectionMode) {
                                    if (isSelected) selectedItems.remove(transcription.id)
                                    else selectedItems.add(transcription.id)
                                    isSelectAll =
                                        selectedItems.size == transcriptionListState.size &&
                                                transcriptionListState.isNotEmpty()
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
                                isSelectAll =
                                    selectedItems.size == transcriptionListState.size &&
                                            transcriptionListState.isNotEmpty()
                            },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        if (transcriptionListState.lastOrNull() != transcription) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        } // End of main Column
    } // End of Scaffold

    if (showDetailDialog && selectedTranscriptionForDialog != null) {
        TranscriptionDetailDialog(
            transcription = selectedTranscriptionForDialog!!,
            onDismissRequest = {
                showDetailDialog = false
                selectedTranscriptionForDialog = null
            }
        )
    }

    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete ${selectedItems.size} selected transcription(s)? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onDeleteSelectedClick(selectedItems.toList())
                        selectedItems.clear()
                        isSelectionMode = false
                        isSelectAll = false
                        showDeleteConfirmationDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmationDialog = false }) {
                    Text("Cancel")
                }
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
        modifier = modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxHeight(0.6f)
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
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
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}