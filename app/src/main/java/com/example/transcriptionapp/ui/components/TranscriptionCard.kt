package com.example.transcriptionapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TranscriptionCard(transcription: String, timestamp: Long,  onCopyClicked: (String) -> Unit) {
    val formattedTimestamp = formatTimestamp(timestamp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(5.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = { onCopyClicked(transcription) },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = "Copy",
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transcription",
                        style = MaterialTheme.typography.titleMedium,

                    )


                }
                Text(
                    modifier = Modifier.padding(bottom = 8.dp),
                    text = formattedTimestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Text(
                    text = transcription,
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.bodyMedium
                )


            }
        }
    }
}


fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("dd.MM.yyyy • HH:mm", Locale.getDefault())
    return format.format(date)
}

@Preview(showBackground = true)
@Composable
fun TranscriptionCardPreview() {
    TranscriptionCard(
        transcription = "This is a sample transcription that will be copied. This is a sample transcription that will be copied. This is a sample transcription that will be copied.",
        timestamp = System.currentTimeMillis()
        ,onCopyClicked = {  })
}