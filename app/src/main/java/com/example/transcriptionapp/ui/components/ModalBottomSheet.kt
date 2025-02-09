package com.example.transcriptionapp.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.composables.core.DragIndication
import com.composables.core.ModalBottomSheet
import com.composables.core.Sheet
import com.composables.core.SheetDetent
import com.composables.core.SheetDetent.Companion.FullyExpanded
import com.composables.core.SheetDetent.Companion.Hidden
import com.composables.core.rememberModalBottomSheetState
import com.example.transcriptionapp.viewmodel.BottomSheetViewModel

private val Peek = SheetDetent("peek") { containerHeight, sheetHeight -> containerHeight * 0.6f }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BottomSheet(
  viewModel: BottomSheetViewModel,
  activity: ComponentActivity? = null,
  finishAfter: Boolean? = false,
) {

  val transcription = viewModel.transcription.collectAsState().value
  val isLoading by viewModel.isLoading.collectAsState()
  val showBottomSheet by viewModel.isBottomSheetVisible.collectAsState()

  val sheetState =
    rememberModalBottomSheetState(
      initialDetent = Hidden,
      detents = listOf(Hidden, Peek, FullyExpanded),
    )

  val context = LocalContext.current

  if (showBottomSheet) {

    ModalBottomSheet(state = sheetState) {
      Sheet(modifier = Modifier.fillMaxWidth().background(Color.White)) {
        Box(
          modifier = Modifier.fillMaxWidth().height(1200.dp),
          contentAlignment = Alignment.TopCenter,
        ) {
          DragIndication()
        }

        Column(
          modifier =
            Modifier.fillMaxWidth()
              .wrapContentHeight()
              .padding(horizontal = 10.dp, vertical = 5.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          if (isLoading) {
            Box(
              modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator(modifier = Modifier.size(100.dp))
              Text(modifier = Modifier.offset(y = (70).dp), text = "Loading...")
            }
          } else {
            Column(modifier = Modifier) {
              TranscriptionCard(
                transcription,
                { text -> viewModel.copyToClipboard(context, text) },
                false,
                false,
                {},
              )
              HorizontalDivider(Modifier.padding(8.dp))

              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 5.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Button(
                  onClick = { viewModel.onSummarizeClick() },
                  modifier = Modifier,
                  shape = CircleShape,
                ) {
                  Icon(
                    Icons.AutoMirrored.Filled.Message,
                    contentDescription = "Summarize",
                    modifier = Modifier,
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
                    modifier = Modifier,
                  )
                }
                Button(
                  onClick = { viewModel.onSaveClick() },
                  modifier = Modifier,
                  shape = CircleShape,
                ) {
                  Icon(Icons.Filled.Save, contentDescription = "Save", modifier = Modifier)
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(16.dp))
        }
      }
    }
  }
}
