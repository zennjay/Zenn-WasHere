package com.example.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaActionSound
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.media.MediaRepository
import com.example.processing.CinematicProcessor
import com.example.processing.NightProcessor
import com.example.processing.PanoramaProcessor
import com.example.processing.PortraitProcessor
import com.example.settings.CameraSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class CameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val scope: CoroutineScope,
    private val mediaRepository: MediaRepository
) {
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private val mediaActionSound = MediaActionSound().apply {
        try { load(MediaActionSound.SHUTTER_CLICK); load(MediaActionSound.START_VIDEO_RECORDING); load(MediaActionSound.STOP_VIDEO_RECORDING) } catch (_: Exception) {}
    }

    // State flows
    private val _currentMode = MutableStateFlow(CameraMode.PHOTO)
    val currentMode: StateFlow<CameraMode> = _currentMode.asStateFlow()

    private val _lensFacing = MutableStateFlow(CameraSelector.LENS_FACING_BACK)
    val lensFacing: StateFlow<Int> = _lensFacing.asStateFlow()

    private val _flashMode = MutableStateFlow(FlashMode.AUTO)
    val flashMode: StateFlow<FlashMode> = _flashMode.asStateFlow()

    private val _hdrMode = MutableStateFlow(HdrMode.AUTO)
    val hdrMode: StateFlow<HdrMode> = _hdrMode.asStateFlow()

    private val _timerMode = MutableStateFlow(TimerMode.OFF)
    val timerMode: StateFlow<TimerMode> = _timerMode.asStateFlow()

    private val _countdownSeconds = MutableStateFlow(0)
    val countdownSeconds: StateFlow<Int> = _countdownSeconds.asStateFlow()

    private val _zoomRatio = MutableStateFlow(1.0f)
    val zoomRatio: StateFlow<Float> = _zoomRatio.asStateFlow()

    private val _hardwareCapabilities = MutableStateFlow(HardwareCapabilities())
    val hardwareCapabilities: StateFlow<HardwareCapabilities> = _hardwareCapabilities.asStateFlow()

    private val _focusPoint = MutableStateFlow(FocusPointState())
    val focusPoint: StateFlow<FocusPointState> = _focusPoint.asStateFlow()

    private val _evCompensation = MutableStateFlow(0)
    val evCompensation: StateFlow<Int> = _evCompensation.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isRecordingPaused = MutableStateFlow(false)
    val isRecordingPaused: StateFlow<Boolean> = _isRecordingPaused.asStateFlow()

    private val _recordingDurationSeconds = MutableStateFlow(0L)
    val recordingDurationSeconds: StateFlow<Long> = _recordingDurationSeconds.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _lastCapturedMedia = MutableStateFlow<Uri?>(null)
    val lastCapturedMedia: StateFlow<Uri?> = _lastCapturedMedia.asStateFlow()

    private val _showScreenFlash = MutableStateFlow(false)
    val showScreenFlash: StateFlow<Boolean> = _showScreenFlash.asStateFlow()

    private val _portraitAperture = MutableStateFlow(2.8f) // F1.4 - F16
    val portraitAperture: StateFlow<Float> = _portraitAperture.asStateFlow()

    private val _cinematicColorProfile = MutableStateFlow(CinematicColorProfile.NATURAL)
    val cinematicColorProfile: StateFlow<CinematicColorProfile> = _cinematicColorProfile.asStateFlow()

    private val _proSettings = MutableStateFlow(ProSettings())
    val proSettings: StateFlow<ProSettings> = _proSettings.asStateFlow()

    private val _isMicEnabled = MutableStateFlow(true)
    val isMicEnabled: StateFlow<Boolean> = _isMicEnabled.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    // Panorama capture state
    private val _panoramaFrames = mutableListOf<Bitmap>()
    private val _isCapturingPanorama = MutableStateFlow(false)
    val isCapturingPanorama: StateFlow<Boolean> = _isCapturingPanorama.asStateFlow()

    private val _panoramaProgress = MutableStateFlow(0f)
    val panoramaProgress: StateFlow<Float> = _panoramaProgress.asStateFlow()

    private var previewSurfaceProvider: Preview.SurfaceProvider? = null
    private var currentSettings = CameraSettings()
    private var recordingTimerJob: Job? = null

    init {
        scope.launch {
            val lastMedia = mediaRepository.getLastCapturedMedia()
            _lastCapturedMedia.value = lastMedia?.uri
        }
    }

    fun updateSettings(settings: CameraSettings) {
        currentSettings = settings
    }

    fun setSurfaceProvider(provider: Preview.SurfaceProvider?) {
        previewSurfaceProvider = provider
        bindCameraUseCases()
    }

    fun setMode(mode: CameraMode) {
        if (_currentMode.value == mode) return
        _currentMode.value = mode
        bindCameraUseCases()
    }

    fun switchCamera() {
        val nextFacing = if (_lensFacing.value == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        _lensFacing.value = nextFacing
        _zoomRatio.value = 1.0f
        refreshHardwareCapabilities()
        bindCameraUseCases()
    }

    fun refreshHardwareCapabilities() {
        scope.launch(Dispatchers.IO) {
            val caps = HardwareDetector.detectCapabilities(context, _lensFacing.value)
            _hardwareCapabilities.value = caps
        }
    }

    fun toggleFlash() {
        _flashMode.value = when (_flashMode.value) {
            FlashMode.AUTO -> FlashMode.ON
            FlashMode.ON -> FlashMode.OFF
            FlashMode.OFF -> FlashMode.AUTO
        }
        applyFlashToImageCapture()
    }

    fun toggleTorch() {
        val next = !_isTorchOn.value
        _isTorchOn.value = next
        camera?.cameraControl?.enableTorch(next)
    }

    fun toggleHdr() {
        _hdrMode.value = when (_hdrMode.value) {
            HdrMode.AUTO -> HdrMode.ON
            HdrMode.ON -> HdrMode.OFF
            HdrMode.OFF -> HdrMode.AUTO
        }
    }

    fun cycleTimer() {
        _timerMode.value = when (_timerMode.value) {
            TimerMode.OFF -> TimerMode.SEC_3
            TimerMode.SEC_3 -> TimerMode.SEC_10
            TimerMode.SEC_10 -> TimerMode.OFF
        }
    }

    fun setPortraitAperture(fStop: Float) {
        _portraitAperture.value = fStop.coerceIn(1.4f, 16.0f)
    }

    fun setCinematicColorProfile(profile: CinematicColorProfile) {
        _cinematicColorProfile.value = profile
    }

    fun setMicEnabled(enabled: Boolean) {
        _isMicEnabled.value = enabled
    }

    fun setZoom(ratio: Float) {
        val caps = _hardwareCapabilities.value
        val clamped = ratio.coerceIn(caps.minZoomRatio, caps.maxZoomRatio)
        _zoomRatio.value = clamped
        camera?.cameraControl?.setZoomRatio(clamped)
    }

    fun onPinchZoom(scaleFactor: Float) {
        val current = _zoomRatio.value
        val newZoom = current * scaleFactor
        setZoom(newZoom)
    }

    fun onTapToFocus(xNorm: Float, yNorm: Float, previewWidth: Float, previewHeight: Float) {
        val factory = SurfaceOrientedMeteringPointFactory(previewWidth, previewHeight)
        val point = factory.createPoint(xNorm * previewWidth, yNorm * previewHeight)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(4, TimeUnit.SECONDS)
            .build()

        _focusPoint.value = FocusPointState(
            x = xNorm,
            y = yNorm,
            isVisible = true,
            isLocked = false,
            timestamp = System.currentTimeMillis()
        )

        camera?.cameraControl?.startFocusAndMetering(action)
    }

    fun onLockFocusAndExposure(xNorm: Float, yNorm: Float, previewWidth: Float, previewHeight: Float) {
        val factory = SurfaceOrientedMeteringPointFactory(previewWidth, previewHeight)
        val point = factory.createPoint(xNorm * previewWidth, yNorm * previewHeight)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .disableAutoCancel()
            .build()

        _focusPoint.value = FocusPointState(
            x = xNorm,
            y = yNorm,
            isVisible = true,
            isLocked = true,
            timestamp = System.currentTimeMillis()
        )

        camera?.cameraControl?.startFocusAndMetering(action)
    }

    fun setEvCompensation(ev: Int) {
        val caps = _hardwareCapabilities.value
        val clamped = ev.coerceIn(caps.minEv, caps.maxEv)
        _evCompensation.value = clamped
        camera?.cameraControl?.setExposureCompensationIndex(clamped)
    }

    fun updateProSettings(update: (ProSettings) -> ProSettings) {
        _proSettings.value = update(_proSettings.value)
    }

    fun performShutterAction(onCaptured: (Uri?) -> Unit) {
        val mode = _currentMode.value
        if (mode.isVideoMode) {
            if (_isRecording.value) {
                stopRecording()
            } else {
                startRecording()
            }
        } else if (mode == CameraMode.PANORAMA) {
            if (_isCapturingPanorama.value) {
                finishPanorama(onCaptured)
            } else {
                startPanorama()
            }
        } else {
            // Photo modes (PHOTO, PORTRAIT, NIGHT, PRO)
            val seconds = _timerMode.value.seconds
            if (seconds > 0) {
                scope.launch {
                    for (i in seconds downTo 1) {
                        _countdownSeconds.value = i
                        delay(1000L)
                    }
                    _countdownSeconds.value = 0
                    captureStillPhoto(onCaptured)
                }
            } else {
                captureStillPhoto(onCaptured)
            }
        }
    }

    private fun captureStillPhoto(onCaptured: (Uri?) -> Unit) {
        val imageCap = imageCapture ?: return
        _isProcessing.value = true

        // Play shutter sound if enabled
        if (currentSettings.shutterSoundEnabled) {
            try { mediaActionSound.play(MediaActionSound.SHUTTER_CLICK) } catch (_: Exception) {}
        }

        // Screen flash for front camera in low light / on mode
        val isFront = _lensFacing.value == CameraSelector.LENS_FACING_FRONT
        if (isFront && _flashMode.value != FlashMode.OFF) {
            _showScreenFlash.value = true
        }

        imageCap.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    scope.launch {
                        _showScreenFlash.value = false
                        val bitmap = imageProxyToBitmap(image, isFront && currentSettings.mirrorSelfieEnabled)
                        image.close()

                        if (bitmap == null) {
                            _isProcessing.value = false
                            onCaptured(null)
                            return@launch
                        }

                        // Apply mode-specific processing
                        val processedBitmap = when (_currentMode.value) {
                            CameraMode.PORTRAIT -> PortraitProcessor.applyPortraitBokeh(
                                bitmap,
                                apertureFStop = _portraitAperture.value,
                                focusX = _focusPoint.value.x,
                                focusY = _focusPoint.value.y
                            )
                            CameraMode.NIGHT -> NightProcessor.enhanceLowLightImage(bitmap, 1.4f)
                            CameraMode.CINEMATIC -> CinematicProcessor.applyCinematicFilter(bitmap, _cinematicColorProfile.value)
                            else -> bitmap
                        }

                        val prefix = when (_currentMode.value) {
                            CameraMode.PORTRAIT -> "PORTRAIT"
                            CameraMode.NIGHT -> "NIGHT"
                            CameraMode.PRO -> "PRO"
                            else -> "IMG"
                        }

                        val savedUri = mediaRepository.saveBitmapToMediaStore(
                            processedBitmap,
                            titlePrefix = prefix,
                            isPortrait = _currentMode.value == CameraMode.PORTRAIT,
                            fStop = "f/${_portraitAperture.value}"
                        )

                        _lastCapturedMedia.value = savedUri
                        _isProcessing.value = false
                        onCaptured(savedUri)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    scope.launch {
                        _showScreenFlash.value = false
                        _isProcessing.value = false
                        Log.e("CameraController", "Photo capture failed: ${exception.message}", exception)
                        onCaptured(null)
                    }
                }
            }
        )
    }

    private fun startRecording() {
        val videoCap = videoCapture ?: return
        val fileName = "VID_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}"

        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "$fileName.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_DCIM + "/Camera")
            }
        }

        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        try {
            val pendingRecording = videoCap.output.prepareRecording(context, mediaStoreOutput)
            if (_isMicEnabled.value) {
                try {
                    // Check audio permission before enabling audio
                    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        pendingRecording.withAudioEnabled()
                    }
                } catch (e: SecurityException) {
                    Log.w("CameraController", "Audio recording permission missing: ${e.message}")
                }
            }

            if (currentSettings.shutterSoundEnabled) {
                try { mediaActionSound.play(MediaActionSound.START_VIDEO_RECORDING) } catch (_: Exception) {}
            }

            activeRecording = pendingRecording.start(ContextCompat.getMainExecutor(context)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        _isRecording.value = true
                        _isRecordingPaused.value = false
                        _recordingDurationSeconds.value = 0L
                        startDurationTimer()
                    }
                    is VideoRecordEvent.Pause -> {
                        _isRecordingPaused.value = true
                    }
                    is VideoRecordEvent.Resume -> {
                        _isRecordingPaused.value = false
                    }
                    is VideoRecordEvent.Finalize -> {
                        _isRecording.value = false
                        _isRecordingPaused.value = false
                        stopDurationTimer()
                        if (currentSettings.shutterSoundEnabled) {
                            try { mediaActionSound.play(MediaActionSound.STOP_VIDEO_RECORDING) } catch (_: Exception) {}
                        }
                        if (!event.hasError()) {
                            _lastCapturedMedia.value = event.outputResults.outputUri
                        } else {
                            Log.e("CameraController", "Video recording error: ${event.error}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CameraController", "Failed to start recording: ${e.message}", e)
        }
    }

    fun pauseRecording() {
        activeRecording?.pause()
    }

    fun resumeRecording() {
        activeRecording?.resume()
    }

    fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
    }

    private fun startDurationTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = scope.launch {
            while (_isRecording.value) {
                delay(1000L)
                if (!_isRecordingPaused.value) {
                    _recordingDurationSeconds.value += 1
                }
            }
        }
    }

    private fun stopDurationTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
    }

    private fun startPanorama() {
        _panoramaFrames.clear()
        _isCapturingPanorama.value = true
        _panoramaProgress.value = 0f
        capturePanoramaStep()
    }

    private fun capturePanoramaStep() {
        val imageCap = imageCapture ?: return
        imageCap.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bmp = imageProxyToBitmap(image, false)
                    image.close()
                    if (bmp != null) {
                        _panoramaFrames.add(bmp)
                        _panoramaProgress.value = (_panoramaFrames.size / 5f).coerceIn(0f, 1f)
                    }
                }
                override fun onError(exception: ImageCaptureException) {}
            }
        )
    }

    fun addPanoramaFrame() {
        if (_isCapturingPanorama.value && _panoramaFrames.size < 5) {
            capturePanoramaStep()
        }
    }

    private fun finishPanorama(onCaptured: (Uri?) -> Unit) {
        _isProcessing.value = true
        _isCapturingPanorama.value = false

        scope.launch {
            val stitched = PanoramaProcessor.stitchFrames(_panoramaFrames)
            if (stitched != null) {
                val uri = mediaRepository.saveBitmapToMediaStore(stitched, "PANO", false)
                _lastCapturedMedia.value = uri
                _isProcessing.value = false
                onCaptured(uri)
            } else {
                _isProcessing.value = false
                onCaptured(null)
            }
            _panoramaFrames.clear()
            _panoramaProgress.value = 0f
        }
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: run {
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                cameraProvider = future.get()
                refreshHardwareCapabilities()
                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(context))
            return
        }

        val surfaceProvider = previewSurfaceProvider ?: return

        try {
            provider.unbindAll()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(_lensFacing.value)
                .build()

            // Preview
            val preview = Preview.Builder()
                .build().also {
                    it.setSurfaceProvider(surfaceProvider)
                }

            // ImageCapture
            val flashConfig = when (_flashMode.value) {
                FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
                FlashMode.ON -> ImageCapture.FLASH_MODE_ON
                FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
            }

            val captureMode = if (_currentMode.value == CameraMode.NIGHT) {
                ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
            } else {
                ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(captureMode)
                .setFlashMode(flashConfig)
                .build()

            // VideoCapture
            val quality = when (currentSettings.videoResolution) {
                "4K" -> Quality.UHD
                "720p" -> Quality.HD
                else -> Quality.FHD
            }

            val qualitySelector = QualitySelector.from(quality, androidx.camera.video.FallbackStrategy.higherQualityOrLowerThan(Quality.SD))
            val recorder = Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .setExecutor(cameraExecutor)
                .build()

            videoCapture = VideoCapture.withOutput(recorder)

            // Bind to lifecycle
            camera = if (_currentMode.value.isVideoMode) {
                provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture, imageCapture)
            } else {
                provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
            }

            // Re-apply zoom & torch
            camera?.cameraControl?.setZoomRatio(_zoomRatio.value)
            camera?.cameraControl?.enableTorch(_isTorchOn.value)
        } catch (e: Exception) {
            Log.e("CameraController", "Use case binding failed: ${e.message}", e)
        }
    }

    private fun applyFlashToImageCapture() {
        val flashConfig = when (_flashMode.value) {
            FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
            FlashMode.ON -> ImageCapture.FLASH_MODE_ON
            FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        }
        imageCapture?.flashMode = flashConfig
    }

    private fun imageProxyToBitmap(image: ImageProxy, mirror: Boolean): Bitmap? {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

        val rotationDegrees = image.imageInfo.rotationDegrees
        if (rotationDegrees == 0 && !mirror) {
            return bitmap
        }

        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        if (mirror) {
            matrix.postScale(-1f, 1f)
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun onDestroy() {
        cameraExecutor.shutdown()
        mediaActionSound.release()
    }
}
