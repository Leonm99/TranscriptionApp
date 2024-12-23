package com.example.transcriptionapp.ui.components

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import com.example.transcriptionapp.viewmodel.TranscriptionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheet(
    viewModel: TranscriptionViewModel,
    activity: ComponentActivity? = null,
    finishAfter: Boolean? = false
) {

  val showBottomSheet by viewModel.showBottomSheet.collectAsState()
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
  val isLoading by viewModel.isLoading.collectAsState()
  val transcription by viewModel.transcription.collectAsState()

  if (showBottomSheet) {

    ModalBottomSheet(
        modifier = Modifier.fillMaxHeight().padding(top = 50.dp),
        sheetState = sheetState,
        onDismissRequest = {
          viewModel.hideBottomSheet()
          if (activity != null && finishAfter == true) {
            Log.d("ModalBottomSheet", "FINISH")
            activity.finish()
          }
        }) {
          if (isLoading) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              CircularProgressIndicator(modifier = Modifier.size(100.dp))
            }
          } else {
            Column {
              transcription?.let {
                Text(modifier = Modifier.padding(16.dp), text = "Transcription: $it")
              }
            }
          }
        }
  }
}
