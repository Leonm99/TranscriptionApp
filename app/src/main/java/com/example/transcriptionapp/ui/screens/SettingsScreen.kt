package com.example.transcriptionapp.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.GeneratingTokens
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alorma.compose.settings.ui.SettingsCheckbox // Assuming this is from a third-party library
import com.alorma.compose.settings.ui.SettingsMenuLink // Assuming this is from a third-party library
import com.alorma.compose.settings.ui.SettingsSwitch   // Assuming this is from a third-party library
import com.example.transcriptionapp.ui.components.DeleteDialog
import com.example.transcriptionapp.ui.components.LanguageDialog
import com.example.transcriptionapp.ui.components.SummaryDialog
import com.example.transcriptionapp.ui.components.TranscriptionDialog
import com.example.transcriptionapp.util.showToast
import com.example.transcriptionapp.viewmodel.DialogType
import com.example.transcriptionapp.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBackClick: () -> Unit, viewModel: SettingsViewModel) {
  val showDialog by viewModel.showDialog.collectAsStateWithLifecycle()
  val dialogType by viewModel.dialogType.collectAsStateWithLifecycle()
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val context = LocalContext.current

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Settings") },
        navigationIcon = {
          IconButton(onClick = onBackClick) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
      )
    },
    containerColor = MaterialTheme.colorScheme.background
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 8.dp), // Add some horizontal padding for the cards
      contentPadding = PaddingValues(vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp) // Space between cards
    ) {
      item {
        SettingsCardGroup(title = "Transcription Service") {
          SettingsMenuLink(
            title = { Text(text = "Transcription Provider") },
            subtitle = { Text(text = if (settings.selectedTranscriptionProvider.toString() == "OPEN_AI") "Open AI" else "Google Gemini") },
            onClick = { viewModel.showDialog(DialogType.TRANSCRIPTION_PROVIDER) },
            icon = {
              Icon(
                imageVector = Icons.Default.GeneratingTokens,
                contentDescription = "Transcription Provider",
                tint = MaterialTheme.colorScheme.primary
              )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
          )

          SettingsDivider()

            SettingsMenuLink(
              title = { Text(text = "Summarization & Translation Provider") },
              subtitle = { Text(text = if (settings.selectedSummaryProvider.toString() == "OPEN_AI") "Open AI" else "Google Gemini") },
              onClick = { viewModel.showDialog(DialogType.SUMMARIZATION_PROVIDER) },
              icon = {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.Message,
                  contentDescription = "Model",
                  tint = MaterialTheme.colorScheme.primary
                )
              },
              colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            SettingsDivider()

          SettingsSwitch(
            state = settings.mockApi,
            title = { Text(text = "Use Mock API") },
            subtitle = { Text(text = "For testing without real API calls") },
            icon = {
              Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = "Mock API",
                tint = MaterialTheme.colorScheme.primary
              )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            onCheckedChange = { viewModel.setMockApi(it) },
          )
        }
      }

      item {
        SettingsCardGroup(title = "General Preferences") {
          SettingsMenuLink(
            title = { Text(text = "Language") },
            subtitle = { Text(text = settings.selectedLanguage) },
            onClick = { viewModel.showDialog(DialogType.LANGUAGE) },
            icon = {
              Icon(
                imageVector = Icons.Default.Language,
                contentDescription = "Language",
                tint = MaterialTheme.colorScheme.primary
              )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
          )
          SettingsDivider() // Optional divider
          SettingsCheckbox(
            state = settings.autoSave,
            title = { Text(text = "Autosave Transcriptions") },
            subtitle = { Text(text = "Save transcriptions automatically after generation.") },
            onCheckedChange = { viewModel.setAutoSave(it) },
            icon = {
              Icon(
                imageVector = Icons.Default.AutoMode, // Consider Icons.Filled.Save
                contentDescription = "Autosave",
                tint = MaterialTheme.colorScheme.primary
              )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
          )
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SettingsDivider() // Optional divider
            SettingsSwitch(
              state = settings.dynamicColor,
              title = { Text(text = "Dynamic Color") },
              subtitle = { Text(text = "Use Material You theme colors (requires app restart)") },
              icon = {
                Icon(
                  imageVector = Icons.Default.ColorLens,
                  contentDescription = "Dynamic Color",
                  tint = MaterialTheme.colorScheme.primary
                )
              },
              colors = ListItemDefaults.colors(containerColor = Color.Transparent),
              onCheckedChange = {
                viewModel.setDynamicColor(it)
                showToast(context, "Restart app to apply changes.", true)
              },
            )
          }
        }
      }

      item {
        SettingsCardGroup(title = "Data Management") {
          SettingsMenuLink(
            title = { Text(text = "Delete All Transcriptions") },
            subtitle = { Text(text = "Permanently removes all saved transcriptions.") },
            onClick = { viewModel.showDialog(DialogType.DELETE) },
            icon = {
              Icon(
                imageVector = Icons.Default.DeleteSweep, // More indicative icon
                contentDescription = "Delete Transcriptions",
                tint = MaterialTheme.colorScheme.error // Use error color for destructive actions
              )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
          )
        }
      }
    }
  }

  if (showDialog) {
    when (dialogType) {
      DialogType.LANGUAGE -> LanguageDialog(viewModel, settings.selectedLanguage)
      DialogType.TRANSCRIPTION_PROVIDER -> TranscriptionDialog(viewModel, settings.selectedTranscriptionProvider)
      DialogType.SUMMARIZATION_PROVIDER -> SummaryDialog(viewModel, settings.selectedSummaryProvider)
      DialogType.DELETE -> DeleteDialog(viewModel)
    }
  }
}

@Composable
fun SettingsCardGroup(
  title: String,
  content: @Composable ColumnScope.() -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant) // Or surface
  ) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium, // Or titleSmall
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
      )
      // You can use a simple Column for the content if your third-party settings items
      // don't require a specific parent from that library.
      // If they do, you might need to pass the library's group Composable here.
      // For now, assuming they can be direct children of a Column:
      content()
    }
  }
}

// Optional: A styled divider for within cards if needed
@Composable
fun SettingsDivider() {
  HorizontalDivider(
    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    thickness = 0.5.dp,
    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
  )
}


// Modify your SettingsMenuLink/Checkbox/Switch if they allow color customization for title/icon
// For example, if your `com.alorma.compose.settings.ui.SettingsMenuLink` has color params:
/*
@Composable
fun SettingsMenuLink(
    // ... other params
    icon: (@Composable () -> Unit)? = null,
    title: @Composable () -> Unit,
    subtitle: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    titleColor: Color = LocalContentColor.current, // Add params like these if lib supports
    subtitleColor: Color = LocalContentColor.current.copy(alpha = 0.7f),
    iconTint: Color = LocalContentColor.current
) {
    // Original implementation, but pass the colors to relevant Text and Icon Composables
}
*/