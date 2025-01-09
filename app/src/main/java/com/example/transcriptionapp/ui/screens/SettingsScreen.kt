package com.example.transcriptionapp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
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
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.alorma.compose.settings.ui.SettingsSwitch
import com.example.transcriptionapp.ui.components.ApiKeyDialog
import com.example.transcriptionapp.ui.components.DeleteDialog
import com.example.transcriptionapp.ui.components.LanguageDialog
import com.example.transcriptionapp.ui.components.ModelDialog
import com.example.transcriptionapp.viewmodel.DialogType
import com.example.transcriptionapp.viewmodel.SettingsViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBackClick: () -> Unit, viewModel: SettingsViewModel) {




    val showDialog by viewModel.showDialog.collectAsState()
    val dialogType by viewModel.dialogType.collectAsState()
    val settings by viewModel.settings.collectAsState()

    Column(modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally) {


        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(
                    onClick = {
                        onBackClick()
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
            subtitle = { Text(text = settings.userApiKey) },
            onClick = {
                viewModel.showDialog(DialogType.API)
            },
            icon = { Icon(imageVector = Icons.Default.Key, contentDescription = "API Key") },
        )

        SettingsMenuLink(
            title = { Text(text = "Language") },
            subtitle = { Text(text = settings.selectedLanguage) },
            onClick = {
                viewModel.showDialog(DialogType.LANGUAGE)
            },
            icon = { Icon(imageVector = Icons.Default.Language, contentDescription = "Language") },
        )

        SettingsMenuLink(
            title = { Text(text = "Model") },
            subtitle = { Text(text = settings.selectedModel) },
            onClick = {
                viewModel.showDialog(DialogType.MODEL)
            },
            icon = { Icon(imageVector = Icons.Default.Bolt, contentDescription = "Model") },
        )

        SettingsSwitch(
            state = settings.formatSwitchState,
            title = { Text(text = "Text correction") },
            subtitle = { Text(text = "Format the Output Text.") },
            icon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Format") },
            onCheckedChange = {viewModel.updateSwitchState(it)
            },
        )
        SettingsMenuLink(
            title = { Text(text = "Delete Transcriptions") },
            subtitle = { Text(text = "Deletes all Transcriptions.") },
            onClick = {
                viewModel.showDialog(DialogType.DELETE)
            },
            icon = { Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete") },
        )



    }

    if(showDialog){
            when(dialogType){
                DialogType.API -> ApiKeyDialog(viewModel)
                DialogType.LANGUAGE-> LanguageDialog(viewModel, settings.selectedLanguage)
                DialogType.MODEL -> ModelDialog(viewModel, settings.selectedModel)
                DialogType.DELETE -> DeleteDialog(viewModel)
            }

    }
}