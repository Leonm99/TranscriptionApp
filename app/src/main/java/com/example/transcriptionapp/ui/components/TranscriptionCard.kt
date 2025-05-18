package com.example.transcriptionapp.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SignalCellularConnectedNoInternet0Bar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.transcriptionapp.com.example.transcriptionapp.model.database.Transcription
import com.example.transcriptionapp.ui.theme.SpacingMedium
import com.example.transcriptionapp.ui.theme.SpacingSmall
import com.example.transcriptionapp.viewmodel.formatTimestamp
import eu.wewox.modalsheet.ExperimentalSheetApi

@OptIn(ExperimentalSheetApi::class)
@Composable
@Preview
fun TranscriptionCardPreview() {
  val previewPagerState = rememberPagerState(pageCount = { 1 })

  TranscriptionCard(
    pagerState = previewPagerState,
    transcription =
      Transcription(
        0,
        "Transcription text",
        "Summary text",
        "Translation text",
        formatTimestamp(System.currentTimeMillis()),
      ),
    onCopyClicked = {},
    isSelected = false,
    onSelected = {},
  )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TranscriptionCard(
  modifier: Modifier = Modifier,
  pagerState: PagerState,
  transcription: Transcription,
  onCopyClicked: (String) -> Unit,
  isSelected: Boolean = false,
  isSelectionMode: Boolean = false,
  onSelected: () -> Unit,
  errorMessage: String? = null,
) {


  val titleText =
    when (pagerState.currentPage) {
      0 -> "Transcription"
      1 -> if (!transcription.summaryText.isNullOrEmpty()) "Summary" else "Translation"
      2 -> "Translation"
      else -> "Transcription" // Fallback, should not happen
    }

  Card(
    modifier =
      modifier
        .fillMaxWidth()
        .wrapContentHeight()
        .combinedClickable(
          onClick = { if (isSelectionMode) onSelected() },
          onLongClick = { onSelected() },
        ),
    shape = RoundedCornerShape(12.dp),
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (errorMessage == null) MaterialTheme.colorScheme.secondaryContainer
          else MaterialTheme.colorScheme.errorContainer
      ),
  ) {
    if (errorMessage != null) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(15.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        if (errorMessage.contains("No network")) {
          Icon(
            modifier = Modifier.size(50.dp),
            imageVector = Icons.Filled.SignalCellularConnectedNoInternet0Bar,
            contentDescription = "Copy",
          )
        } else {
          Icon(
            modifier = Modifier.size(50.dp),
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = "Copy",
          )
        }

        Text(
          modifier = Modifier.padding(top = 10.dp),
          text = errorMessage,
          style = MaterialTheme.typography.bodyMedium,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.error,
        )
      }
    } else {
      Box(contentAlignment = Alignment.BottomCenter) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingMedium),
          verticalArrangement = Arrangement.Top,
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 4.dp, bottom = SpacingSmall),
            verticalAlignment = Alignment.CenterVertically, // Centers the items vertically
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Column(
              modifier = Modifier
                .wrapContentSize()
                .offset(y = 11.dp),
              horizontalAlignment = Alignment.Start,
            ) {
              Text(
                text = titleText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )

              Text(
                modifier = Modifier,
                text = transcription.timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            }

            if (isSelectionMode) {
              Checkbox(
                checked = isSelected,
                onCheckedChange = { onSelected() },
                modifier = Modifier,
              )
            } else {
              IconButton(
                onClick = {
                  val currentText = when (pagerState.currentPage) {
                    0 -> transcription.transcriptionText
                    1 -> if (!transcription.summaryText.isNullOrEmpty()) transcription.summaryText else transcription.translationText
                    2 -> transcription.translationText
                    else -> ""
                  }
                  currentText?.let { onCopyClicked(it) }
            },
                modifier = Modifier
                  .size(25.dp)
                  .padding(top = 4.dp),
              ) {
                Icon(
                  Icons.Filled.ContentCopy,
                  contentDescription = "Copy",
                  modifier = Modifier.size(20.dp),
                )
              }
            }
          }

          HorizontalPager( // Use the passed pagerState
            state = pagerState,
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 50.dp, max = 250.dp) // Or your desired height
              .padding(top = 4.dp),
          ) { page -> // page here is the index
            val scrollState = rememberScrollState()
            val currentDisplayTranscription = transcription // To avoid confusion with outer scope

            val textToShow = when (page) {
              0 -> currentDisplayTranscription.transcriptionText
              1 -> if (!currentDisplayTranscription.summaryText.isNullOrEmpty()) {
                currentDisplayTranscription.summaryText
              } else { // Fallback to translation if summary is page 1 but empty
                currentDisplayTranscription.translationText
              }
              2 -> currentDisplayTranscription.translationText
              else -> "" // Should not happen if pageCount is correct
            }

            if (!textToShow.isNullOrEmpty()) {
              Text(
                modifier = Modifier
                  .fillMaxWidth()
                  .wrapContentHeight()
                  .nestedScroll(rememberNestedScrollInteropConnection())
                  .verticalScroll(scrollState)
                  .verticalScrollbar(scrollState),
                text = textToShow,
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            } else {
              // Optional: Show a placeholder if text is null/empty for a page
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Content not available.", style = MaterialTheme.typography.bodyMedium)
              }
            }
          }

          if (pagerState.pageCount > 1) { // Use pagerState.pageCount
            Row(
              Modifier
                .wrapContentHeight()
                .fillMaxWidth()
                .padding(bottom = 5.dp, top = 5.dp),
              horizontalArrangement = Arrangement.Center,
            ) {
              repeat(pagerState.pageCount) { iteration ->
                val color =
                  if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.onPrimaryContainer
                  else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f) // Dim inactive
                Box(
                  modifier = Modifier
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(color)
                    .size(8.dp) // Slightly bigger dots
                )
              }
            }
          } else {
            Spacer(modifier = Modifier.height(10.dp))
          }
        }
      }
    }
  }
}