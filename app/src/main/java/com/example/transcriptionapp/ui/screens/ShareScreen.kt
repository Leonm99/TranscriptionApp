package com.example.transcriptionapp.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import com.example.transcriptionapp.ui.components.BottomSheet
import com.example.transcriptionapp.viewmodel.BottomSheetViewModel

@Composable
fun ShareScreen(viewModel: BottomSheetViewModel, activity: ComponentActivity) {
  BottomSheet(viewModel, activity, true)
}
