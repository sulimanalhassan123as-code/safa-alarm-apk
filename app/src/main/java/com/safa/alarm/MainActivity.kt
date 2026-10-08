package com.safa.alarm

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.UUID

class MainActivity : Activity() {

    private var preview: android.media.MediaPlayer? = null

    private fun stopPreview() {
        try { preview?.stop() } catch (_: Exception) { }
        try { preview?.release() } catch (_: Exception) { }
        preview = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Prefs.deviceKey(this).isEmpty()) Prefs.setDeviceKey(this, UUID.randomUUID().toString())

        // Ask for notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        // CRITICAL: without "display over other apps" Android blocks the alarm screen from
        // launching by itself -> you only get a silent notification. Open the settings now.
        if (!android.provider.Settings.canDrawOverlays(this)) {
            try {
                startActivity(android.content.Intent(
                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:com.safa.alarm")
                ))
            } catch (_: Exception) { }
        }

        val pad = (resources.displayMetrics.density * 20).toInt()
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }
        val title = TextView(this).apply {
            text = "Safa Alarm"
            textSize = 26f
            setTextColor(0xFF1a3a5c.toInt())
        }
        val status = TextView(this).apply {
            text = "Customer watch service starting...\nThis app rings whenever a customer " +
                "on the website asks for a price, requests the owner, or the chat gets serious.\n\n" +
                "IMPORTANT: for the alarm to ring over the lock screen by itself, allow it under \"Display over other apps\" (the setting opens automatically on first launch).\n\n" +
                "Keep it installed on this phone only. The service runs in the background " +
                "and shows a small ongoing notification."
            textSize = 15f
            setPadding(0, pad / 2, 0, pad)
        }
        root.addView(title)
        root.addView(status)

        fun btn(label: String, onClick: () -> Unit): Button {
            val b = Button(this)
            b.text = label
            b.setOnClickListener { onClick() }
            b.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = pad / 2 }
            root.addView(b)
            return b
        }

        btn("🔔  TEST ALARM (check the ringing)") {
            Thread {
                try { Net.call(this, "test") } catch (_: Exception) {}
            }.start()
            status.text = "Test alarm sent. Your phone should ring within 20 seconds..."
        }

        btn("Allow alarm over other apps (FIXES silent alarms)") {
            try {
                startActivity(android.content.Intent(
                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:com.safa.alarm")
                ))
            } catch (_: Exception) { }
        }

        btn("Open Admin Chat") {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Net.ADMIN_URL)))
        }

        // ---- Alarm sound picker ----
        root.addView(TextView(this).apply {
            text = "Alarm sound (tap to hear & set)"
            textSize = 18f
            setTextColor(0xFF1a3a5c.toInt())
            setPadding(0, pad, 0, pad / 2)
        })
        val soundStatus = TextView(this).apply {
            text = "Current: " + Sfx.displayName(Prefs.soundName(this@MainActivity))
            textSize = 14f
            setPadding(0, 0, 0, pad / 2)
        }
        for ((id, label, desc) in Sfx.OPTIONS) {
            btn("🔔  $label — $desc") {
                Prefs.setSoundName(this, id)
                stopPreview()
                preview = android.media.MediaPlayer.create(this, Sfx.resId(id))
                preview?.start()
                soundStatus.text = "Current: " + Sfx.displayName(id) + " (saved)"
            }
        }
        root.addView(soundStatus)

        btn("Allow alarm in battery settings (important)") {
            try {
                startActivity(Intent(
                    android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:com.safa.alarm")
                ))
            } catch (_: Exception) {
                startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:com.safa.alarm")))
            }
        }

        btn("Start / restart watch service") {
            val i = Intent(this, AlarmService::class.java)
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
        }

        scroll.addView(root)
        setContentView(scroll)

        stopPreview()
        // register + start service
        Thread {
            try { Net.call(this, "register", org.json.JSONObject().put("name", android.os.Build.MODEL)) } catch (_: Exception) {}
        }.start()
        val i = Intent(this, AlarmService::class.java)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
    }
}
