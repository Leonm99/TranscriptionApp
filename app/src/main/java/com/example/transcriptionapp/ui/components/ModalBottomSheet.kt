package com.example.transcriptionapp.ui.components

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.transcriptionapp.viewmodel.BottomSheetViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BottomSheet(
  viewModel: BottomSheetViewModel,
  activity: ComponentActivity? = null,
  finishAfter: Boolean? = false,
) {
  val showBottomSheet by viewModel.isBottomSheetVisible.collectAsState()
  val transcription = viewModel.transcription.collectAsState().value
  val isLoading by viewModel.isLoading.collectAsState()

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val context = LocalContext.current
  val screenHeight = LocalConfiguration.current.screenHeightDp.dp

  var transcriptionCardHeight by remember { mutableStateOf(Dp.Unspecified) }

  val targetTopPadding =
    if (transcriptionCardHeight > 410.0.dp) {
      screenHeight / 2 - 30.dp
    } else {
      screenHeight - 300.dp
    }

  val animatedTopPadding by
    animateDpAsState(
      targetValue = targetTopPadding,
      animationSpec = tween(durationMillis = 300),
      label = "BottomSheetTopPaddingAnimation",
    )

  if (showBottomSheet) {
    ModalBottomSheet(
      modifier = Modifier.padding(top = animatedTopPadding.coerceAtLeast(0.dp)),
      sheetState = sheetState,
      onDismissRequest = {
        viewModel.hideBottomSheet()
        if (activity != null && finishAfter == true) {
          activity.finish()
        }
      },
    ) {
      Column(
        modifier =
          Modifier.fillMaxWidth().wrapContentHeight().padding(horizontal = 10.dp, vertical = 5.dp),
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
            TranscriptionCard(
              transcription,
              { text -> viewModel.copyToClipboard(context, text) },
              false,
              false,
              {},
              modifier =
                Modifier.onGloballyPositioned { layoutCoordinates ->
                  transcriptionCardHeight = layoutCoordinates.size.height.dp
                  Log.d("BottomSheet", "TranscriptionCard Height: $transcriptionCardHeight")
                },
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
                Icon(Icons.Filled.Translate, contentDescription = "Translate", modifier = Modifier)
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
      }
    }
  }
}
