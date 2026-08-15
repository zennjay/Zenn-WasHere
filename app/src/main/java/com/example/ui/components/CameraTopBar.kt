package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.HdrAuto
import androidx.compose.material.icons.filled.HdrOff
import androidx.compose.material.icons.filled.HdrOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Adjust
import androidx.compose.material.icons.outlined.MotionPhotosOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.CameraMode
import com.example.camera.FlashMode
import com.example.camera.HardwareCapabilities
import com.example.camera.HdrMode
import com.example.camera.TimerMode
import com.example.settings.CameraSettings
import com.example.ui.theme.CameraGreen
import com.example.ui.theme.CameraYellow
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurfaceDark

@Composable
fun CameraTopBar(
    mode: CameraMode,
    flashMode: FlashMode,
    hdrMode: HdrMode,
    timerMode: TimerMode,
    isMicEnabled: Boolean,
    isTorchOn: Boolean,
    isRecording: Boolean,
    hardwareCapabilities: HardwareCapabilities,
    cameraSettings: CameraSettings,
    onToggleFlash: () -> Unit,
    onToggleHdr: () -> Unit,
    onCycleTimer: () -> Unit,
    onToggleMic: () -> Unit,
    onToggleTorch: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Left actions: Flash & Night/HDR
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flash Button
            if (hardwareCapabilities.hasFlash) {
                val isFlashActive = flashMode != FlashMode.OFF || isTorchOn
                IconButton(
                    onClick = {
                        if (mode.isVideoMode) onToggleTorch() else onToggleFlash()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isFlashActive) Color(0x33FFCC00) else GlassSurfaceDark)
                        .border(
                            1.dp,
                            if (isFlashActive) CameraYellow.copy(alpha = 0.6f) else GlassBorder,
                            CircleShape
                        )
                        .testTag("flash_toggle_button")
                ) {
                    val icon = if (mode.isVideoMode) {
                        if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff
                    } else {
                        when (flashMode) {
                            FlashMode.AUTO -> Icons.Default.FlashAuto
                            FlashMode.ON -> Icons.Default.FlashOn
                            FlashMode.OFF -> Icons.Default.FlashOff
                        }
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Flash mode",
                        tint = if (isFlashActive) CameraYellow else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // HDR Toggle
            if (mode.supportsHdr) {
                val isHdrActive = hdrMode != HdrMode.OFF
                IconButton(
                    onClick = onToggleHdr,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isHdrActive) Color(0x33FFCC00) else GlassSurfaceDark)
                        .border(
                            1.dp,
                            if (isHdrActive) CameraYellow.copy(alpha = 0.6f) else GlassBorder,
                            CircleShape
                        )
                        .testTag("hdr_toggle_button")
                ) {
                    val icon = when (hdrMode) {
                        HdrMode.AUTO -> Icons.Default.HdrAuto
                        HdrMode.ON -> Icons.Default.HdrOn
                        HdrMode.OFF -> Icons.Default.HdrOff
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "HDR Mode",
                        tint = if (isHdrActive) CameraYellow else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Mic Toggle for Video Modes
            if (mode.isVideoMode) {
                IconButton(
                    onClick = onToggleMic,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isMicEnabled) Color(0x3330D158) else GlassSurfaceDark)
                        .border(
                            1.dp,
                            if (isMicEnabled) CameraGreen.copy(alpha = 0.6f) else GlassBorder,
                            CircleShape
                        )
                        .testTag("mic_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isMicEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Microphone",
                        tint = if (isMicEnabled) CameraGreen else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Center Status Badges
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Stabilization Badge
            if (cameraSettings.stabilizationEnabled && (hardwareCapabilities.supportsOis || hardwareCapabilities.supportsEis)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(GlassSurfaceDark)
                        .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (hardwareCapabilities.supportsOis) "OIS" else "EIS",
                        color = CameraYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Video Mode Resolution / FPS Badge
            if (mode.isVideoMode) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(GlassSurfaceDark)
                        .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    val label = if (mode == CameraMode.CINEMATIC) {
                        "${cameraSettings.cinematicResolution} · ${cameraSettings.cinematicFps}"
                    } else if (mode == CameraMode.SLOW_MOTION) {
                        "SLO-MO · ${if (hardwareCapabilities.supports120Fps) "120" else "60"}"
                    } else {
                        "${cameraSettings.videoResolution} · ${cameraSettings.videoFps}"
                    }
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.4.sp
                    )
                }
            }

            // Night Mode indicator
            if (mode == CameraMode.NIGHT) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(CameraYellow)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nightlight,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "NIGHT AUTO",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        // Right actions: Timer & Settings
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timer Button (Photos)
            if (!mode.isVideoMode && mode != CameraMode.PANORAMA) {
                val isTimerActive = timerMode != TimerMode.OFF
                IconButton(
                    onClick = onCycleTimer,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isTimerActive) Color(0x33FFCC00) else GlassSurfaceDark)
                        .border(
                            1.dp,
                            if (isTimerActive) CameraYellow.copy(alpha = 0.6f) else GlassBorder,
                            CircleShape
                        )
                        .testTag("timer_toggle_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Timer",
                            tint = if (isTimerActive) CameraYellow else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        if (isTimerActive) {
                            Text(
                                text = "${timerMode.seconds}",
                                color = CameraYellow,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 1.dp)
                            )
                        }
                    }
                }
            }

            // Settings Button
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GlassSurfaceDark)
                    .border(1.dp, GlassBorder, CircleShape)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
