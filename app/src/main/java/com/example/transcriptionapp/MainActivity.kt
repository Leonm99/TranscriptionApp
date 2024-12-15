package com.example.transcriptionapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.transcriptionapp.ui.theme.TranscriptionAppTheme

class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<TranscriptionViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TranscriptionAppTheme {
                TranscriptionScreen(viewModel)

            }
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TranscriptionScreen(viewModel: TranscriptionViewModel) {
        val showBottomSheet by viewModel.showBottomSheet.collectAsState()
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        val isLoading by viewModel.isLoading.collectAsState()
        val transcription by viewModel.transcription.collectAsState()
        val activity = LocalContext.current
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == ComponentActivity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    viewModel.onAudioSelected(uri, activity)
                    }
                }
            }


            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
            if (isLoading) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)) // Darken background
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(100.dp)
                    )
                }
            } else {
                Button(onClick = {
                   viewModel.buttonOnClick(launcher)
                }) {
                    Text("Pick Audio")
                }
                }

            if (showBottomSheet) {
                ModalBottomSheet(
                    modifier = Modifier.fillMaxHeight(),
                    sheetState = sheetState,
                    onDismissRequest = { viewModel.hideBottomSheet() }
                ) {
                    // Display the transcription if available
                    transcription?.let {
                        Text(modifier = Modifier.padding(16.dp), text = "Transcription: $it")
                    }

                }
            }
        }

    }

    @Preview(showBackground = true)
    @Composable
    fun AppPreview() {
        TranscriptionAppTheme {
            TranscriptionScreen(viewModel)
        }
    }



}
