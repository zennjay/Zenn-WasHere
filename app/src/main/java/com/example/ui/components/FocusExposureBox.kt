package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.FocusPointState
import com.example.ui.theme.CameraYellow
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun FocusExposureBox(
    focusState: FocusPointState,
    evCompensation: Int,
    previewWidthPx: Float,
    previewHeightPx: Float,
    onEvChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    var currentEvOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(focusState.timestamp) {
        if (focusState.isVisible) {
            isVisible = true
            currentEvOffset = evCompensation.toFloat()
            if (!focusState.isLocked) {
                delay(3500L)
                isVisible = false
            }
        }
    }

    if (!isVisible || previewWidthPx <= 0f || previewHeightPx <= 0f) return

    val boxSizeDp = 72.dp
    val focusXPx = focusState.x * previewWidthPx
    val focusYPx = focusState.y * previewHeightPx

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    x = (focusXPx - 100).toInt().coerceIn(0, (previewWidthPx - 200).toInt().coerceAtLeast(0)),
                    y = (focusYPx - 100).toInt().coerceIn(0, (previewHeightPx - 200).toInt().coerceAtLeast(0))
                )
            }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // AE/AF Lock Badge
            if (focusState.isLocked) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CameraYellow)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "AE/AF LOCK",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Focus Square
                Box(
                    modifier = Modifier
                        .size(boxSizeDp)
                        .border(1.5.dp, CameraYellow, RoundedCornerShape(2.dp))
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Sun Exposure Slider
                Column(
                    modifier = Modifier
                        .height(90.dp)
                        .width(28.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { change, dragAmount ->
                                change.consume()
                                currentEvOffset -= (dragAmount * 0.08f)
                                val newEv = currentEvOffset.roundToInt().coerceIn(-4, 4)
                                onEvChanged(newEv)
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Vertical line guide
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .weight(1f)
                            .background(CameraYellow.copy(alpha = 0.5f))
                    )

                    // Sun Icon
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = "Exposure slider",
                        tint = CameraYellow,
                        modifier = Modifier.size(18.dp)
                    )

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .weight(1f)
                            .background(CameraYellow.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}
