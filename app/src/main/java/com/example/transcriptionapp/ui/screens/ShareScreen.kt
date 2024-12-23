package com.example.transcriptionapp.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.example.transcriptionapp.ui.components.BottomSheet
import com.example.transcriptionapp.viewmodel.TranscriptionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(viewModel: TranscriptionViewModel, activity: ComponentActivity) {
  BottomSheet(viewModel, activity, true)
}
