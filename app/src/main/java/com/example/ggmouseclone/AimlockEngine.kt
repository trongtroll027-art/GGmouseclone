package com.example.ggmouseclone

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlin.math.abs

class AimlockEngine(private val context: Context) {

    var isAimlockEnabled = false
        private set

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    fun initProjection(resultCode: Int, data: android.content.Intent) {
        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)
        
        val metrics = context.resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "AimlockScreen",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        startAimlockLoop()
    }

    fun toggleAimlock() {
        isAimlockEnabled = !isAimlockEnabled
        if (isAimlockEnabled && !isRunning) {
            Log.d("Aimlock", "Aimlock ON")
        } else {
            Log.d("Aimlock", "Aimlock OFF")
        }
    }

    private fun startAimlockLoop() {
        isRunning = true
        handler.post(object : Runnable {
            override fun run() {
                if (!isAimlockEnabled) {
                    handler.postDelayed(this, 100)
                    return
                }

                val image = imageReader?.acquireLatestImage()
                if (image != null) {
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * image.width

                    val bitmap = Bitmap.createBitmap(
                        image.width + rowPadding / pixelStride,
                        image.height,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.copyPixelsFromBuffer(buffer)
                    image.close()

                    processAimlock(bitmap)
                }

                handler.postDelayed(this, 50)
            }
        })
    }

    private fun processAimlock(bitmap: Bitmap) {
        val centerX = bitmap.width / 2
        val centerY = bitmap.height / 2
        var targetX = -1
        var targetY = -1
        var minDist = Int.MAX_VALUE

        for (y in (centerY - 200)..(centerY + 200) step 4) {
            for (x in (centerX - 200)..(centerX + 200) step 4) {
                if (x in 0 until bitmap.width && y in 0 until bitmap.height) {
                    val pixel = bitmap.getPixel(x, y)
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF

                    if (r > 180 && g < 80 && b < 80) {
                        val dist = abs(x - centerX) + abs(y - centerY)
                        if (dist < minDist) {
                            minDist = dist
                            targetX = x
                            targetY = y
                        }
                    }
                }
            }
        }

        if (targetX != -1 && targetY != -1) {
            val deltaX = targetX - centerX
            val deltaY = targetY - centerY

            MyAccessibilityService.instance?.swipe(
                centerX.toFloat(), centerY.toFloat(),
                (centerX + deltaX).toFloat(), (centerY + deltaY).toFloat(),
                10
            )
        }
    }

    fun stop() {
        isRunning = false
        virtualDisplay?.release()
        mediaProjection?.stop()
        handler.removeCallbacksAndMessages(null)
    }
}
