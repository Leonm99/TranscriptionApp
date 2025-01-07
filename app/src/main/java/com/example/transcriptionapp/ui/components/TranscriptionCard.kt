package com.example.transcriptionapp.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TranscriptionCard(transcriptionText: String){
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp, max = 700.dp)
            .wrapContentHeight(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,

    )
    ) {
        Text(
            modifier = Modifier.padding(16.dp)
            .verticalScroll(rememberScrollState()),
            text = transcriptionText,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}