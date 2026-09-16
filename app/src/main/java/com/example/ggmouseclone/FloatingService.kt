package com.example.ggmouseclone

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast

class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var keymapView: KeymapOverlayView
    private var usbHandler: UsbInputHandler? = null
    private var aimlockEngine: AimlockEngine? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification())

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        aimlockEngine = AimlockEngine(this)

        usbHandler = UsbInputHandler(this)
        usbHandler?.setOnKeyListener { keyCode ->
            if (keyCode == 34) {
                aimlockEngine?.toggleAimlock()
                Toast.makeText(
                    this,
                    "Aimlock: ${if (aimlockEngine?.isAimlockEnabled == true) "ON" else "OFF"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        usbHandler?.start()

        createOverlay()
    }

    private fun createOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.overlay_keymap, null)
        keymapView = overlayView.findViewById(R.id.keymap_container)

        overlayView.findViewById<Button>(R.id.btn_add_component).setOnClickListener {
            Toast.makeText(this, "Chọn thành phần để thêm vào Keymap", Toast.LENGTH_SHORT).show()
        }

        overlayView.findViewById<Button>(R.id.btn_aimlock_toggle).setOnClickListener {
            aimlockEngine?.toggleAimlock()
        }

        windowManager.addView(overlayView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::overlayView.isInitialized) windowManager.removeView(overlayView)
        usbHandler?.stop()
        aimlockEngine?.stop()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "gg_mouse_channel",
                "GG Mouse Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "gg_mouse_channel")
                .setContentTitle("GG Mouse Clone")
                .setContentText("Đang chạy overlay")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("GG Mouse Clone")
                .setContentText("Đang chạy overlay")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .build()
        }
    }
}
