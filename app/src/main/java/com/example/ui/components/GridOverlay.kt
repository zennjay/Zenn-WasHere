package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GridOverlay(
    modifier: Modifier = Modifier,
    lineColor: Color = Color.White.copy(alpha = 0.25f)
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val x1 = w / 3f
        val x2 = w * 2f / 3f
        val y1 = h / 3f
        val y2 = h * 2f / 3f

        val stroke = 0.8.dp.toPx()

        // Vertical grid lines
        drawLine(lineColor, Offset(x1, 0f), Offset(x1, h), strokeWidth = stroke)
        drawLine(lineColor, Offset(x2, 0f), Offset(x2, h), strokeWidth = stroke)

        // Horizontal grid lines
        drawLine(lineColor, Offset(0f, y1), Offset(w, y1), strokeWidth = stroke)
        drawLine(lineColor, Offset(0f, y2), Offset(w, y2), strokeWidth = stroke)
    }
}
