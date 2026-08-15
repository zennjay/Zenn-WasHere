package com.example.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.camera.CinematicColorProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.cameraDataStore by preferencesDataStore(name = "camera_settings")

data class CameraSettings(
    val photoQuality: String = "High",
    val videoResolution: String = "1080p",
    val videoFps: Int = 30,
    val stabilizationEnabled: Boolean = true,
    val gridEnabled: Boolean = true,
    val levelEnabled: Boolean = true,
    val shutterSoundEnabled: Boolean = true,
    val mirrorSelfieEnabled: Boolean = true,
    val volumeButtonAction: String = "Shutter", // Shutter, Zoom, Volume
    val cinematicFps: Int = 24,
    val cinematicResolution: String = "1080p",
    val cinematicFocusSpeedMs: Long = 1200L,
    val subjectTrackingEnabled: Boolean = true,
    val defaultColorProfile: String = CinematicColorProfile.NATURAL.id,
    val noiseReduction: String = "High Quality",
    val saveLocation: String = "DCIM/Camera"
)

class CameraPreferencesRepository(private val context: Context) {

    private object Keys {
        val PHOTO_QUALITY = stringPreferencesKey("photo_quality")
        val VIDEO_RESOLUTION = stringPreferencesKey("video_resolution")
        val VIDEO_FPS = intPreferencesKey("video_fps")
        val STABILIZATION = booleanPreferencesKey("stabilization")
        val GRID = booleanPreferencesKey("grid")
        val LEVEL = booleanPreferencesKey("level")
        val SHUTTER_SOUND = booleanPreferencesKey("shutter_sound")
        val MIRROR_SELFIE = booleanPreferencesKey("mirror_selfie")
        val VOLUME_ACTION = stringPreferencesKey("volume_action")
        val CINEMATIC_FPS = intPreferencesKey("cinematic_fps")
        val CINEMATIC_RES = stringPreferencesKey("cinematic_res")
        val CINEMATIC_FOCUS_SPEED = intPreferencesKey("cinematic_focus_speed")
        val SUBJECT_TRACKING = booleanPreferencesKey("subject_tracking")
        val COLOR_PROFILE = stringPreferencesKey("color_profile")
        val NOISE_REDUCTION = stringPreferencesKey("noise_reduction")
    }

    val settingsFlow: Flow<CameraSettings> = context.cameraDataStore.data.map { prefs ->
        CameraSettings(
            photoQuality = prefs[Keys.PHOTO_QUALITY] ?: "High",
            videoResolution = prefs[Keys.VIDEO_RESOLUTION] ?: "1080p",
            videoFps = prefs[Keys.VIDEO_FPS] ?: 30,
            stabilizationEnabled = prefs[Keys.STABILIZATION] ?: true,
            gridEnabled = prefs[Keys.GRID] ?: true,
            levelEnabled = prefs[Keys.LEVEL] ?: true,
            shutterSoundEnabled = prefs[Keys.SHUTTER_SOUND] ?: true,
            mirrorSelfieEnabled = prefs[Keys.MIRROR_SELFIE] ?: true,
            volumeButtonAction = prefs[Keys.VOLUME_ACTION] ?: "Shutter",
            cinematicFps = prefs[Keys.CINEMATIC_FPS] ?: 24,
            cinematicResolution = prefs[Keys.CINEMATIC_RES] ?: "1080p",
            cinematicFocusSpeedMs = (prefs[Keys.CINEMATIC_FOCUS_SPEED] ?: 1200).toLong(),
            subjectTrackingEnabled = prefs[Keys.SUBJECT_TRACKING] ?: true,
            defaultColorProfile = prefs[Keys.COLOR_PROFILE] ?: CinematicColorProfile.NATURAL.id,
            noiseReduction = prefs[Keys.NOISE_REDUCTION] ?: "High Quality"
        )
    }

    suspend fun updateGrid(enabled: Boolean) {
        context.cameraDataStore.edit { it[Keys.GRID] = enabled }
    }

    suspend fun updateLevel(enabled: Boolean) {
        context.cameraDataStore.edit { it[Keys.LEVEL] = enabled }
    }

    suspend fun updateStabilization(enabled: Boolean) {
        context.cameraDataStore.edit { it[Keys.STABILIZATION] = enabled }
    }

    suspend fun updateMirrorSelfie(enabled: Boolean) {
        context.cameraDataStore.edit { it[Keys.MIRROR_SELFIE] = enabled }
    }

    suspend fun updateShutterSound(enabled: Boolean) {
        context.cameraDataStore.edit { it[Keys.SHUTTER_SOUND] = enabled }
    }

    suspend fun updateVideoResolution(res: String) {
        context.cameraDataStore.edit { it[Keys.VIDEO_RESOLUTION] = res }
    }

    suspend fun updateVideoFps(fps: Int) {
        context.cameraDataStore.edit { it[Keys.VIDEO_FPS] = fps }
    }

    suspend fun updateCinematicFps(fps: Int) {
        context.cameraDataStore.edit { it[Keys.CINEMATIC_FPS] = fps }
    }

    suspend fun updateCinematicRes(res: String) {
        context.cameraDataStore.edit { it[Keys.CINEMATIC_RES] = res }
    }

    suspend fun updateVolumeAction(action: String) {
        context.cameraDataStore.edit { it[Keys.VOLUME_ACTION] = action }
    }

    suspend fun updateSubjectTracking(enabled: Boolean) {
        context.cameraDataStore.edit { it[Keys.SUBJECT_TRACKING] = enabled }
    }

    suspend fun updateNoiseReduction(mode: String) {
        context.cameraDataStore.edit { it[Keys.NOISE_REDUCTION] = mode }
    }

    suspend fun resetDefaults() {
        context.cameraDataStore.edit { it.clear() }
    }
}
