package com.example.ui

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.camera.CameraController
import com.example.camera.CameraMode
import com.example.camera.OrientationSensorHelper
import com.example.settings.CameraPreferencesRepository
import com.example.settings.CameraSettings
import com.example.ui.components.CameraBottomBar
import com.example.ui.components.CameraTopBar
import com.example.ui.components.CinematicControlsOverlay
import com.example.ui.components.FocusExposureBox
import com.example.ui.components.GridOverlay
import com.example.ui.components.LevelIndicatorView
import com.example.ui.components.ModeSelectorCarousel
import com.example.ui.components.PanoramaGuideView
import com.example.ui.components.PortraitControlsBar
import com.example.ui.components.ProControlsBar
import com.example.ui.components.ScreenFlashOverlay
import com.example.ui.theme.CameraYellow

@Composable
fun CameraMainScreen(
    cameraController: CameraController,
    sensorHelper: OrientationSensorHelper,
    cameraSettings: CameraSettings,
    onOpenGallery: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }

    val currentMode by cameraController.currentMode.collectAsState()
    val flashMode by cameraController.flashMode.collectAsState()
    val hdrMode by cameraController.hdrMode.collectAsState()
    val timerMode by cameraController.timerMode.collectAsState()
    val countdownSec by cameraController.countdownSeconds.collectAsState()
    val zoomRatio by cameraController.zoomRatio.collectAsState()
    val hardwareCaps by cameraController.hardwareCapabilities.collectAsState()
    val focusPoint by cameraController.focusPoint.collectAsState()
    val evComp by cameraController.evCompensation.collectAsState()
    val isRecording by cameraController.isRecording.collectAsState()
    val isRecordingPaused by cameraController.isRecordingPaused.collectAsState()
    val recordingDuration by cameraController.recordingDurationSeconds.collectAsState()
    val isProcessing by cameraController.isProcessing.collectAsState()
    val lastMediaUri by cameraController.lastCapturedMedia.collectAsState()
    val showScreenFlash by cameraController.showScreenFlash.collectAsState()
    val portraitAperture by cameraController.portraitAperture.collectAsState()
    val cinematicProfile by cameraController.cinematicColorProfile.collectAsState()
    val proSettings by cameraController.proSettings.collectAsState()
    val isMicEnabled by cameraController.isMicEnabled.collectAsState()
    val isTorchOn by cameraController.isTorchOn.collectAsState()
    val isCapturingPanorama by cameraController.isCapturingPanorama.collectAsState()
    val panoramaProgress by cameraController.panoramaProgress.collectAsState()

    val tiltState by sensorHelper.tiltState.collectAsState()

    // Keep settings in sync
    LaunchedEffect(cameraSettings) {
        cameraController.updateSettings(cameraSettings)
    }

    DisposableEffect(Unit) {
        sensorHelper.startListening()
        onDispose {
            sensorHelper.stopListening()
        }
    }

    // Trigger subtle haptic feedback when leveling is achieved
    var lastLevelState by remember { mutableStateOf(false) }
    LaunchedEffect(tiltState.isLevel) {
        if (tiltState.isLevel && !lastLevelState && cameraSettings.levelEnabled) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(20)
                }
            } catch (_: Exception) {}
        }
        lastLevelState = tiltState.isLevel
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        // 1. Camera Viewfinder Preview
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    cameraController.setSurfaceProvider(surfaceProvider)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom != 1.0f) {
                            cameraController.onPinchZoom(zoom)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            cameraController.switchCamera()
                        },
                        onLongPress = { offset ->
                            val normX = offset.x / widthPx
                            val normY = offset.y / heightPx
                            cameraController.onLockFocusAndExposure(normX, normY, widthPx, heightPx)
                        },
                        onTap = { offset ->
                            val normX = offset.x / widthPx
                            val normY = offset.y / heightPx
                            cameraController.onTapToFocus(normX, normY, widthPx, heightPx)
                        }
                    )
                }
        )

        // 2. 3x3 Grid Overlay
        if (cameraSettings.gridEnabled) {
            GridOverlay()
        }

        // 3. Horizon Level Indicator
        if (cameraSettings.levelEnabled && !isRecording && currentMode != CameraMode.PANORAMA) {
            LevelIndicatorView(tiltState = tiltState)
        }

        // 4. Interactive Focus & Exposure Reticle Box
        FocusExposureBox(
            focusState = focusPoint,
            evCompensation = evComp,
            previewWidthPx = widthPx,
            previewHeightPx = heightPx,
            onEvChanged = { cameraController.setEvCompensation(it) }
        )

        // 5. Screen Flash for selfie in low-light
        ScreenFlashOverlay(visible = showScreenFlash)

        // Top Gradient Scrim for Sleek Aesthetic
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xD9000000),
                            Color(0x66000000),
                            Color.Transparent
                        )
                    )
                )
        )

        // Bottom Gradient Scrim for Sleek Aesthetic
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x80000000),
                            Color(0xF2000000)
                        )
                    )
                )
        )

        // 6. Countdown Timer Display
        if (countdownSec > 0) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$countdownSec",
                    color = CameraYellow,
                    fontSize = 88.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // 7. Top Bar Controls with Status Bar Padding
        CameraTopBar(
            mode = currentMode,
            flashMode = flashMode,
            hdrMode = hdrMode,
            timerMode = timerMode,
            isMicEnabled = isMicEnabled,
            isTorchOn = isTorchOn,
            isRecording = isRecording,
            hardwareCapabilities = hardwareCaps,
            cameraSettings = cameraSettings,
            onToggleFlash = { cameraController.toggleFlash() },
            onToggleHdr = { cameraController.toggleHdr() },
            onCycleTimer = { cameraController.cycleTimer() },
            onToggleMic = { cameraController.setMicEnabled(!isMicEnabled) },
            onToggleTorch = { cameraController.toggleTorch() },
            onOpenSettings = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        )

        // 8. Bottom Overlays & Controls with Navigation Bar Padding
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mode-specific specialized sub-bars
            when (currentMode) {
                CameraMode.PRO -> {
                    ProControlsBar(
                        proSettings = proSettings,
                        hardwareCapabilities = hardwareCaps,
                        onUpdateProSettings = { cameraController.updateProSettings(it) }
                    )
                }
                CameraMode.CINEMATIC -> {
                    CinematicControlsOverlay(
                        activeProfile = cinematicProfile,
                        onProfileSelected = { cameraController.setCinematicColorProfile(it) }
                    )
                }
                CameraMode.PORTRAIT -> {
                    PortraitControlsBar(
                        currentAperture = portraitAperture,
                        onApertureSelected = { cameraController.setPortraitAperture(it) }
                    )
                }
                CameraMode.PANORAMA -> {
                    PanoramaGuideView(
                        isCapturing = isCapturingPanorama,
                        progress = panoramaProgress,
                        onAddFrame = { cameraController.addPanoramaFrame() }
                    )
                }
                else -> {}
            }

            // Mode Selector Carousel
            if (!isRecording) {
                ModeSelectorCarousel(
                    currentMode = currentMode,
                    onModeSelected = { cameraController.setMode(it) }
                )
            }

            // Main Bottom Bar (Zoom, Gallery, Shutter, Switch)
            CameraBottomBar(
                mode = currentMode,
                isRecording = isRecording,
                isRecordingPaused = isRecordingPaused,
                recordingDurationSeconds = recordingDuration,
                isProcessing = isProcessing,
                zoomRatio = zoomRatio,
                hardwareCapabilities = hardwareCaps,
                lastMediaUri = lastMediaUri,
                onShutterClick = {
                    cameraController.performShutterAction { /* Media captured */ }
                },
                onPauseResumeRecording = {
                    if (isRecordingPaused) cameraController.resumeRecording() else cameraController.pauseRecording()
                },
                onSwitchCamera = { cameraController.switchCamera() },
                onZoomSelected = { cameraController.setZoom(it) },
                onOpenGallery = onOpenGallery
            )
        }
    }
}
