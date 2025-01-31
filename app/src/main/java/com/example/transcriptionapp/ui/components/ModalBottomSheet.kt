package com.example.transcriptionapp.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.transcriptionapp.viewmodel.TranscriptionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheet(
  viewModel: TranscriptionViewModel,
  activity: ComponentActivity? = null,
  finishAfter: Boolean? = false,
) {
  val showBottomSheet by viewModel.isBottomSheetVisible.collectAsState()
  val transcription = viewModel.transcription.collectAsState().value
  val isLoading by viewModel.isLoading.collectAsState()

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val context = LocalContext.current

  val screenHeight = LocalConfiguration.current.screenHeightDp.dp
  val halfScreenHeight = screenHeight / 2
  Box(modifier = Modifier.fillMaxSize()) {
    if (showBottomSheet) {
      ModalBottomSheet(
        modifier = Modifier.padding(top = halfScreenHeight),
        sheetState = sheetState,
        onDismissRequest = {
          viewModel.hideBottomSheet()
          if (activity != null && finishAfter == true) {
            activity.finish()
          }
        },
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          if (isLoading) {
            Box(
              modifier = Modifier.fillMaxWidth().fillMaxHeight(),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator(modifier = Modifier.size(100.dp))
              Text(modifier = Modifier.offset(y = (70).dp), text = "Loading...")
            }
          } else {
            Column(modifier = Modifier.weight(1f)) {
              TranscriptionCard(transcription) { text -> viewModel.copyToClipboard(context, text) }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              horizontalArrangement = Arrangement.SpaceEvenly,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              StickyBottomSheetButton(
                onClick = { viewModel.onSummarizeClick() },
                text = "Summarize",
              )
              StickyBottomSheetButton(
                onClick = { viewModel.onTranslateClick() },
                text = "Translate",
              )
              StickyBottomSheetButton(onClick = { viewModel.onSaveClick() }, text = "Save")
            }
          }
        }
      }
    }
  }
}
