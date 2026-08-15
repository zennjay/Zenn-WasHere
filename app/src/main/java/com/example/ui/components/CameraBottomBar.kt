package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.camera.CameraMode
import com.example.camera.HardwareCapabilities
import com.example.ui.theme.CameraRed
import com.example.ui.theme.CameraYellow
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderHighlight
import com.example.ui.theme.GlassSurfaceDark
import com.example.ui.theme.GlassSurfaceElevated
import kotlin.math.abs

@Composable
fun CameraBottomBar(
    mode: CameraMode,
    isRecording: Boolean,
    isRecordingPaused: Boolean,
    recordingDurationSeconds: Long,
    isProcessing: Boolean,
    zoomRatio: Float,
    hardwareCapabilities: HardwareCapabilities,
    lastMediaUri: Uri?,
    onShutterClick: () -> Unit,
    onPauseResumeRecording: () -> Unit,
    onSwitchCamera: () -> Unit,
    onZoomSelected: (Float) -> Unit,
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Zoom Pills Selector (Floating Glass Pill)
        if (mode != CameraMode.PANORAMA && hardwareCapabilities.supportedZoomLevels.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(GlassSurfaceDark)
                    .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                hardwareCapabilities.supportedZoomLevels.forEach { zoomLevel ->
                    val isSelected = abs(zoomRatio - zoomLevel) < 0.15f
                    val label = when {
                        zoomLevel == 0.5f -> ".5"
                        zoomLevel % 1f == 0f -> "${zoomLevel.toInt()}x"
                        else -> "${zoomLevel}x"
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) CameraYellow else Color.Transparent)
                            .clickable { onZoomSelected(zoomLevel) }
                            .testTag("zoom_button_$label"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        // Recording duration indicator when recording
        if (isRecording) {
            val minutes = recordingDurationSeconds / 60
            val seconds = recordingDurationSeconds % 60
            val timeText = String.format("%02d:%02d", minutes, seconds)

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(GlassSurfaceDark)
                    .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isRecordingPaused) CameraYellow else CameraRed)
                )
                Text(
                    text = if (isRecordingPaused) "PAUSED $timeText" else timeText,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Main Controls Row: Gallery - Shutter - Switch Camera
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Gallery Thumbnail (Sleek Frosted Glass Container)
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(GlassSurfaceElevated)
                    .border(1.5.dp, GlassBorderHighlight, CircleShape)
                    .clickable { onOpenGallery() }
                    .testTag("gallery_thumbnail_button"),
                contentAlignment = Alignment.Center
            ) {
                if (lastMediaUri != null) {
                    AsyncImage(
                        model = lastMediaUri,
                        contentDescription = "Gallery Thumbnail",
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.PhotoLibrary,
                        contentDescription = "Open Gallery",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Center: Luxury Camera Shutter Button
            ShutterButton(
                mode = mode,
                isRecording = isRecording,
                isProcessing = isProcessing,
                onClick = onShutterClick
            )

            // Right: Switch Camera or Pause/Resume if recording
            if (isRecording) {
                IconButton(
                    onClick = onPauseResumeRecording,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(GlassSurfaceElevated)
                        .border(1.dp, GlassBorder, CircleShape)
                        .testTag("pause_resume_button")
                ) {
                    Icon(
                        imageVector = if (isRecordingPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isRecordingPaused) "Resume Recording" else "Pause Recording",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onSwitchCamera,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(GlassSurfaceElevated)
                        .border(1.dp, GlassBorder, CircleShape)
                        .testTag("switch_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Switch Camera",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Home gesture bar indicator
        Box(
            modifier = Modifier
                .width(128.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.25f))
        )
    }
}

@Composable
fun ShutterButton(
    mode: CameraMode,
    isRecording: Boolean,
    isProcessing: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = tween(100, easing = FastOutSlowInEasing),
        label = "shutter_scale"
    )

    val innerSize by animateDpAsState(
        targetValue = if (isRecording) 32.dp else 64.dp,
        animationSpec = tween(200),
        label = "inner_shutter_size"
    )

    val innerCornerRadius by animateDpAsState(
        targetValue = if (isRecording) 8.dp else 32.dp,
        animationSpec = tween(200),
        label = "shutter_corner_radius"
    )

    Box(
        modifier = Modifier
            .size(80.dp)
            .scale(scale)
            .border(3.5.dp, Color.White, CircleShape)
            .padding(4.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("shutter_button"),
        contentAlignment = Alignment.Center
    ) {
        if (isProcessing) {
            CircularProgressIndicator(
                modifier = Modifier.size(42.dp),
                color = CameraYellow,
                strokeWidth = 3.dp
            )
        } else {
            val innerColor = when {
                mode.isVideoMode -> CameraRed
                mode == CameraMode.PORTRAIT -> CameraYellow
                mode == CameraMode.NIGHT -> CameraYellow
                else -> Color.White
            }

            Box(
                modifier = Modifier
                    .size(innerSize)
                    .clip(RoundedCornerShape(innerCornerRadius))
                    .background(innerColor)
            )
        }
    }
}
