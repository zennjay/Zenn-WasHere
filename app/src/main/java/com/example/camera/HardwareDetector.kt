package com.example.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.util.Range
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object HardwareDetector {

    fun detectCapabilities(context: Context, lensFacing: Int): HardwareCapabilities {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return HardwareCapabilities()

            var backId: String? = null
            var frontId: String? = null
            var targetId: String? = null

            val cameraIds = cameraManager.cameraIdList
            var hasBack = false
            var hasFront = false

            for (id in cameraIds) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    hasBack = true
                    if (backId == null) backId = id
                } else if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    hasFront = true
                    if (frontId == null) frontId = id
                }
            }

            targetId = if (lensFacing == CameraSelector.LENS_FACING_FRONT) frontId ?: cameraIds.firstOrNull()
            else backId ?: cameraIds.firstOrNull()

            if (targetId == null) {
                return HardwareCapabilities(hasBackCamera = hasBack, hasFrontCamera = hasFront)
            }

            val chars = cameraManager.getCameraCharacteristics(targetId)

            // Flash
            val flashAvailable = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

            // Zoom range
            var minZoom = 1.0f
            var maxZoom = 5.0f
            var hasUltraWide = false
            var hasTelephoto = false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val zoomRange = chars.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
                if (zoomRange != null) {
                    minZoom = zoomRange.lower.coerceAtLeast(0.5f)
                    maxZoom = zoomRange.upper.coerceAtMost(30.0f)
                    if (minZoom <= 0.6f) hasUltraWide = true
                    if (maxZoom >= 3.0f) hasTelephoto = true
                } else {
                    val maxScaler = chars.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 5.0f
                    maxZoom = maxScaler.coerceIn(2.0f, 10.0f)
                }
            } else {
                val maxScaler = chars.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 5.0f
                maxZoom = maxScaler.coerceIn(2.0f, 10.0f)
            }

            val zoomLevels = mutableListOf<Float>()
            if (hasUltraWide && minZoom <= 0.6f) zoomLevels.add(0.5f)
            zoomLevels.add(1.0f)
            if (maxZoom >= 2.0f) zoomLevels.add(2.0f)
            if (hasTelephoto || maxZoom >= 3.0f) zoomLevels.add(3.0f)
            if (maxZoom >= 5.0f) zoomLevels.add(5.0f)

            // Stabilization OIS / EIS
            val oisModes = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
            val supportsOis = oisModes?.any { it == CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON } == true

            val videoStabModes = chars.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
            val supportsEis = videoStabModes?.any { it == CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_ON } == true

            // Resolution & FPS from StreamConfigurationMap
            val map: StreamConfigurationMap? = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            var supports4K = false
            var supports60Fps = false
            var supports120Fps = false
            var supports240Fps = false

            if (map != null) {
                val outputSizes = map.getOutputSizes(android.media.MediaRecorder::class.java)
                    ?: map.getOutputSizes(ImageFormat.JPEG) ?: emptyArray()

                for (size in outputSizes) {
                    if (size.width >= 3840 && size.height >= 2160) {
                        supports4K = true
                    }
                }

                val fpsRanges: Array<Range<Int>>? = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                fpsRanges?.forEach { range ->
                    if (range.upper >= 60) supports60Fps = true
                    if (range.upper >= 120) supports120Fps = true
                    if (range.upper >= 240) supports240Fps = true
                }

                // High speed video sizes
                try {
                    val highSpeedFps = map.highSpeedVideoFpsRanges
                    if (!highSpeedFps.isNullOrEmpty()) {
                        highSpeedFps.forEach { r ->
                            if (r.upper >= 120) supports120Fps = true
                            if (r.upper >= 240) supports240Fps = true
                        }
                    }
                } catch (_: Exception) {}
            }

            // ISO & Exposure ranges
            val isoRange = chars.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
            val minIso = isoRange?.lower ?: 100
            val maxIso = isoRange?.upper ?: 3200

            val expRange = chars.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
            val minExp = expRange?.lower ?: 100000L
            val maxExp = expRange?.upper ?: 1000000000L

            // EV Compensation
            val aeRange = chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
            val minEv = aeRange?.lower ?: -4
            val maxEv = aeRange?.upper ?: 4
            val aeStep = chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)?.toFloat() ?: 0.5f

            HardwareCapabilities(
                hasBackCamera = hasBack,
                hasFrontCamera = hasFront,
                hasFlash = flashAvailable,
                hasFrontScreenFlash = true,
                minZoomRatio = minZoom,
                maxZoomRatio = maxZoom,
                hasUltraWide = hasUltraWide,
                hasTelephoto = hasTelephoto,
                supportedZoomLevels = zoomLevels.distinct(),
                supportsOis = supportsOis,
                supportsEis = supportsEis,
                supports4K = supports4K,
                supports60Fps = supports60Fps,
                supports120Fps = supports120Fps,
                supports240Fps = supports240Fps,
                supportsManualExposure = true,
                supportsManualFocus = true,
                supportsManualIso = isoRange != null,
                supportsManualWb = true,
                minIso = minIso,
                maxIso = maxIso,
                minExposureTimeNs = minExp,
                maxExposureTimeNs = maxExp,
                minEv = minEv,
                maxEv = maxEv,
                evStep = aeStep
            )
        } catch (e: Exception) {
            HardwareCapabilities()
        }
    }
}
