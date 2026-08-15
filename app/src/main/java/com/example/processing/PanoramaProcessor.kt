package com.example.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PanoramaProcessor {

    suspend fun stitchFrames(frames: List<Bitmap>): Bitmap? = withContext(Dispatchers.Default) {
        if (frames.isEmpty()) return@withContext null
        if (frames.size == 1) return@withContext frames.first()

        try {
            val singleWidth = frames[0].width
            val height = frames[0].height
            // Overlap ~30% between frames
            val stepWidth = (singleWidth * 0.65f).toInt()
            val totalWidth = singleWidth + stepWidth * (frames.size - 1)

            val panorama = Bitmap.createBitmap(totalWidth, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(panorama)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

            for (i in frames.indices) {
                val frame = frames[i]
                val xPos = (i * stepWidth).toFloat()
                canvas.drawBitmap(frame, xPos, 0f, paint)
            }

            panorama
        } catch (e: Exception) {
            frames.firstOrNull()
        }
    }
}
