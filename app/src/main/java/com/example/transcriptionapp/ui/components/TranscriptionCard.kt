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
      )
  ) {}
}

@Composable
fun TranscriptionCard(transcription: Transcription, onCopyClicked: (String) -> Unit) {
  val pageCount =
    1 +
      (if (transcription.summaryText != null) 1 else 0) +
      (if (transcription.translationText != null) 1 else 0)
  val pagerState = rememberPagerState(pageCount = { pageCount })

  Card(
    modifier =
      Modifier.fillMaxWidth().wrapContentHeight().padding(5.dp).heightIn(max = 500.dp, min = 50.dp),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
          state = pagerState,
          modifier =
            Modifier.fillMaxWidth()
              .heightIn(min = 50.dp, max = 300.dp)
              .padding(vertical = 10.dp, horizontal = 10.dp)
              .animateContentSize(animationSpec = tween(durationMillis = 175, easing = EaseInOut)),
        ) { page ->
          when (page) {
            0 ->
              TranscriptionContent(
                transcription.transcriptionText,
                transcription.timestamp!!,
                "Transcription",
              )
            1 ->
              if (!transcription.summaryText.isNullOrEmpty())
                TranscriptionContent(
                  transcription.summaryText,
                  transcription.timestamp!!,
                  "Summary",
                )
              else Box {}
            2 ->
              if (!transcription.translationText.isNullOrEmpty())
                TranscriptionContent(
                  transcription.translationText,
                  transcription.timestamp!!,
                  "Translation",
                )
              else Box {}
          }
        }

        IconButton(
          onClick = {
            onCopyClicked(
              when (pagerState.currentPage) {
                0 -> transcription.transcriptionText
                1 -> transcription.summaryText ?: "No summary text"
                2 -> transcription.translationText ?: "No translation text"
                else -> ""
              }
            )
          },
          modifier = Modifier.align(Alignment.TopStart).padding(end = 8.dp, top = 8.dp),
        ) {
          Icon(
            Icons.Filled.ContentCopy,
            contentDescription = "stringResource(id = R.string.copy_content)",
          )
        }
      }

      Row(
        Modifier.wrapContentHeight().fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.Center,
      ) {
        repeat(pagerState.pageCount) { iteration ->
          val color = if (pagerState.currentPage == iteration) Color.DarkGray else Color.LightGray
          Box(modifier = Modifier.padding(2.dp).clip(CircleShape).background(color).size(4.dp))
        }
      }
    }
  }
}

@Composable
fun TranscriptionContent(text: String, timestamp: String, title: String) {
  val scrollState = rememberScrollState()
  Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(text = title, style = MaterialTheme.typography.titleMedium)

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
          .verticalScrollbar(scrollState = scrollState),
      text = text,
      textAlign = TextAlign.Start,
      style = MaterialTheme.typography.bodyMedium,
    )
  }
}
