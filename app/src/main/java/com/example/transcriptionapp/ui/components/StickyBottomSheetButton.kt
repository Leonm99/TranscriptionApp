package com.example.transcriptionapp.ui.components


import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickyBottomSheetButton(
    onClick: () -> Unit,
    text: String
) {
    Button(
        modifier = Modifier
            .wrapContentWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(25.dp)
            ),
        onClick = onClick
    ) {
        Text(text = text)
    }
}