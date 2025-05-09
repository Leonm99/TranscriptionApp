package com.example.transcriptionapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.transcriptionapp.ui.theme.SpacingSmall
import com.example.transcriptionapp.util.copyToClipboard
import com.example.transcriptionapp.viewmodel.BottomSheetViewModel
import eu.wewox.modalsheet.ExperimentalSheetApi
import eu.wewox.modalsheet.ModalSheet

@OptIn(ExperimentalSheetApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScrollableWithFixedPartsModalSheet(viewModel: BottomSheetViewModel) {
  val showBottomSheet by viewModel.isBottomSheetVisible.collectAsStateWithLifecycle()
  val transcription = viewModel.transcription.collectAsStateWithLifecycle().value
  val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
  val errorText by viewModel.transcriptionError.collectAsStateWithLifecycle()

  val context = LocalContext.current

  ModalSheet(
    visible = showBottomSheet,
    onVisibleChange = { viewModel.toggleBottomSheet(it) },
    onSystemBack = { viewModel.onSaveClick() },
    cancelable = true,
    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    elevation = 0.dp,
    backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    Box(modifier = Modifier.fillMaxWidth()) {
      Column(
        modifier =
          Modifier.verticalScroll(rememberScrollState())
            .systemBarsPadding()
            .padding(bottom = 64.dp)
            .align(Alignment.BottomCenter)
      ) {

        // ###################################CONTENT############################################
        if (isLoading) {
          Box(
            modifier = Modifier.fillMaxWidth().padding(bottom = 50.dp),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(modifier = Modifier.size(100.dp))
            Text(
              modifier = Modifier.offset(y = (70).dp),
              text = "Loading...",
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        } else {
          Column(modifier = Modifier) {
            TranscriptionCard(
              modifier = Modifier.padding(horizontal = SpacingSmall),
              transcription,
              { text -> copyToClipboard(context, text) },
              false,
              false,
              {},
              errorText,
            )
          }
        }
        // ###################################CONTENT############################################
      }

      Row(
        modifier =
          Modifier.fillMaxWidth()
            .align(Alignment.BottomCenter)
            .padding(4.dp)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (errorText.isNullOrEmpty()) {
          Button(
            onClick = { viewModel.onSummarizeClick() },
            modifier = Modifier,
            shape = CircleShape,
          ) {
            Icon(
              Icons.AutoMirrored.Filled.Message,
              contentDescription = "Summarize",
              modifier = Modifier.padding(horizontal = 10.dp),
            )
          }
          Button(
            onClick = { viewModel.onTranslateClick() },
            modifier = Modifier,
            shape = CircleShape,
          ) {
            Icon(
              Icons.Filled.Translate,
              contentDescription = "Translate",
              modifier = Modifier.padding(horizontal = 10.dp),
            )
          }
          Button(onClick = { viewModel.onSaveClick() }, modifier = Modifier, shape = CircleShape) {
            Icon(
              Icons.Filled.Save,
              contentDescription = "Save",
              modifier = Modifier.padding(horizontal = 10.dp),
            )
          }
        } else {
          Button(
            onClick = { viewModel.onRetryClick() },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp),
            shape = CircleShape,
          ) {
            Icon(Icons.Default.Refresh, contentDescription = "Retry", modifier = Modifier)
          }
        }
      }
    }
  }
}
