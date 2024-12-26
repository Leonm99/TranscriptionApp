package com.example.transcriptionapp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.alorma.compose.settings.ui.SettingsSwitch
import com.example.transcriptionapp.ui.components.ApiKeyDialog
import com.example.transcriptionapp.ui.components.DeleteDialog
import com.example.transcriptionapp.ui.components.LanguageDialog
import com.example.transcriptionapp.ui.components.ModelDialog
import com.example.transcriptionapp.viewmodel.SettingsViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.TranscriptionScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator


@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SettingsScreen(navigator: DestinationsNavigator) {

    val viewModel: SettingsViewModel = viewModel()
    val showDialog by viewModel.showDialog.collectAsState()
    val dialogType by viewModel.dialogType.collectAsState()
    val userApiKey by viewModel.userApiKey.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()

    Column(modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally) {


        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(
                    onClick = {
                        navigator.navigate(TranscriptionScreenDestination())
                    }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
        )

        SettingsMenuLink(
            title = { Text(text = "OpenAI API Key") },
            subtitle = { Text(text = userApiKey) },
            onClick = {
                viewModel.showDialog("API")
            },
            icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Back") },
        )

        SettingsMenuLink(
            title = { Text(text = "Language") },
            subtitle = { Text(text = selectedLanguage) },
            onClick = {
                viewModel.showDialog("LANGUAGE")
            },
            icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Back") },
        )

        SettingsMenuLink(
            title = { Text(text = "Model") },
            subtitle = { Text(text = "Model for Summarization.") },
            onClick = {
                viewModel.showDialog("MODEL")
            },
            icon = { Icon(imageVector = Icons.Default.Star, contentDescription = "Back") },
        )

        SettingsSwitch(
            state = false,
            title = { Text(text = "Text correction") },
            subtitle = { Text(text = "Format the Output Text.") },
            modifier = Modifier,
            enabled = true,
            icon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Back") },
            onCheckedChange = { newState: Boolean -> },
        )
        SettingsMenuLink(
            title = { Text(text = "Delete Transcriptions") },
            subtitle = { Text(text = "Deletes all Transcriptions.") },
            onClick = {

            },
            icon = { Icon(imageVector = Icons.Default.Delete, contentDescription = "Back") },
        )



    }

    if(showDialog){
        BasicAlertDialog(onDismissRequest = { viewModel.hideDialog() }) {
            when(dialogType){
                0 -> ApiKeyDialog(viewModel)
                1 -> LanguageDialog(viewModel)
                2 -> ModelDialog(viewModel)
                3 -> DeleteDialog(viewModel)
            }
        }
    }
}