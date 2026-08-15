package com.example.camera

import android.net.Uri
import androidx.camera.core.CameraSelector

enum class FlashMode(val label: String) {
    AUTO("Auto"),
    ON("On"),
    OFF("Off")
}

enum class HdrMode(val label: String) {
    AUTO("Auto HDR"),
    ON("HDR On"),
    OFF("HDR Off")
}

enum class TimerMode(val seconds: Int, val label: String) {
    OFF(0, "Off"),
    SEC_3(3, "3s"),
    SEC_10(10, "10s")
}

enum class VideoResolution(val label: String, val width: Int, val height: Int) {
    HD_720P("720p", 1280, 720),
    FHD_1080P("1080p", 1920, 1080),
    UHD_4K("4K", 3840, 2160)
}

enum class VideoFps(val fps: Int, val label: String) {
    FPS_24(24, "24 fps"),
    FPS_30(30, "30 fps"),
    FPS_60(60, "60 fps"),
    FPS_120(120, "120 fps"),
    FPS_240(240, "240 fps")
}

enum class CinematicColorProfile(val id: String, val label: String) {
    NATURAL("natural", "Natural"),
    CINEMATIC_WARM("cinematic_warm", "Warm Film"),
    COOL_TEAL("cool_teal", "Teal & Orange"),
    DRAMATIC_BW("dramatic_bw", "B&W Dramatic"),
    FILM_NOIR("film_noir", "Noir Classic")
}

data class FocusPointState(
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val isVisible: Boolean = false,
    val isLocked: Boolean = false,
    val timestamp: Long = 0L
)

data class HardwareCapabilities(
    val hasBackCamera: Boolean = true,
    val hasFrontCamera: Boolean = true,
    val hasFlash: Boolean = true,
    val hasFrontScreenFlash: Boolean = true,
    val minZoomRatio: Float = 1.0f,
    val maxZoomRatio: Float = 5.0f,
    val hasUltraWide: Boolean = false,
    val hasTelephoto: Boolean = false,
    val supportedZoomLevels: List<Float> = listOf(1.0f, 2.0f, 5.0f),
    val supportsOis: Boolean = false,
    val supportsEis: Boolean = false,
    val supports4K: Boolean = false,
    val supports60Fps: Boolean = false,
    val supports120Fps: Boolean = false,
    val supports240Fps: Boolean = false,
    val supportsManualExposure: Boolean = true,
    val supportsManualFocus: Boolean = true,
    val supportsManualIso: Boolean = true,
    val supportsManualWb: Boolean = true,
    val minIso: Int = 100,
    val maxIso: Int = 3200,
    val minExposureTimeNs: Long = 100000L,
    val maxExposureTimeNs: Long = 1000000000L,
    val minEv: Int = -4,
    val maxEv: Int = 4,
    val evStep: Float = 0.5f,
    val supportsHdrExtension: Boolean = false,
    val supportsNightExtension: Boolean = false,
    val supportsPortraitExtension: Boolean = false
)

data class ProSettings(
    val iso: Int = 100,
    val isIsoAuto: Boolean = true,
    val shutterSpeedIndex: Int = 0, // 0 = Auto
    val isShutterAuto: Boolean = true,
    val wbKelvin: Int = 5500, // 2000K to 10000K
    val isWbAuto: Boolean = true,
    val focusDistance: Float = 0.0f, // 0.0 = Infinity / Auto
    val isFocusAuto: Boolean = true,
    val evCompensation: Int = 0
)

data class CapturedMediaInfo(
    val uri: Uri,
    val isVideo: Boolean,
    val displayName: String,
    val dateAdded: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long = 0L,
    val iso: String? = null,
    val fNumber: String? = null,
    val exposureTime: String? = null,
    val focalLength: String? = null,
    val model: String? = null
)
