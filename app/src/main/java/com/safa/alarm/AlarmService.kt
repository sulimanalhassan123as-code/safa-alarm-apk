package com.safa.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.net.Uri
import android.os.IBinder
import android.os.SystemClock
import org.json.JSONArray
import org.json.JSONObject

class AlarmService : Service() {

    private lateinit var thread: HandlerThread
    private lateinit var handler: Handler
    private var lastPoll = 0L

    companion object { const val TICK_MS = 20_000L }

    override fun onCreate() {
        super.onCreate()
        thread = HandlerThread("safa-poll").apply { start() }
        handler = Handler(thread.looper)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1001, buildNotification())
        handler.removeCallbacksAndMessages(null)
        handler.post { loop() }
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        thread.quitSafely()
        super.onDestroy()
    }

    private fun loop() {
        try { poll() } catch (_: Exception) { }
        if (lastPoll == 0L) lastPoll = SystemClock.elapsedRealtime()
        handler.postDelayed({ loop() }, TICK_MS)
    }

    private fun poll() {
        val resp = Net.call(this, "poll")
        val alerts = resp.optJSONArray("alerts") ?: JSONArray()
        val firsts = resp.optJSONArray("firsts") ?: JSONArray()

        // first successful poll: swallow history without ringing
        if (Prefs.firstRun(this)) {
            for (i in 0 until alerts.length()) {
                Prefs.markSeen(this, alerts.optJSONObject(i)?.optString("id") ?: continue)
            }
            for (i in 0 until firsts.length()) {
                Prefs.markSeen(this, "f_" + (firsts.optJSONObject(i)?.optString("id") ?: continue))
            }
            Prefs.setFirstRun(this, false)
            return
        }

        var ring: JSONObject? = null
        for (i in 0 until alerts.length()) {
            val a = alerts.optJSONObject(i) ?: continue
            val id = a.optString("id")
            if (id.isNotEmpty() && !Prefs.seen(this).contains(id)) {
                Prefs.markSeen(this, id)
                if (ring == null) ring = a
            }
        }
        for (i in 0 until firsts.length()) {
            val f = firsts.optJSONObject(i) ?: continue
            val id = "f_" + f.optString("id")
            if (id != "f_" && !Prefs.seen(this).contains(id)) {
                Prefs.markSeen(this, id)
                if (ring == null) {
                    ring = JSONObject().apply {
                        put("id", f.optString("id"))
                        put("reason", "new_lead")
                        put("text", f.optString("text"))
                    }
                }
            }
        }
        if (ring != null) fireAlarm(ring)
    }

    private fun fireAlarm(a: JSONObject) {
        val reason = a.optString("reason", "alert")
        val text = a.optString("text", "")
        val id = a.optString("id", "")
        val label = when (reason) {
            "price_request" -> "Customer asking for PRICE"
            "owner_request" -> "Customer wants to talk to YOU"
            "deep_chat" -> "Customer getting serious"
            "test_alarm" -> "TEST ALARM"
            else -> "New customer message"
        }

        val i = Intent(this, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("title", label)
            putExtra("text", text)
            putExtra("alert_id", id)
        }
        // With "display over other apps" granted this launches over anything, even the lock screen.
        // Without it Android 10+ blocks background launches -> falls back to the full-screen notification.
        try {
            if (android.provider.Settings.canDrawOverlays(this)) startActivity(i)
        } catch (_: Exception) { }

        // full-screen notification fallback (rings over lock screen like a call)
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel("safa_alarm", "Safa Alarm", NotificationManager.IMPORTANCE_HIGH)
            val sound = Uri.parse("android.resource://" + packageName + "/" + R.raw.alarm)
            ch.setSound(sound, android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_ALARM).build())
            ch.enableVibration(true)
            ch.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            nm.createNotificationChannel(ch)
            val pi = PendingIntent.getActivity(this, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val n = Notification.Builder(this, "safa_alarm")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Safa: $label")
                .setContentText(text)
                .setPriority(Notification.PRIORITY_MAX)
                .setCategory(Notification.CATEGORY_ALARM)
                .setFullScreenIntent(pi, true)
                .setAutoCancel(true)
                .build()
            nm.notify(2002, n)
        }
    }

    private fun buildNotification(): Notification {
        val pi = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel("safa_watch", "Safa Alarm Watch", NotificationManager.IMPORTANCE_MIN)
            nm.createNotificationChannel(ch)
            return Notification.Builder(this, "safa_watch")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Safa Alarm")
                .setContentText("Watching for customers...")
                .setContentIntent(pi)
                .setOngoing(true)
                .build()
        }
        @Suppress("DEPRECATION")
        return Notification.Builder(this)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Safa Alarm")
            .setContentText("Watching for customers...")
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }
}
