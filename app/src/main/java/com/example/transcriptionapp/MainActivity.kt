package com.example.transcriptionapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.transcriptionapp.ui.screens.TranscriptionScreen
import com.example.transcriptionapp.ui.theme.TranscriptionAppTheme
import com.example.transcriptionapp.viewmodel.TranscriptionViewModel
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs

class MainActivity : ComponentActivity() {

  val viewModel by viewModels<TranscriptionViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { TranscriptionAppTheme { DestinationsNavHost(navGraph = NavGraphs.root) } }
  }

  @Preview(showBackground = true)
  @Composable
  fun AppPreview() {
    TranscriptionAppTheme { DestinationsNavHost(navGraph = NavGraphs.root) }
  }
}
