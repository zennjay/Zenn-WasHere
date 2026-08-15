package com.example.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.example.camera.CinematicColorProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CinematicProcessor {

    fun getColorMatrix(profile: CinematicColorProfile): ColorMatrix {
        return when (profile) {
            CinematicColorProfile.NATURAL -> ColorMatrix()
            CinematicColorProfile.CINEMATIC_WARM -> {
                ColorMatrix(
                    floatArrayOf(
                        1.12f, 0.05f, 0.0f, 0f, 10f,
                        0.02f, 1.05f, 0.0f, 0f, 5f,
                        0.0f, 0.02f, 0.88f, 0f, -5f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            CinematicColorProfile.COOL_TEAL -> {
                ColorMatrix(
                    floatArrayOf(
                        0.90f, 0.05f, 0.0f, 0f, -5f,
                        0.05f, 1.08f, 0.05f, 0f, 8f,
                        0.02f, 0.08f, 1.18f, 0f, 15f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            CinematicColorProfile.DRAMATIC_BW -> {
                val bw = ColorMatrix().apply { setSaturation(0f) }
                val contrast = ColorMatrix(
                    floatArrayOf(
                        1.35f, 0f, 0f, 0f, -25f,
                        0f, 1.35f, 0f, 0f, -25f,
                        0f, 0f, 1.35f, 0f, -25f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                bw.postConcat(contrast)
                bw
            }
            CinematicColorProfile.FILM_NOIR -> {
                val bw = ColorMatrix().apply { setSaturation(0.08f) }
                val film = ColorMatrix(
                    floatArrayOf(
                        1.20f, 0.05f, 0.02f, 0f, -15f,
                        0.02f, 1.15f, 0.02f, 0f, -15f,
                        0.01f, 0.02f, 1.05f, 0f, -10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                bw.postConcat(film)
                bw
            }
        }
    }

    suspend fun applyCinematicFilter(src: Bitmap, profile: CinematicColorProfile): Bitmap = withContext(Dispatchers.Default) {
        if (profile == CinematicColorProfile.NATURAL) return@withContext src
        try {
            val output = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(getColorMatrix(profile))
            }
            canvas.drawBitmap(src, 0f, 0f, paint)
            output
        } catch (e: Exception) {
            src
        }
    }
}
