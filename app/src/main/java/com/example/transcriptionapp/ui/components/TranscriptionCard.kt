package com.example.transcriptionapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.transcriptionapp.model.database.Transcription
import com.example.transcriptionapp.ui.theme.SpacingMedium
import com.example.transcriptionapp.ui.theme.SpacingSmall
import com.example.transcriptionapp.util.formatTimestamp



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TranscriptionCard(
  modifier: Modifier = Modifier,
  pagerState: PagerState,
  transcription: Transcription,
  onCopyClicked: (String) -> Unit,
  isSelected: Boolean = false,
  isSelectionMode: Boolean = false,
  onSelected: () -> Unit, // This is for toggling selection
  onClick: () -> Unit,    // This is for the primary action when not in selection mode
  errorMessage: String? = null,
) {
  val interactionSource = remember { MutableInteractionSource() }

  val cardColors = when {
    errorMessage != null -> CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.errorContainer,
      contentColor = MaterialTheme.colorScheme.onErrorContainer
    )
    isSelected -> CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), // Slightly different selected color
      contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
    else -> CardDefaults.cardColors( // Default state
      containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp), // Elevated surface
      contentColor = MaterialTheme.colorScheme.onSurface
    )
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .wrapContentHeight()
      .clip(RoundedCornerShape(16.dp)) // Increased corner radius for a softer look
      .combinedClickable(
        interactionSource = interactionSource,
        indication = ripple(),
        onClick = {
          if (isSelectionMode) onSelected() else onClick()
        },
        onLongClick = {
          if (!isSelectionMode) onSelected() // Enter selection mode on long press
        },
        role = Role.Button
      )
      .animateContentSize(), // Smoothly animate size changes
    shape = RoundedCornerShape(16.dp),
    colors = cardColors,
    elevation = CardDefaults.cardElevation(
      defaultElevation = if (isSelected) 4.dp else 2.dp, // More elevation when selected
      pressedElevation = 8.dp
    )
  ) {
    Column(
      modifier = Modifier.padding(SpacingMedium) // Consistent padding for content
    ) {
      if (errorMessage != null) {
        ErrorDisplay(errorMessage)
      } else {
        NormalContent(
          pagerState = pagerState,
          transcription = transcription,
          onCopyClicked = onCopyClicked,
          isSelectionMode = isSelectionMode,
          isSelected = isSelected,
          onSelected = onSelected
        )
      }
    }
  }
}

@Composable
private fun ErrorDisplay(errorMessage: String) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = SpacingMedium, horizontal = SpacingSmall), // Adjusted padding
    verticalArrangement = Arrangement.spacedBy(SpacingSmall, Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    val errorIcon: ImageVector
    val errorDescription: String
    if (errorMessage.contains("No network", ignoreCase = true)) {
      errorIcon = Icons.Filled.CloudOff
      errorDescription = "No Network Connection Icon"
    } else {
      errorIcon = Icons.Filled.ErrorOutline
      errorDescription = "Error Icon"
    }
    Icon(
      modifier = Modifier.size(48.dp), // Slightly larger icon
      imageVector = errorIcon,
      contentDescription = errorDescription,
      tint = MaterialTheme.colorScheme.error // Ensure icon tint matches error state
    )
    Text(
      text = errorMessage,
      style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
      textAlign = TextAlign.Center,
      color = MaterialTheme.colorScheme.error // Ensure text color matches error state
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NormalContent(
  pagerState: PagerState,
  transcription: Transcription,
  onCopyClicked: (String) -> Unit,
  isSelectionMode: Boolean,
  isSelected: Boolean,
  onSelected: () -> Unit
) {
  val currentDisplayTranscription = transcription // To avoid confusion

  val titleText = when (pagerState.currentPage) {
    0 -> "Transcription"
    1 -> if (!currentDisplayTranscription.summaryText.isNullOrEmpty()) "Summary" else "Translation"
    2 -> "Translation"
    else -> "Transcription"
  }

  // Header: Title, Timestamp, and Actions (Copy/Checkbox)
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = SpacingSmall),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(Modifier.weight(1f)) { // Allow text to take available space
      Text(
        text = titleText,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold, // Make title slightly bolder
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Text(
        text = currentDisplayTranscription.timestamp,
        style = MaterialTheme.typography.labelMedium, // Slightly larger label
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }

    // Action items on the right
    AnimatedVisibility(
      visible = true, // Always visible, content changes
      enter = fadeIn(),
      exit = fadeOut()
    ) {
      if (isSelectionMode) {
        Checkbox(
          checked = isSelected,
          onCheckedChange = { onSelected() },
          colors = CheckboxDefaults.colors(
            checkedColor = MaterialTheme.colorScheme.primary,
            checkmarkColor = MaterialTheme.colorScheme.onPrimary
          ),
          modifier = Modifier.size(40.dp) // Ensure tappable area
        )
      } else {
        IconButton(
          onClick = {
            val currentText = when (pagerState.currentPage) {
              0 -> currentDisplayTranscription.transcriptionText
              1 -> if (!currentDisplayTranscription.summaryText.isNullOrEmpty()) {
                currentDisplayTranscription.summaryText
              } else {
                currentDisplayTranscription.translationText
              }
              2 -> currentDisplayTranscription.translationText
              else -> ""
            }
            currentText?.let { onCopyClicked(it) }
          },
          colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary),
          modifier = Modifier.size(40.dp) // Consistent tappable area
        ) {
          Icon(
            Icons.Filled.ContentCopy,
            contentDescription = "Copy $titleText",
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }
  }

  // Horizontal Pager for Text Content
  HorizontalPager(
    state = pagerState,
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 60.dp, max = 200.dp) // Adjusted height constraints
      .clip(RoundedCornerShape(8.dp)) // Clip pager content area
      .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)) // Slight background distinction
      .padding(SpacingSmall), // Padding inside the pager's background
  ) { page ->
    val scrollState = rememberScrollState()
    val textToShow = when (page) {
      0 -> currentDisplayTranscription.transcriptionText
      1 -> if (!currentDisplayTranscription.summaryText.isNullOrEmpty()) {
        currentDisplayTranscription.summaryText
      } else {
        currentDisplayTranscription.translationText
      }
      2 -> currentDisplayTranscription.translationText
      else -> ""
    }

    if (!textToShow.isNullOrEmpty()) {
      Text(
        modifier = Modifier
          .fillMaxSize() // Fill the pager's page
          .nestedScroll(rememberNestedScrollInteropConnection())
          .verticalScroll(scrollState)
          .verticalScrollbar( // Make sure this is correctly imported
            scrollState = scrollState,
            scrollBarColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
          ),
        text = textToShow,
        style = MaterialTheme.typography.bodyMedium,
        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2 // Increased line height for readability
      )
    } else {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
          "Content not available.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }

  // Pager Indicator
  if (pagerState.pageCount > 1) {
    Row(
      Modifier
        .wrapContentHeight()
        .fillMaxWidth()
        .padding(top = SpacingMedium, bottom = SpacingSmall / 2), // Adjusted padding
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      repeat(pagerState.pageCount) { iteration ->
        val color = if (pagerState.currentPage == iteration) {
          MaterialTheme.colorScheme.primary
        } else {
          MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        }
        val size = if (pagerState.currentPage == iteration) 10.dp else 8.dp // Larger active dot
        Box(
          modifier = Modifier
            .padding(horizontal = 3.dp) // Spacing between dots
            .clip(CircleShape)
            .background(color)
            .size(size)
            .animateContentSize() // Animate size change of dots
        )
      }
    }
  } else {
    Spacer(modifier = Modifier.height(SpacingSmall)) // Maintain some spacing if no pager
  }

  // Visual cue for selected state if not in selection mode (optional but can be nice)
  AnimatedVisibility(visible = isSelected && !isSelectionMode) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = SpacingSmall),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Rounded.CheckCircle,
        contentDescription = "Selected",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(16.dp)
      )
      Spacer(modifier = Modifier.size(SpacingSmall / 2))
      Text(
        "Selected",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary
      )
    }
  }
}


