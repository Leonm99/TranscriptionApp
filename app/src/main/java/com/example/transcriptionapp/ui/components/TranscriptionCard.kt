package com.example.transcriptionapp.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.transcriptionapp.com.example.transcriptionapp.ui.components.verticalScrollbar
import com.example.transcriptionapp.viewmodel.formatTimestamp

@Composable
fun TranscriptionCard(
  transcription: String,
  summary: String?,
  translation: String?,
  timestamp: String,
  onCopyClicked: (String) -> Unit,
) {
  val pageCount = 1 + (if (summary != null) 1 else 0) + (if (translation != null) 1 else 0)
  val pagerState = rememberPagerState(pageCount = { pageCount })

  Card(
    modifier =
      Modifier.fillMaxWidth().wrapContentHeight().padding(5.dp).heightIn(max = 325.dp, min = 50.dp),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Column(modifier = Modifier) {
      HorizontalPager(
        state = pagerState,
        modifier =
          Modifier.wrapContentHeight()
            .heightIn(max = 300.dp)
            .padding(vertical = 16.dp, horizontal = 10.dp)
            .animateContentSize(
              animationSpec =
                tween(
                  durationMillis = 175, // Adjust the duration as needed
                  easing = EaseInOut, // Optional, change if desired
                )
            ),
      ) { page ->
        val scrollState = rememberScrollState()
        Box(modifier = Modifier.fillMaxWidth().heightIn(max = 275.dp)) {
          Column(modifier = Modifier.fillMaxWidth()) {
            if (page == 0) {
              Text(text = "Transcription", style = MaterialTheme.typography.titleMedium)

              Text(
                modifier = Modifier.padding(bottom = 8.dp),
                text = timestamp,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
              )

              Text(
                modifier =
                  Modifier.fillMaxWidth()
                    .wrapContentHeight()
                    .nestedScroll(rememberNestedScrollInteropConnection())
                    .verticalScroll(scrollState)
                    .verticalScrollbar(scrollState),
                text = transcription,
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyMedium,
              )
            } else if (page == 1 && !summary.isNullOrEmpty()) {
              Text(text = "Summary", style = MaterialTheme.typography.titleMedium)

              Text(
                modifier = Modifier.padding(bottom = 8.dp),
                text = timestamp,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
              )

              Text(
                modifier =
                  Modifier.fillMaxWidth()
                    .wrapContentHeight()
                    .nestedScroll(rememberNestedScrollInteropConnection())
                    .verticalScroll(scrollState)
                    .verticalScrollbar(scrollState),
                text = summary,
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyMedium,
              )
            } else if (
              page == (if (!summary.isNullOrEmpty()) 2 else 1) && !translation.isNullOrEmpty()
            ) {

              Text(text = "Translation", style = MaterialTheme.typography.titleMedium)

              Text(
                modifier = Modifier.padding(bottom = 8.dp),
                text = timestamp,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
              )
              val scrollState = rememberScrollState()

              Text(
                modifier =
                  Modifier.fillMaxWidth()
                    .wrapContentHeight()
                    .nestedScroll(rememberNestedScrollInteropConnection())
                    .verticalScroll(scrollState)
                    .verticalScrollbar(scrollState),
                text = translation,
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.bodyMedium,
              )
            }
          }

          IconButton(
            onClick = {
              onCopyClicked(
                if (pagerState.currentPage == 0) transcription
                else if (pagerState.currentPage == 1 && summary != null) summary
                else translation ?: "WOW HOW DID THIS HAPPEN?!"
              )
            },
            modifier = Modifier.align(Alignment.TopEnd),
          ) {
            Icon(
              Icons.Filled.ContentCopy,
              contentDescription = "Copy",
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }

      Row(
        Modifier.fillMaxWidth().wrapContentHeight().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        repeat(pagerState.pageCount) { iteration ->
          val color = if (pagerState.currentPage == iteration) Color.DarkGray else Color.LightGray
          Box(modifier = Modifier.padding(2.dp).clip(CircleShape).background(color).size(5.dp))
        }
      }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun TranscriptionCardPreview() {
  TranscriptionCard(
    transcription = "This is a sample transcription that will be copied. ".repeat(20),
    summary = "This is a sample summary.",
    translation = "This is a sample translation.".repeat(20),
    timestamp = formatTimestamp(System.currentTimeMillis()),
    onCopyClicked = {},
  )
}
