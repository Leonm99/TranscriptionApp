package com.example.transcriptionapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.transcriptionapp.R
import com.example.transcriptionapp.model.ProviderType
import com.example.transcriptionapp.viewmodel.SettingsViewModel


@Composable
fun LanguageDialog(viewModel: SettingsViewModel, selectedLanguageKey: String?) {
  val userSelectedLanguage = rememberSaveable { mutableStateOf(selectedLanguageKey) }
  val items = stringArrayResource(id = R.array.string_array_languages)

  AlertDialog(
    modifier = Modifier.heightIn(max = 550.dp),
    onDismissRequest = { viewModel.hideDialog() },
    title = { Text(text = "Language") },
    text = {
      LazyColumn {
        items(items) { language ->
          LabelRadioButton(
            item = language,
            isSelected = language == userSelectedLanguage.value,
            onClick = {
              userSelectedLanguage.value = language
              viewModel.setSelectedLanguage(language)
            },
          )
        }
      }
    },
    confirmButton = { TextButton(onClick = { viewModel.hideDialog() }) { Text(text = "Back") } },
    dismissButton = {},
  )
}

@Composable
fun TranscriptionDialog(viewModel: SettingsViewModel, selectedProvider: ProviderType) {
  val userSelectedProvider = rememberSaveable { mutableStateOf(selectedProvider) }
  val items = ProviderType.entries.map { it }

  AlertDialog(
    onDismissRequest = { viewModel.hideDialog() },
    title = { Text(text = "Providers") },
    text = {
      LazyColumn {
        items(items) { provider ->
          LabelRadioButton(
            item = if (provider.toString() == "OPEN_AI") {
              "Open AI"
            } else {
              "Google Gemini"
            },
            isSelected = provider == userSelectedProvider.value,
            onClick = {
              userSelectedProvider.value = if (provider.toString() == "OPEN_AI") {
                ProviderType.OPEN_AI
              } else {
                ProviderType.GEMINI
              }
              viewModel.setSelectedTranscriptionProvider(provider)
              viewModel.hideDialog()
            },
          )
        }
      }
    },
    confirmButton = { TextButton(onClick = { viewModel.hideDialog() }) { Text(text = "Back") } },
    dismissButton = {},
  )
}

@Composable
fun SummaryDialog(viewModel: SettingsViewModel, selectedProvider: ProviderType) {
  val userSelectedProvider = rememberSaveable { mutableStateOf(selectedProvider) }
  val items = ProviderType.entries.map { it }

  AlertDialog(
    onDismissRequest = { viewModel.hideDialog() },
    title = { Text(text = "Providers") },
    text = {
      LazyColumn {
        items(items) { provider ->
          LabelRadioButton(
            item = if (provider.toString() == "OPEN_AI") {
              "Open AI"
            } else {
              "Google Gemini"
            },
            isSelected = provider == userSelectedProvider.value,
            onClick = {
              userSelectedProvider.value = if (provider.toString() == "OPEN_AI") {
                ProviderType.OPEN_AI
              } else {
                ProviderType.GEMINI
              }
              viewModel.setSelectedSummaryProvider(provider)
              viewModel.hideDialog()
            },
          )
        }
      }
    },
    confirmButton = { TextButton(onClick = { viewModel.hideDialog() }) { Text(text = "Back") } },
    dismissButton = {},
  )
}

@Composable
fun DeleteDialog(viewModel: SettingsViewModel) {

  AlertDialog(
    onDismissRequest = { viewModel.hideDialog() },
    title = { Text(text = "Delete Transcription Database") },
    text = { Text(text = "This will delete ALL transcriptions from the database.") },
    confirmButton = {
      TextButton(
        onClick = {
          viewModel.deleteDatabase()
          viewModel.hideDialog()
        }
      ) {
        Text(text = "DELETE")
      }
    },
    dismissButton = { TextButton(onClick = { viewModel.hideDialog() }) { Text(text = "Cancel") } },
  )
}

@Composable
fun PermissionDialog(
  permissionTextProvider: PermissionTextProvider,
  isPermanentlyDeclined: Boolean,
  onDismiss: () -> Unit,
  onOkClick: () -> Unit,
  onGoToAppSettingsClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider()
        Text(
          text =
            if (isPermanentlyDeclined) {
              "Grant permission"
            } else {
              "OK"
            },
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
          modifier =
            Modifier
              .fillMaxWidth()
              .clickable {
                if (isPermanentlyDeclined) {
                  onGoToAppSettingsClick()
                } else {
                  onOkClick()
                }
              }
              .padding(16.dp),
        )
      }
    },
    title = { Text(text = "Permission required") },
    text = {
      Text(
        text = permissionTextProvider.getDescription(isPermanentlyDeclined = isPermanentlyDeclined)
      )
    },
    modifier = modifier,
  )
}

interface PermissionTextProvider {
  fun getDescription(isPermanentlyDeclined: Boolean): String
}

class AudioPermissionTextProvider : PermissionTextProvider {
  override fun getDescription(isPermanentlyDeclined: Boolean): String {
    return if (isPermanentlyDeclined) {
      "It seems you permanently declined Audio Media permission. " +
        "You can go to the app settings to grant it."
    } else {
      "This app needs access to your Audio Media so it can transcribe."
    }
  }
}
