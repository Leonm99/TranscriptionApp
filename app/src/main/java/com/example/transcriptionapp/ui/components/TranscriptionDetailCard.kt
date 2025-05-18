package com.example.transcriptionapp.ui.components

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.transcriptionapp.com.example.transcriptionapp.model.database.Transcription
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

    val tabs = remember {
        mutableListOf("Transcription").apply {
            if (!transcription.summaryText.isNullOrBlank()) add("Summary")
            if (!transcription.translationText.isNullOrBlank()) add("Translation")
        }
    }
    val pagerState = rememberPagerState { tabs.size }
    val showTabs = tabs.size > 1

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false

        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f) // Example: 90% of screen width
                .heightIn(min = 200.dp, max = 600.dp)
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxHeight()
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
                        .weight(1f)
                ) { pageIndex ->
                    val currentTabTitle = tabs[pageIndex]
                    val transcriptionCopyAction: (() -> Unit)? = transcription.transcriptionText.takeIf { it.isNotBlank() }?.let { text ->
                        { scope.launch { copyToClipboard(context,text); Toast.makeText(context, "Transcription copied", Toast.LENGTH_SHORT).show() } }
                    }
                    val summaryCopyAction: (() -> Unit)? = transcription.summaryText?.takeIf { it.isNotBlank() }?.let { text ->
                        { scope.launch { copyToClipboard(context,text); Toast.makeText(context, "Summary copied", Toast.LENGTH_SHORT).show() } }
                    }
                    val translationCopyAction: (() -> Unit)? = transcription.translationText?.takeIf { it.isNotBlank() }?.let { text ->
                        { scope.launch { copyToClipboard(context,text); Toast.makeText(context, "Translation copied", Toast.LENGTH_SHORT).show() } }
                    }

                    val (textToShow, finalCopyAction) = when (currentTabTitle) {
                        "Transcription" -> transcription.transcriptionText to transcriptionCopyAction
                        "Summary" -> transcription.summaryText to summaryCopyAction
                        "Translation" -> transcription.translationText to translationCopyAction
                        else -> "" to null
                    }

                    PagedContent(
                        // title = currentTabTitle, // Title not strictly needed for copy button contentDescription anymore
                        text = textToShow ?: "Not available",
                        onCopyClick = finalCopyAction
                    )
                }

                if (showTabs) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 3.dp,
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        TabRow(
                            selectedTabIndex = pagerState.currentPage,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            containerColor = Color.Transparent,
                            indicator = {},
                            divider = {}
                        ) {
                            tabs.forEachIndexed { index, title ->
                                val selected = pagerState.currentPage == index
                                Tab(
                                    selected = selected,
                                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(20.dp)),
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
                                            .clip(RoundedCornerShape(20.dp)),
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
                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun PagedContent(
    // title: String, // No longer needed for copy button's contentDescription here
    text: String,
    onCopyClick: (() -> Unit)?
) {
    val scrollState = rememberScrollState()
    Box(modifier = Modifier.fillMaxSize()) { // Box to allow positioning of the FAB
        Column(
            modifier = Modifier
                .fillMaxSize() // Column takes full size for scrolling
                .verticalScroll(scrollState)
                .verticalScrollbar(scrollState)
                // Add padding at the bottom to prevent FAB from overlapping last lines of text
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 72.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.25,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        onCopyClick?.let {
            FloatingActionButton(
                onClick = it,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp), // Padding for the FAB itself from the edges of the Box
                shape = CircleShape, // Or MaterialTheme.shapes.medium for a squircle
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "Copy text" // Generic description, or you could pass title back if needed
                )
            }
        }
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
                summaryText = null,
                translationText = ""
            ),
            onDismissRequest = {}
        )
    }
}