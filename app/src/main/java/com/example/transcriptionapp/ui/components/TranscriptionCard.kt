package com.example.transcriptionapp.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
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
import com.example.transcriptionapp.com.example.transcriptionapp.ui.components.verticalScrollbar
import com.example.transcriptionapp.viewmodel.formatTimestamp

@Composable
@Preview
fun TranscriptionCardPreview() {
  TranscriptionCard(
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
  transcription: Transcription,
  onCopyClicked: (String) -> Unit,
  isSelected: Boolean = false,
  isSelectionMode: Boolean = false,
  onSelected: () -> Unit,
) {
  val pageCount =
    1 +
      (if (transcription.summaryText != null) 1 else 0) +
      (if (transcription.translationText != null) 1 else 0)
  val pagerState = rememberPagerState(pageCount = { pageCount })
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
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
  ) {
    // Use Box to stack elements and position dots at the bottom
    Box(contentAlignment = Alignment.BottomCenter) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(15.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically, // Centers the items vertically
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(
            modifier = Modifier.wrapContentSize().offset(y = 6.dp),
            horizontalAlignment = Alignment.Start,
          ) {
            Text(text = titleText, style = MaterialTheme.typography.titleMedium)

            Text(
              modifier = Modifier.padding(bottom = 8.dp),
              text = transcription.timestamp,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.outline,
            )
          }

          if (isSelectionMode) {
            Checkbox(checked = isSelected, onCheckedChange = { onSelected() }, modifier = Modifier)
          } else {
            IconButton(
              onClick = {
                onCopyClicked(
                  if (pagerState.currentPage == 0) transcription.transcriptionText
                  else if (pagerState.currentPage == 1 && transcription.summaryText != null)
                    transcription.summaryText
                  else transcription.translationText ?: "WOW HOW DID THIS HAPPEN?!"
                )
              },
              modifier = Modifier,
            ) {
              Icon(
                Icons.Filled.ContentCopy,
                contentDescription = "Copy",
                modifier = Modifier.size(20.dp),
              )
            }
          }
        }

        HorizontalPager(
          state = pagerState,
          modifier =
            Modifier.fillMaxWidth().heightIn(min = 50.dp, max = 200.dp).animateContentSize(),
        ) { page ->
          val scrollState = rememberScrollState()

          Column(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
            if (page == 0) {
              Text(
                modifier =
                  Modifier.fillMaxWidth()
                    .wrapContentHeight()
                    .nestedScroll(rememberNestedScrollInteropConnection())
                    .verticalScroll(scrollState)
                    .verticalScrollbar(scrollState)
                    .padding(top = 8.dp),
                text = transcription.transcriptionText,
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyMedium,
              )
            } else if (page == 1 && !transcription.summaryText.isNullOrEmpty()) {
              Text(
                modifier =
                  Modifier.fillMaxWidth()
                    .wrapContentHeight()
                    .nestedScroll(rememberNestedScrollInteropConnection())
                    .verticalScroll(scrollState)
                    .verticalScrollbar(scrollState)
                    .padding(top = 8.dp),
                text = transcription.summaryText,
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyMedium,
              )
            } else if (
              page == (if (!transcription.summaryText.isNullOrEmpty()) 2 else 1) &&
                !transcription.translationText.isNullOrEmpty()
            ) {
              val scrollState = rememberScrollState()

              Text(
                modifier =
                  Modifier.fillMaxWidth()
                    .wrapContentHeight()
                    .nestedScroll(rememberNestedScrollInteropConnection())
                    .verticalScroll(scrollState)
                    .verticalScrollbar(scrollState)
                    .padding(top = 8.dp),
                text = transcription.translationText,
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyMedium,
              )
            }
          }
        }
      }

      // Dots Row is now inside the Box and aligned to the bottom
      Row(
        Modifier.wrapContentHeight().fillMaxWidth().padding(bottom = 5.dp, top = 5.dp),
        horizontalArrangement = Arrangement.Center,
      ) {
        repeat(pagerState.pageCount) { iteration ->
          val color =
            if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSecondary
          Box(modifier = Modifier.padding(2.dp).clip(CircleShape).background(color).size(4.dp))
        }
      }
    }
  }
}
