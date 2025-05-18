package com.example.transcriptionapp.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.SpeakerNotes
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.transcriptionapp.com.example.transcriptionapp.model.database.Transcription

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TranscriptionListItem(
    transcription: Transcription,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium) // Ensure clip is applied before clickable for ripple shape
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onItemLongClick() // Toggle selection
                    } else {
                        onItemClick()     // Perform primary action
                    }
                },
                onLongClick = {
                    onItemLongClick() // Always toggle selection on long click
                },
                role = Role.Button
            )
            .padding(horizontal = 8.dp, vertical = 4.dp), // Padding for spacing between cards
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp, // Optional: slightly more elevation when pressed
            focusedElevation = 3.dp, // Optional
            hoveredElevation = 3.dp  // Optional
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp) // Adjusted padding for content
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Optional: Leading icon for selection state (alternative to checkbox in some contexts)
            // if (isSelected && !isSelectionMode) {
            //     Icon(
            //         imageVector = Icons.Filled.CheckCircle,
            //         contentDescription = "Selected",
            //         tint = MaterialTheme.colorScheme.primary,
            //         modifier = Modifier.padding(end = 8.dp).size(20.dp)
            //     )
            // }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transcription.timestamp,
                        style = MaterialTheme.typography.labelMedium, // Slightly larger than labelSmall
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), // De-emphasize
                        modifier = Modifier.weight(1f) // Allow timestamp to take space but wrap
                    )
                    // Icons for summary/translation
                    Row(horizontalArrangement = Arrangement.End) {
                        if (!transcription.summaryText.isNullOrBlank()) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.SpeakerNotes,
                                contentDescription = "Summary available",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp).padding(start = 4.dp)
                            )
                        }
                        if (!transcription.translationText.isNullOrBlank()) {
                            Icon(
                                imageVector = Icons.Outlined.Translate,
                                contentDescription = "Translation available",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp).padding(start = 4.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = transcription.transcriptionText,
                    style = MaterialTheme.typography.bodyLarge, // Make transcription text more prominent
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Normal // Or FontWeight.Medium if you want more emphasis
                )
            }

            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onItemLongClick() }, // Click on checkbox also toggles selection
                    modifier = Modifier.padding(start = 12.dp) // Add some padding to the checkbox
                )
            }
        }
    }
}