// --- Previews ---
@OptIn(ExperimentalFoundationApi::class)
@Preview(showBackground = true, name = "Normal Card Light")
@Composable
fun TranscriptionCardPreviewLight() {
  MaterialTheme(colorScheme = androidx.compose.material3.lightColorScheme()) { // Example Light Theme
    val previewPagerState = rememberPagerState(pageCount = { 3 })
    TranscriptionCard(
      pagerState = previewPagerState,
      transcription = Transcription(
        0,
        "This is a sample transcription text. It's moderately long to demonstrate scrolling and how the content is displayed within the card.",
        "This is a summary of the transcription.",
        "Ceci est une traduction du texte.",
        formatTimestamp(System.currentTimeMillis()),
        null
      ),
      onCopyClicked = {},
      isSelected = false,
      isSelectionMode = false,
      onSelected = {},
      onClick = {}
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview(showBackground = true, name = "Selected Card Light")
@Composable
fun TranscriptionCardPreviewSelected() {
  MaterialTheme(colorScheme = androidx.compose.material3.lightColorScheme()) {
    val previewPagerState = rememberPagerState(pageCount = { 1 })
    TranscriptionCard(
      pagerState = previewPagerState,
      transcription = Transcription(
        0,
        "This card is selected.",
        null, null,
        formatTimestamp(System.currentTimeMillis()),
        null
      ),
      onCopyClicked = {},
      isSelected = true,
      isSelectionMode = true, // Show checkbox
      onSelected = {},
      onClick = {}
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview(showBackground = true, name = "Error Card Light")
@Composable
fun TranscriptionCardPreviewError() {
  MaterialTheme(colorScheme = androidx.compose.material3.lightColorScheme()) {
    val previewPagerState = rememberPagerState(pageCount = { 1 })
    TranscriptionCard(
      pagerState = previewPagerState,
      transcription = Transcription(0, "", null, null, formatTimestamp(System.currentTimeMillis()), null),
      onCopyClicked = {},
      isSelected = false,
      isSelectionMode = false,
      onSelected = {},
      onClick = {},
      errorMessage = "Network connection failed. Please try again."
    )
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview(showBackground = true, widthDp = 360, name = "Normal Card Dark")
@Composable
fun TranscriptionCardPreviewDark() {
  MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme()) { // Example Dark Theme
    val previewPagerState = rememberPagerState(pageCount = { 2 })
    TranscriptionCard(
      pagerState = previewPagerState,
      transcription = Transcription(
        0,
        "Dark theme transcription example. Content should be clearly visible.",
        "Summary for dark theme.",
        null, // No translation for this preview
        formatTimestamp(System.currentTimeMillis()),
        null
      ),
      onCopyClicked = {},
      isSelected = false,
      isSelectionMode = false,
      onSelected = {},
      onClick = {}
    )
  }
}