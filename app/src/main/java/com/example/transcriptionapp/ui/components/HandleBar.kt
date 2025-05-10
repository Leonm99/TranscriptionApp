package com.example.transcriptionapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HandleBar(modifier: Modifier) {
  Box(modifier.fillMaxWidth().height(4.dp).padding(vertical = 8.dp))
}
