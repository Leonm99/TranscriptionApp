package com.example.transcriptionapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role

@Composable
internal fun LabelRadioButton(
    item: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(
            role = Role.RadioButton,
            onClick = onClick,
            onClickLabel = item,
        ),
        headlineContent = { Text(text = item) },
        trailingContent = { RadioButton(selected = isSelected, onClick = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}