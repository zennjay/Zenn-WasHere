package com.example.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NightProcessor {

    suspend fun enhanceLowLightImage(src: Bitmap, exposureBoost: Float = 1.35f): Bitmap = withContext(Dispatchers.Default) {
        try {
            val width = src.width
            val height = src.height
            val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)

            // Tone curve matrix: boost shadows, preserve highlights, slight warmth
            val cm = ColorMatrix(
                floatArrayOf(
                    exposureBoost * 1.05f, 0f, 0f, 0f, 15f,
                    0f, exposureBoost * 1.02f, 0f, 0f, 12f,
                    0f, 0f, exposureBoost * 0.98f, 0f, 8f,
                    0f, 0f, 0f, 1f, 0f
                )
            )

            // Slight saturation boost for vivid low-light colors
            val satMatrix = ColorMatrix().apply { setSaturation(1.15f) }
            cm.postConcat(satMatrix)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(cm)
            }

            canvas.drawBitmap(src, 0f, 0f, paint)
            output
        } catch (e: Exception) {
            src
        }
    }
}
