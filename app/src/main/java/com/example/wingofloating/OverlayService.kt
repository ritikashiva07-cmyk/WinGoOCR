package com.example.wingofloating

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var panel: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var timerView: TextView? = null
    private var resultView: TextView? = null
    private var count = 0
    private val tick = object : Runnable {
        override fun run() {
            val remain = 30 - ((System.currentTimeMillis() / 1000) % 30).toInt()
            timerView?.text = "WinGo 30S  •  %02d".format(remain)
            val ocrText = getSharedPreferences("ocr", MODE_PRIVATE).getString("latest_text", "OCR: waiting for screen reader")
            resultView?.text = "Screen text: ${ocrText?.take(140)}"
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showPanel()
        handler.post(tick)
    }

    private fun showPanel() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 12, 14, 12)
            setBackgroundColor(Color.rgb(22, 35, 55))
        }
        timerView = TextView(this).apply {
            text = "WinGo 30S"
            textSize = 17f
            setTextColor(Color.CYAN)
        }
        resultView = TextView(this).apply {
            text = "OCR: waiting for screen reader"
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(0, 8, 0, 8)
        }
        val add = Button(this).apply {
            text = "Manual entry +1"
            setOnClickListener {
                count++
                resultView?.text = "Manual entries: $count"
            }
        }
        val close = Button(this).apply {
            text = "Close panel"
            setOnClickListener { stopSelf() }
        }
        root.addView(timerView)
        root.addView(resultView)
        root.addView(add)
        root.addView(close)

        val type = if (android.os.Build.VERSION.SDK_INT >= 26)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 20; y = 180
        }

        var startX = 0f; var startY = 0f; var initialX = 0; var initialY = 0
        root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX; startY = event.rawY
                    initialX = params.x; initialY = params.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX - (event.rawX - startX).toInt()
                    params.y = initialY + (event.rawY - startY).toInt()
                    wm.updateViewLayout(root, params)
                    true
                }
                else -> false
            }
        }
        panel = root
        wm.addView(root, params)
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        panel?.let { if (it.isAttachedToWindow) wm.removeView(it) }
        panel = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
