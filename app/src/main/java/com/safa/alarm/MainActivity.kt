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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Prefs.deviceKey(this).isEmpty()) Prefs.setDeviceKey(this, UUID.randomUUID().toString())

        // Ask for notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
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
            text = "Customer watch service starting...\nThis app rings loudly whenever a customer " +
                "on the website asks for a price, requests the owner, or the chat gets serious.\n\n" +
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

        btn("Open Admin Chat") {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Net.ADMIN_URL)))
        }

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

        // register + start service
        Thread {
            try { Net.call(this, "register", org.json.JSONObject().put("name", android.os.Build.MODEL)) } catch (_: Exception) {}
        }.start()
        val i = Intent(this, AlarmService::class.java)
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
    }
}
