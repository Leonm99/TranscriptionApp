package com.example.transcriptionapp.ui.components

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.transcriptionapp.model.database.Transcription
import com.example.transcriptionapp.util.copyToClipboard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TranscriptionDetailDialog(
    transcription: Transcription,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val tabs = remember(transcription.summaryText, transcription.translationText) { // Key on relevant text
        mutableListOf("Transcription").apply {
            if (!transcription.summaryText.isNullOrBlank()) add("Summary")
            if (!transcription.translationText.isNullOrBlank()) add("Translation")
        }
    }
    val pagerState = rememberPagerState { tabs.size }
    val showTabs = tabs.size > 1

    // Determine if there's any copyable content at all to show the FAB
    val hasAnyCopyableContent = remember(transcription, tabs, pagerState.currentPage) {
        when (tabs.getOrNull(pagerState.currentPage)) {
            "Transcription" -> transcription.transcriptionText.isNotBlank()
            "Summary" -> !transcription.summaryText.isNullOrBlank()
            "Translation" -> !transcription.translationText.isNullOrBlank()
            else -> false
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .heightIn(min = 200.dp, max = 600.dp)
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            // Box to allow FAB to overlay content and be positioned at the bottom-end
            Box(modifier = Modifier.fillMaxHeight()) {
                Column(
                    modifier = Modifier.fillMaxHeight() // Column takes full height for pager and tabs
                ) {
                    // Dialog Title, Close Button, and Timestamp
                    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Details",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(onClick = onDismissRequest) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close dialog",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Recorded: ${transcription.timestamp}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f) // Pager takes available vertical space
                    ) { pageIndex ->
                        val currentTabTitle = tabs[pageIndex]
                        val textToShow = when (currentTabTitle) {
                            "Transcription" -> transcription.transcriptionText
                            "Summary" -> transcription.summaryText
                            "Translation" -> transcription.translationText
                            else -> ""
                        }
                        PagedContent(
                            text = textToShow ?: "Not available"
                        )
                    }

                    // Tabs or Divider
                    if (showTabs) {
                        Surface( // Surface for elevation shadow for tabs
                            modifier = Modifier.fillMaxWidth(),
                            shadowElevation = 3.dp, // Add shadow to separate tabs from content
                            color = MaterialTheme.colorScheme.surfaceContainer // Or surface
                        ) {
                            TabRow(
                                selectedTabIndex = pagerState.currentPage,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                containerColor = Color.Transparent, // Handled by Surface
                                indicator = {}, // No default indicator
                                divider = {} // No default divider
                            ) {
                                tabs.forEachIndexed { index, title ->
                                    val selected = pagerState.currentPage == index
                                    Tab(
                                        selected = selected,
                                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp) // Spacing between tabs
                                            .height(40.dp) // Fixed height for tabs
                                            .clip(RoundedCornerShape(20.dp)), // Pill shape
                                        selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    if (selected) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                )
                                                .clip(RoundedCornerShape(20.dp)), // Ensure background respects shape
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = title,
                                                style = MaterialTheme.typography.labelLarge,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Add a spacer at the bottom if no tabs, so content doesn't sit flush with card bottom
                        // FAB padding will handle most of this, but a small spacer can be good.
                        Spacer(modifier = Modifier.height(8.dp))
                        // If no tabs, we might still want a divider before the card ends if there's no FAB
                        if (!hasAnyCopyableContent) { // Only show divider if no FAB
                            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                } // End of Main Column

                // Stationary FAB, shown if there's content to copy in the current tab
                if (hasAnyCopyableContent) {
                    FloatingActionButton(
                        onClick = {
                            val textToCopy = when (tabs.getOrNull(pagerState.currentPage)) {
                                "Transcription" -> transcription.transcriptionText
                                "Summary" -> transcription.summaryText
                                "Translation" -> transcription.translationText
                                else -> null
                            }
                            textToCopy?.let {
                                if (it.isNotBlank()) {
                                    scope.launch {
                                        copyToClipboard(context, it)
                                        val contentType = tabs.getOrNull(pagerState.currentPage) ?: "Text"
                                        Toast.makeText(context, "$contentType copied", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(vertical = if(showTabs) 70.dp else 16.dp, horizontal = 16.dp), // Padding for the FAB from the edges of the Card
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copy ${tabs.getOrNull(pagerState.currentPage) ?: "text"}"
                        )
                    }
                }
            } // End of Box for FAB positioning
        } // End of Card
    } // End of Dialog
}

@Composable
private fun PagedContent(
    text: String,
    // Removed onCopyClick parameter
) {
    val scrollState = rememberScrollState()
    Column( // Column now doesn't need to be a Box
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .verticalScrollbar(
                scrollState = scrollState,
                scrollBarColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            // Reduced bottom padding as FAB is no longer inside PagedContent
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.25,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// --- Previews ---
@Preview(showBackground = true, widthDp = 380, heightDp = 550)
@Composable
private fun PreviewTranscriptionDetailDialogWithBottomTabsAndFAB() {
    MaterialTheme {
        TranscriptionDetailDialog(
            transcription = Transcription(
                id = 1,
                timestamp = "2023-10-27 10:00 AM",
                transcriptionText = "This is a sample transcription text. It's designed to be long enough to demonstrate scrolling within a tab. We can add more lines here to ensure that the vertical scroll is definitely needed for this section. \n\nMore lines... \nAnd more... \nTo ensure the FAB is clearly visible over scrolling content.",
                summaryText = "Summary of the transcription. This could also be long enough to scroll and test the FAB.",
                translationText = "Translation Text Sample, possibly also quite long to demonstrate the FAB behavior."
            ),
            onDismissRequest = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 400)
@Composable
private fun PreviewTranscriptionDetailDialogOnlyTranscriptionWithFAB() {
    MaterialTheme {
        TranscriptionDetailDialog(
            transcription = Transcription(
                id = 1,
                timestamp = "2023-10-27 10:00 AM",
                transcriptionText = "This is a sample transcription text when only transcription is available.\nIt should still have a copy FAB if this text is not blank.",
                summaryText = null, // Or ""
                translationText = ""  // Or null
            ),
            onDismissRequest = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 300)
@Composable
private fun PreviewTranscriptionDetailDialogNoCopyableContent() {
    MaterialTheme {
        TranscriptionDetailDialog(
            transcription = Transcription(
                id = 1,
                timestamp = "2023-10-27 10:00 AM",
                transcriptionText = "", // Empty
                summaryText = null,
                translationText = null
            ),
            onDismissRequest = {}
        )
    }
}