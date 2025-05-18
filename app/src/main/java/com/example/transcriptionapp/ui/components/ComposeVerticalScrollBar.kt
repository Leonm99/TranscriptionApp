package com.example.transcriptionapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

// Reference: https://stackoverflow.com/questions/66341823/jetpack-compose-scrollbars/68056586#68056586
// Modify: LazyListState -> ScrollState

fun Modifier.verticalScrollbar(
    scrollState: ScrollState,
    scrollBarWidth: Dp = 4.dp,
    minScrollBarHeight: Dp = 5.dp,
    scrollBarColor: Color? = null,
    cornerRadius: Dp = 2.dp,
): Modifier = composed {
    val resolvedScrollBarColor = scrollBarColor ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

    val targetAlpha = if (scrollState.isScrollInProgress) 0.7f else 0f
    val duration = if (scrollState.isScrollInProgress) 150 else 500

    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = duration)
    )

    drawWithContent {
        drawContent()

        val needDrawScrollbar = scrollState.isScrollInProgress || alpha > 0.0f

        if (needDrawScrollbar && scrollState.maxValue > 0) {
            val visibleHeight: Float = this.size.height - scrollState.maxValue
            val scrollBarHeight: Float = max(visibleHeight * (visibleHeight / this.size.height), minScrollBarHeight.toPx())
            val scrollPercent: Float = scrollState.value.toFloat() / scrollState.maxValue
            val scrollBarOffsetY: Float = scrollState.value + (visibleHeight - scrollBarHeight) * scrollPercent

            drawRoundRect(
                resolvedScrollBarColor,
                topLeft = Offset(this.size.width - scrollBarWidth.toPx(), scrollBarOffsetY),
                size = Size(scrollBarWidth.toPx(), scrollBarHeight),
                alpha = alpha,
                cornerRadius = CornerRadius(cornerRadius.toPx())
            )
        }
    }
}
