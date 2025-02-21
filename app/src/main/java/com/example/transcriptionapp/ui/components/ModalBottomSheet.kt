package com.example.transcriptionapp.ui.components

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.transcriptionapp.util.copyToClipboard
import com.example.transcriptionapp.viewmodel.BottomSheetViewModel
import io.morfly.compose.bottomsheet.material3.BottomSheetScaffold
import io.morfly.compose.bottomsheet.material3.rememberBottomSheetScaffoldState
import io.morfly.compose.bottomsheet.material3.rememberBottomSheetState
import kotlinx.coroutines.delay

enum class SheetValue {
  Hidden,
  PartiallyClosed,
  Expanded,
}

@OptIn(
  ExperimentalMaterial3Api::class,
  ExperimentalFoundationApi::class,
  ExperimentalLayoutApi::class,
)
@Composable
fun BottomSheet(
  viewModel: BottomSheetViewModel,
  activity: ComponentActivity? = null,
  finishAfter: Boolean? = false,
) {
  val showBottomSheet by viewModel.isBottomSheetVisible.collectAsStateWithLifecycle()
  val transcription = viewModel.transcription.collectAsStateWithLifecycle().value
  val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
  val shouldFinishActivity by viewModel.shouldFinishActivity.collectAsStateWithLifecycle()
  var hideContent by remember { mutableStateOf(false) }

  val context = LocalContext.current
  val sheetState =
    rememberBottomSheetState(
      initialValue = SheetValue.Expanded,
      defineValues = {
        SheetValue.Hidden at height(0.dp)
        SheetValue.PartiallyClosed at height(100.dp)
        SheetValue.Expanded at contentHeight
      },
    )
  val scaffoldState = rememberBottomSheetScaffoldState(sheetState)

  LaunchedEffect(sheetState.currentValue) {
    when (sheetState.currentValue) {
      SheetValue.Hidden -> {
        Log.d("BottomSheet", "Hidden")
        viewModel.hideBottomSheet()
        viewModel.clearTranscription()
        if (finishAfter == true) {
          viewModel.finishActivity()
        }
      }
      SheetValue.Expanded -> {
        viewModel.dontFinishActivity()
        hideContent = false
      }

      SheetValue.PartiallyClosed -> {
        hideContent = true
      }
    }
  }

  LaunchedEffect(showBottomSheet) {
    if (showBottomSheet) {
      sheetState.animateTo(SheetValue.Expanded)
    } else {
      sheetState.snapTo(SheetValue.Hidden)
    }
  }
  var delayedFinish by remember { mutableStateOf(true) }

  LaunchedEffect(shouldFinishActivity) {
    if (shouldFinishActivity && activity != null) {
      Log.d("BottomSheet", "shouldFinishActivity is true")
      delay(2000) // 2 seconds delay
      if (shouldFinishActivity && delayedFinish) { // Double check after delay
        Log.d("BottomSheet", "Finish Activity triggered after 2 seconds delay")
        activity.finish()
      } else {
        delayedFinish = false
        Log.d(
          "BottomSheet",
          "shouldFinishActivity changed to false within 2 seconds, activity finish cancelled",
        )
      }
    }
  }

  if (showBottomSheet) {
    BottomSheetScaffold(
      modifier = Modifier,
      scaffoldState = scaffoldState,
      containerColor = MaterialTheme.colorScheme.secondaryContainer,
      sheetContent = {
        val bottomPadding =
          if (WindowInsets.navigationBars.getBottom(LocalDensity.current) > 0) {
            WindowInsets.navigationBars.getBottom(LocalDensity.current).dp / 2 - 20.dp
          } else {
            0.dp
          }
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .heightIn(min = 50.dp)
              .wrapContentHeight()
              .padding(horizontal = 10.dp, vertical = 5.dp)
              .padding(bottom = bottomPadding), // Apply bottomPadding here
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          if (isLoading) {
            Box(
              modifier = Modifier.fillMaxWidth().padding(bottom = 50.dp),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator(modifier = Modifier.size(100.dp))
              Text(modifier = Modifier.offset(y = (70).dp), text = "Loading...")
            }
          } else {
            AnimatedVisibility(visible = !hideContent, enter = fadeIn(), exit = fadeOut()) {
              Column(modifier = Modifier) {
                TranscriptionCard(
                  modifier = Modifier.animateContentSize(),
                  transcription,
                  { text -> copyToClipboard(context, text) },
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
                    onClick = {
                      viewModel.showToast(context, "Saved")
                      viewModel.onSaveClick()
                    },
                    modifier = Modifier,
                    shape = CircleShape,
                  ) {
                    Icon(Icons.Filled.Save, contentDescription = "Save", modifier = Modifier)
                  }
                }

                HorizontalDivider(Modifier.padding(8.dp))

                if (bottomPadding < 0.dp) {
                  Log.d("BottomSheet", "Spacer added")
                  Spacer(modifier = Modifier.padding(vertical = 10.dp))
                }
              }
            }
          }
        }
        // Bottom sheet content
      },
      content = {},
    )
  }
}
