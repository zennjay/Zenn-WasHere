package com.example.camera

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class DeviceTiltState(
    val pitch: Float = 0f, // vertical tilt in degrees
    val roll: Float = 0f,   // horizontal tilt in degrees (level)
    val isLevel: Boolean = false,
    val azimuth: Float = 0f // heading for panorama
)

class OrientationSensorHelper(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _tiltState = MutableStateFlow(DeviceTiltState())
    val tiltState: StateFlow<DeviceTiltState> = _tiltState.asStateFlow()

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private val lastAccel = FloatArray(3)
    private val lastMag = FloatArray(3)
    private var hasAccel = false
    private var hasMag = false

    fun startListening() {
        if (sensorManager == null) return
        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            accelSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            magSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

                val isLevel = abs(roll) < 1.5f || abs(roll - 90f) < 1.5f || abs(roll + 90f) < 1.5f
                _tiltState.value = DeviceTiltState(pitch = pitch, roll = roll, isLevel = isLevel, azimuth = azimuth)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, lastAccel, 0, event.values.size)
                hasAccel = true
                calculateFromAccelAndMag()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, lastMag, 0, event.values.size)
                hasMag = true
                calculateFromAccelAndMag()
            }
        }
    }

    private fun calculateFromAccelAndMag() {
        if (hasAccel && hasMag) {
            val success = SensorManager.getRotationMatrix(rotationMatrix, null, lastAccel, lastMag)
            if (success) {
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
                val isLevel = abs(roll) < 1.5f
                _tiltState.value = DeviceTiltState(pitch = pitch, roll = roll, isLevel = isLevel, azimuth = azimuth)
            }
        } else if (hasAccel) {
            val ax = lastAccel[0]
            val ay = lastAccel[1]
            val az = lastAccel[2]
            val roll = Math.toDegrees(atan2(ax.toDouble(), sqrt((ay * ay + az * az).toDouble()))).toFloat()
            val pitch = Math.toDegrees(atan2(ay.toDouble(), sqrt((ax * ax + az * az).toDouble()))).toFloat()
            val isLevel = abs(roll) < 1.5f
            _tiltState.value = DeviceTiltState(pitch = pitch, roll = roll, isLevel = isLevel)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
