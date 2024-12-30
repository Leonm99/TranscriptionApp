package com.example.transcriptionapp.ui.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringArrayResource
import com.example.transcriptionapp.R
import com.example.transcriptionapp.viewmodel.SettingsViewModel

@Composable
fun ApiKeyDialog(viewModel: SettingsViewModel) {
    val userInput = rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { viewModel.hideDialog() },
        title = { Text(text = "API Key") },
        text = {
            TextField(
                value = userInput.value,
                onValueChange = { userInput.value = it },
                label = { Text("Enter text") },
                maxLines = 5  // Allows up to 5 lines
            )
        },
        confirmButton = if (userInput.value.isEmpty()) {
            {
                TextButton(
                    onClick = { viewModel.hideDialog() },
                ) {
                    Text(text = "Cancel")
                }
            }
        } else {
            {
                TextButton(
                    onClick = {
                        viewModel.setUserApiKey(userInput.value)
                        viewModel.hideDialog()
                    },
                ) {
                    Text(text = "Select")
                }
            }
        },
        dismissButton =
            {
                TextButton(
                    onClick = { userInput.value = "" },
                ) {
                    Text(text = "Clear")
                }

            },
    )
    }


@Composable
 fun LanguageDialog(viewModel: SettingsViewModel, selectedLanguageKey: String?) {
   val userSelectedLanguage = rememberSaveable { mutableStateOf(selectedLanguageKey) }
    val items = stringArrayResource(id = R.array.string_array_languages)

    AlertDialog(
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

                            }
                        )
                    }
                }
        },
        confirmButton = {
                TextButton(
                    onClick = { viewModel.hideDialog() },
                ) {
                    Text(text = "Back")
                }
            },
        dismissButton = {

            },
    )
}

@Composable
fun ModelDialog(viewModel: SettingsViewModel, selectedModelKey: String) {
   val userSelectedModel = rememberSaveable { mutableStateOf(selectedModelKey) }
    val items = stringArrayResource(id = R.array.string_array_models)

    AlertDialog(
        onDismissRequest = { viewModel.hideDialog() },
        title = { Text(text = "Models") },
        text = {

            LazyColumn {
                items(items) { model ->
                    LabelRadioButton(
                        item = model,
                        isSelected = model == userSelectedModel.value,
                        onClick = {
                            userSelectedModel.value = model
                            viewModel.setSelectedModel(model)

                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { viewModel.hideDialog() },
            ) {
                Text(text = "Back")
            }
        },
        dismissButton = {

        },
    )
}

@Composable
fun DeleteDialog(viewModel: SettingsViewModel) {

    AlertDialog(
        onDismissRequest = { viewModel.hideDialog() },
        title = { Text(text = "Delete Transcriptions") },
        text = {
            Text(text = "TEST")
        },
        confirmButton =
            {
                TextButton(
                    onClick = { viewModel.hideDialog() },
                ) {
                    Text(text = "Cancel")
                }

        },
        dismissButton =
            {
                TextButton(
                    onClick = { viewModel.hideDialog()  },
                ) {
                    Text(text = "Clear")
                }

            },
    )
}

