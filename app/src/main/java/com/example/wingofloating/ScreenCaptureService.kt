package com.example.wingofloating

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.atomic.AtomicBoolean

class ScreenCaptureService : Service() {
    companion object {
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        private const val CHANNEL = "wingo_screen_reader"
        private const val NOTIFICATION_ID = 74
    }
    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var worker: HandlerThread? = null
    private var handler: Handler? = null
    private val busy = AtomicBoolean(false)
    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private var lastText = "Waiting for visible text…"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = Notification.Builder(this, CHANNEL)
            .setContentTitle("WinGo screen reader is active")
            .setContentText("Reading visible screen text on this device")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true).build()
        startForeground(NOTIFICATION_ID, notification)
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data = if (Build.VERSION.SDK_INT >= 33) intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                   else @Suppress("DEPRECATION") intent?.getParcelableExtra(EXTRA_RESULT_DATA) as? Intent
        if (resultCode != Activity.RESULT_OK || data == null) { stopSelf(); return START_NOT_STICKY }
        try { beginCapture(resultCode, data) } catch (_: Exception) { stopSelf() }
        return START_NOT_STICKY
    }

    private fun beginCapture(resultCode: Int, data: Intent) {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi
        worker = HandlerThread("WingoOCR").also { it.start() }
        handler = Handler(worker!!.looper)
        reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        reader!!.setOnImageAvailableListener({ source ->
            val image = source.acquireLatestImage() ?: return@setOnImageAvailableListener
            if (!busy.compareAndSet(false, true)) { image.close(); return@setOnImageAvailableListener }
            processImage(image)
        }, handler)
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = manager.getMediaProjection(resultCode, data)
        projection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() { stopSelf() }
        }, handler)
        virtualDisplay = projection?.createVirtualDisplay("WinGoOCR", width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader!!.surface, null, handler)
    }

    private fun processImage(image: Image) {
        try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * image.width
            val bitmap = Bitmap.createBitmap(image.width + rowPadding / pixelStride, image.height, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(buffer)
            val cropped = Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
            bitmap.recycle()
            val input = InputImage.fromBitmap(cropped, 0)
            recognizer.process(input)
                .addOnSuccessListener { result ->
                    val text = result.text.lines().map { it.trim() }.filter { it.isNotEmpty() }.take(12).joinToString(" | ")
                    if (text.isNotBlank()) {
                        lastText = text.take(500)
                        getSharedPreferences("ocr", MODE_PRIVATE).edit().putString("latest_text", lastText).apply()
                    }
                }
                .addOnCompleteListener { cropped.recycle(); image.close(); busy.set(false) }
        } catch (_: Exception) { image.close(); busy.set(false) }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL, "Screen reader", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        virtualDisplay?.release(); reader?.close(); projection?.stop()
        recognizer.close(); worker?.quitSafely()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
