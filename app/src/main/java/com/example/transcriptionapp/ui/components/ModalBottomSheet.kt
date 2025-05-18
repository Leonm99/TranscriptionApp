package com.example.transcriptionapp.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.transcriptionapp.ui.theme.SpacingMedium
import com.example.transcriptionapp.ui.theme.SpacingSmall
import com.example.transcriptionapp.util.copyToClipboard
import com.example.transcriptionapp.viewmodel.BottomSheetViewModel
import eu.wewox.modalsheet.ExperimentalSheetApi
import eu.wewox.modalsheet.ModalSheet
import kotlinx.coroutines.launch

private const val PAGE_TRANSCRIPTION_IDX = 0

@OptIn(ExperimentalSheetApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ScrollableWithFixedPartsModalSheet(viewModel: BottomSheetViewModel) {
  val showBottomSheet by viewModel.isBottomSheetVisible.collectAsStateWithLifecycle()
  val transcriptionObject by viewModel.transcription.collectAsStateWithLifecycle()
  val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
  val errorText by viewModel.transcriptionError.collectAsStateWithLifecycle()

  val processingStep by viewModel.processingStep.collectAsStateWithLifecycle()
  val totalAudioCount by viewModel.totalAudioCount.collectAsStateWithLifecycle()
  val currentAudioIndex by viewModel.currentAudioIndex.collectAsStateWithLifecycle()

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  // --- PagerState Hoisting for TranscriptionCard's internal pager ---
  val pageCount = remember(transcriptionObject.summaryText, transcriptionObject.translationText) {
    1 + // Always have transcription page
            (if (!transcriptionObject.summaryText.isNullOrEmpty()) 1 else 0) +
            (if (!transcriptionObject.translationText.isNullOrEmpty()) 1 else 0)
  }
  val cardPagerState = rememberPagerState(pageCount = { pageCount })

  // Effect to adjust currentPage if pageCount changes (e.g., summary/translation added/removed)
  LaunchedEffect(pageCount, cardPagerState.settledPage) {
    if (cardPagerState.currentPage >= pageCount && pageCount > 0) {
      coroutineScope.launch {
        cardPagerState.animateScrollToPage(pageCount - 1)
      }
    }
  }

  // Effect to reset pager or handle state when bottom sheet visibility or main transcription data changes
  LaunchedEffect(showBottomSheet, transcriptionObject.transcriptionText) { // React to main transcription text
    if (showBottomSheet) {
      if (viewModel.isFirstOpenSinceTranscriptionUpdate.value || cardPagerState.currentPage >= pageCount) {
        val targetPage = if (cardPagerState.currentPage >= pageCount && pageCount > 0) pageCount - 1 else PAGE_TRANSCRIPTION_IDX
        coroutineScope.launch { cardPagerState.scrollToPage(targetPage) }
        viewModel.markAsNotFirstOpen() // Manage this flag in viewmodel
      }
    }
  }

  // Auto-scroll to summary when it becomes available
  val justSummarized by viewModel.justSummarized.collectAsStateWithLifecycle() // Add to ViewModel
  LaunchedEffect(justSummarized, transcriptionObject.summaryText, pageCount) {
    if (justSummarized && !transcriptionObject.summaryText.isNullOrEmpty()) {
      val summaryPageIndex = 1
      if (summaryPageIndex < pageCount) {
        coroutineScope.launch {
          cardPagerState.animateScrollToPage(summaryPageIndex)
        }
      }
      viewModel.clearJustSummarizedFlag()
    }
  }

  // Auto-scroll to translation when it becomes available
  val justTranslated by viewModel.justTranslated.collectAsStateWithLifecycle()
  LaunchedEffect(justTranslated, transcriptionObject.translationText, pageCount) {
    if (justTranslated && !transcriptionObject.translationText.isNullOrEmpty()) {
      val summaryExists = !transcriptionObject.summaryText.isNullOrEmpty()
      val translationPageIndex = if (summaryExists) 2 else 1
      if (translationPageIndex < pageCount) {
        coroutineScope.launch {
          cardPagerState.animateScrollToPage(translationPageIndex)
        }
      }
      viewModel.clearJustTranslatedFlag()
    }
  }

  ModalSheet(
    visible = showBottomSheet,
    onVisibleChange = { isVisible ->
      viewModel.toggleBottomSheet(isVisible)
      if (!isVisible) {
        // Reset pager to the first page when sheet is dismissed
        coroutineScope.launch { cardPagerState.scrollToPage(PAGE_TRANSCRIPTION_IDX) }
      }
    },
    onSystemBack = {
      var handledByPager = false
      if (cardPagerState.pageCount > 1 && cardPagerState.currentPage != PAGE_TRANSCRIPTION_IDX) {
        coroutineScope.launch {
          cardPagerState.animateScrollToPage(PAGE_TRANSCRIPTION_IDX)
        }
        handledByPager = true
      }
      if (!handledByPager) {
        viewModel.onSaveClick()
      }
    },
    cancelable = true,
    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    elevation = 0.dp,
    backgroundColor = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    Box(modifier = Modifier.fillMaxWidth()) { // Main container for sheet content
      HandleBar(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .width(50.dp)
          .padding(top = SpacingMedium)
          .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(2.dp))
      )
        // Content Area (TranscriptionCard and potentially loading/error states for it)
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 70.dp)
            .verticalScroll(rememberScrollState())
            .systemBarsPadding()
        ) {
          if (isLoading && errorText.isNullOrEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .size(240.dp)
                .padding(bottom = 25.dp),
              contentAlignment = Alignment.Center
            ) {
              CircularProgressIndicator(modifier = Modifier.size(100.dp))
              if (totalAudioCount > 1 && currentAudioIndex > 0) {
                val stepText = when (processingStep) {
                  BottomSheetViewModel.ProcessingStep.PROCESSING -> "Processing"
                  BottomSheetViewModel.ProcessingStep.TRANSCRIPTION -> "Transcribing"
                }
                Text(
                  modifier = Modifier.offset(y = (70).dp),
                  text = "$stepText $currentAudioIndex of $totalAudioCount",
                  color = MaterialTheme.colorScheme.primary,
                )
              } else {
                Text(
                  modifier = Modifier.offset(y = (70).dp),
                  text = "Loading...",
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
          } else { // Show Transcription Card or error related to it
            TranscriptionCard(
              modifier = Modifier.padding(horizontal = SpacingSmall).padding(top = SpacingSmall),
              pagerState = cardPagerState,
              transcription = transcriptionObject,
              onCopyClicked = { textToCopy -> copyToClipboard(context, textToCopy) },
              errorMessage = errorText,
              isSelected = false,
              isSelectionMode = false,
              onSelected = {},
              onClick = {},


              )

          }
        }

        // Fixed Bottom Action Buttons
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f))
            .padding(vertical = SpacingSmall, horizontal = SpacingSmall)
            .navigationBarsPadding(),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (errorText.isNullOrEmpty() || transcriptionObject.transcriptionText.isNotBlank()) {
            // Summarize Button
            Button(
              onClick = {
                // Check if summary content exists to navigate, otherwise fetch
                if (!transcriptionObject.summaryText.isNullOrEmpty()) {
                  val summaryPageIndex = 1
                  if (summaryPageIndex < cardPagerState.pageCount) {
                    coroutineScope.launch { cardPagerState.animateScrollToPage(summaryPageIndex) }
                  } else { // Should not happen if pageCount is correct
                    viewModel.onSummarizeClick()
                  }
                } else {
                  viewModel.onSummarizeClick()
                }
              },
              modifier = Modifier.weight(1f),
              shape = CircleShape,
              enabled = transcriptionObject.transcriptionText.isNotBlank() && !isLoading
            ) {
              Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "Summarize")
            }

            // Translate Button
            Button(
              onClick = {
                if (!transcriptionObject.translationText.isNullOrEmpty()) {
                  val summaryExists = !transcriptionObject.summaryText.isNullOrEmpty()
                  val translationPageIndex = if (summaryExists) 2 else 1
                  if (translationPageIndex < cardPagerState.pageCount) {
                    coroutineScope.launch { cardPagerState.animateScrollToPage(translationPageIndex) }
                  } else {
                    viewModel.onTranslateClick()
                  }
                } else {
                  viewModel.onTranslateClick()
                }
              },
              modifier = Modifier.weight(1f).padding(horizontal = SpacingSmall),
              shape = CircleShape,
              enabled = transcriptionObject.transcriptionText.isNotBlank() && !isLoading
            ) {
              Icon(Icons.Filled.Translate, contentDescription = "Translate")

            }

            // Save Button
            Button(
              onClick = { viewModel.onSaveClick() },
              modifier = Modifier.weight(1f),
              shape = CircleShape,
              enabled = transcriptionObject.transcriptionText.isNotBlank() && !isLoading
            ) {
              Icon(Icons.Filled.Save, contentDescription = "Save")
            }
          } else { // Error case (primary transcription error)
            Button(
              onClick = { viewModel.onRetryClick() },
              modifier = Modifier.fillMaxWidth().padding(horizontal = SpacingSmall),
              shape = CircleShape,
            ) {
              Icon(Icons.Default.Refresh, contentDescription = "Retry")
              Text("Retry", modifier = Modifier.padding(start = SpacingSmall))
            }
          }
        }

    }// End of outer Box
  } // End of ModalSheet
}