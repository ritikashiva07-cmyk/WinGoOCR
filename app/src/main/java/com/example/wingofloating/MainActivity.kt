package com.example.wingofloating

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val captureRequest = 501
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28, 40, 28, 24) }
        layout.addView(TextView(this).apply {
            text = "WinGo Floating Assistant\n\nScreen reading uses Android's official screen-capture permission and on-device OCR. Only visible screen content is processed. It does not predict or guarantee results."
            textSize = 17f
        })
        layout.addView(Button(this).apply {
            text = "Grant display-over-apps permission"
            setOnClickListener { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))) }
        })
        layout.addView(Button(this).apply {
            text = "Start floating panel"
            setOnClickListener {
                if (Settings.canDrawOverlays(this@MainActivity)) startService(Intent(this@MainActivity, OverlayService::class.java))
                else Toast.makeText(this@MainActivity, "Grant overlay permission first", Toast.LENGTH_LONG).show()
            }
        })
        layout.addView(Button(this).apply {
            text = "Start screen reading (OCR)"
            setOnClickListener {
                val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                startActivityForResult(manager.createScreenCaptureIntent(), captureRequest)
            }
        })
        layout.addView(Button(this).apply {
            text = "Stop screen reading"
            setOnClickListener { stopService(Intent(this@MainActivity, ScreenCaptureService::class.java)) }
        })
        layout.addView(Button(this).apply {
            text = "Stop floating panel"
            setOnClickListener { stopService(Intent(this@MainActivity, OverlayService::class.java)) }
        })
        setContentView(layout)
    }

    @Deprecated("Deprecated by Android; retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == captureRequest && resultCode == RESULT_OK && data != null) {
            val service = Intent(this, ScreenCaptureService::class.java).apply {
                putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
                putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data)
            }
            if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(service) else startService(service)
            Toast.makeText(this, "Screen reading started. Switch to the WinGo page.", Toast.LENGTH_LONG).show()
        }
    }
}
