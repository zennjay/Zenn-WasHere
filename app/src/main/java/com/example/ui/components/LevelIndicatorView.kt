package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.example.camera.DeviceTiltState
import com.example.ui.theme.CameraYellow
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LevelIndicatorView(
    tiltState: DeviceTiltState,
    modifier: Modifier = Modifier
) {
    val isLevel = tiltState.isLevel
    val lineColor by animateColorAsState(
        targetValue = if (isLevel) CameraYellow else Color.White.copy(alpha = 0.6f),
        animationSpec = tween(150),
        label = "level_line_color"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val lineHalfLength = 48.dp.toPx()
        val gap = 16.dp.toPx()

        // Angle in radians (clamped to prevent excessive tilt spinning)
        val angleRad = Math.toRadians(-tiltState.roll.toDouble().coerceIn(-45.0, 45.0))
        val cosA = cos(angleRad).toFloat()
        val sinA = sin(angleRad).toFloat()

        // Left segmented line
        val p1x = centerX - cosA * lineHalfLength
        val p1y = centerY - sinA * lineHalfLength
        val p2x = centerX - cosA * gap
        val p2y = centerY - sinA * gap

        // Right segmented line
        val p3x = centerX + cosA * gap
        val p3y = centerY + sinA * gap
        val p4x = centerX + cosA * lineHalfLength
        val p4y = centerY + sinA * lineHalfLength

        val strokeWidth = if (isLevel) 2.5.dp.toPx() else 1.5.dp.toPx()

        drawLine(
            color = lineColor,
            start = Offset(p1x, p1y),
            end = Offset(p2x, p2y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        drawLine(
            color = lineColor,
            start = Offset(p3x, p3y),
            end = Offset(p4x, p4y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Center crosshair reference dots
        drawCircle(
            color = lineColor.copy(alpha = 0.8f),
            radius = 2.dp.toPx(),
            center = Offset(centerX, centerY)
        )
    }
}
