package com.example.transcriptionapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringArrayResource
import com.alorma.compose.settings.ui.SettingsRadioButton
import com.example.transcriptionapp.R
import com.example.transcriptionapp.viewmodel.SettingsViewModel

@Composable
fun ApiKeyDialog(viewModel: SettingsViewModel) {
    val userInput = remember { mutableStateOf("") }

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
fun LanguageDialog(viewModel: SettingsViewModel) {
    val userInput = remember { mutableStateOf("") }
    val items = stringArrayResource(id = R.array.string_array_languages)

    AlertDialog(
        onDismissRequest = { viewModel.hideDialog() },
        title = { Text(text = "API Key") },
        text = {

                LazyColumn {
                    items(items) { language ->
                        SettingsRadioButton(
                            state = false,
                            title = { Text(text = language) },
                            enabled = false ,
                            onClick = { viewModel.setSelectedLanguage(language) },
                        )
                    }
                }
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
fun ModelDialog(viewModel: SettingsViewModel) {
    val userInput = remember { mutableStateOf("") }

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
fun DeleteDialog(viewModel: SettingsViewModel) {
    val userInput = remember { mutableStateOf("") }

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

