package com.example.processing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

object PortraitProcessor {

    suspend fun applyPortraitBokeh(
        src: Bitmap,
        apertureFStop: Float = 2.0f, // 1.4 to 16.0
        focusX: Float = 0.5f,
        focusY: Float = 0.45f
    ): Bitmap = withContext(Dispatchers.Default) {
        try {
            val width = src.width
            val height = src.height
            val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            // Blur radius based on aperture (lower f-stop = bigger blur)
            val blurFactor = ((16.0f - apertureFStop.coerceIn(1.4f, 16.0f)) / 14.6f).coerceIn(0f, 1f)
            if (blurFactor < 0.05f) {
                return@withContext src
            }

            // Downsample and upsample for fast Gaussian blur simulation
            val sampleScale = max(2, (blurFactor * 8).toInt())
            val smallWidth = max(16, width / sampleScale)
            val smallHeight = max(16, height / sampleScale)

            val smallBitmap = Bitmap.createScaledBitmap(src, smallWidth, smallHeight, true)
            val blurredSmall = fastBlur(smallBitmap, radius = max(2, (blurFactor * 12).toInt()))
            val blurredFull = Bitmap.createScaledBitmap(blurredSmall, width, height, true)

            // Blend blurred background with sharp foreground using radial subject mask
            val canvas = Canvas(result)
            canvas.drawBitmap(blurredFull, 0f, 0f, null)

            val centerX = width * focusX
            val centerY = height * focusY
            val radius = min(width, height) * 0.42f

            val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    centerX, centerY, radius,
                    intArrayOf(Color.WHITE, Color.argb(200, 255, 255, 255), Color.argb(0, 255, 255, 255)),
                    floatArrayOf(0.0f, 0.65f, 1.0f),
                    Shader.TileMode.CLAMP
                )
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
            }

            // Draw sharp foreground over blurred back with mask
            val sharpMasked = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val sharpCanvas = Canvas(sharpMasked)
            sharpCanvas.drawBitmap(src, 0f, 0f, null)
            sharpCanvas.drawPaint(Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = maskPaint.shader
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            })

            canvas.drawBitmap(sharpMasked, 0f, 0f, null)
            result
        } catch (e: Exception) {
            src
        }
    }

    private fun fastBlur(sentBitmap: Bitmap, radius: Int): Bitmap {
        val bitmap = sentBitmap.copy(sentBitmap.config ?: Bitmap.Config.ARGB_8888, true)
        if (radius < 1) return bitmap

        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        val vmin = IntArray(max(w, h))

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        for (idx in 0 until 256 * divsum) {
            dv[idx] = idx / divsum
        }

        yi = 0
        var yw = 0

        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val routsum: Int
        val goutsum: Int
        val boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int

        for (curY in 0 until h) {
            rinsum = 0
            ginsum = 0
            binsum = 0
            var routsumCur = 0
            var goutsumCur = 0
            var boutsumCur = 0
            rsum = 0
            gsum = 0
            bsum = 0
            for (curI in -radius..radius) {
                p = pix[yi + min(wm, max(curI, 0))]
                sir = stack[curI + radius]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = p and 0x0000ff
                rbs = radius + 1 - abs(curI)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (curI > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsumCur += sir[0]
                    goutsumCur += sir[1]
                    boutsumCur += sir[2]
                }
            }
            stackpointer = radius

            for (curX in 0 until w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                rsum -= routsumCur
                gsum -= goutsumCur
                bsum -= boutsumCur

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsumCur -= sir[0]
                goutsumCur -= sir[1]
                boutsumCur -= sir[2]

                if (curY == 0) {
                    vmin[curX] = min(curX + radius + 1, wm)
                }
                p = pix[yw + vmin[curX]]

                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = p and 0x0000ff

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]

                routsumCur += sir[0]
                goutsumCur += sir[1]
                boutsumCur += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi++
            }
            yw += w
        }

        for (curX in 0 until w) {
            rinsum = 0
            ginsum = 0
            binsum = 0
            var routsumCur = 0
            var goutsumCur = 0
            var boutsumCur = 0
            rsum = 0
            gsum = 0
            bsum = 0
            yp = -radius * w
            for (curI in -radius..radius) {
                yi = max(0, yp) + curX
                sir = stack[curI + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = radius + 1 - abs(curI)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                if (curI > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsumCur += sir[0]
                    goutsumCur += sir[1]
                    boutsumCur += sir[2]
                }
                if (curI < hm) {
                    yp += w
                }
            }
            yi = curX
            stackpointer = radius
            for (curY in 0 until h) {
                pix[yi] = (-0x1000000 and pix[yi]) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                rsum -= routsumCur
                gsum -= goutsumCur
                bsum -= boutsumCur

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsumCur -= sir[0]
                goutsumCur -= sir[1]
                boutsumCur -= sir[2]

                if (curX == 0) {
                    vmin[curY] = min(curY + radius + 1, hm) * w
                }
                p = curX + vmin[curY]

                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]

                routsumCur += sir[0]
                goutsumCur += sir[1]
                boutsumCur += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi += w
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }

    private fun abs(v: Int): Int = if (v < 0) -v else v
}
