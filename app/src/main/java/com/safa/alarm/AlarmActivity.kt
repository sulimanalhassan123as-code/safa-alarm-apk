package com.safa.alarm

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class AlarmActivity : Activity() {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var alertId = ""
    private var origVolume = -1
    private var silenceHandler: android.os.Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        if (Build.VERSION.SDK_INT >= 27) setShowWhenLocked(true)
        if (Build.VERSION.SDK_INT >= 27) setTurnScreenOn(true)

        val title = intent.getStringExtra("title") ?: "Safa Alarm"
        val text = intent.getStringExtra("text") ?: ""
        alertId = intent.getStringExtra("alert_id") ?: ""

        val pad = (resources.displayMetrics.density * 24).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF1a3a5c.toInt())
            setPadding(pad, pad * 2, pad, pad)
            gravity = Gravity.CENTER
        }

        root.addView(TextView(this).apply {
            this.text = "🔔🔔🔔"
            textSize = 44f
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            this.text = title
            textSize = 26f
            setTextColor(0xFFd4a017.toInt())
            gravity = Gravity.CENTER
            setPadding(0, pad, 0, pad / 2)
        })
        root.addView(TextView(this).apply {
            this.text = if (text.isEmpty()) "Open the admin chat to reply." else "\"$text\""
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, pad)
        })

        fun btn(label: String, bg: Int, onClick: (View) -> Unit): Button {
            val b = Button(this)
            b.text = label
            b.textSize = 18f
            b.setTextColor(Color.WHITE)
            b.setBackgroundColor(bg)
            b.setOnClickListener(onClick)
            b.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = pad / 2 }
            root.addView(b)
            return b
        }

        btn("OPEN ADMIN CHAT", 0xFFd4a017.toInt()) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Net.ADMIN_URL)))
            stopEverything(ack = true)
        }
        btn("STOP ALARM", 0xFF8b0000.toInt()) {
            stopEverything(ack = true)
        }

        setContentView(root)

        // ring at a sane volume (65% of max) — full volume destroys speakers
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        try {
            origVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
            val target = (am.getStreamMaxVolume(AudioManager.STREAM_ALARM) * 0.55).toInt().coerceAtLeast(1)
            am.setStreamVolume(AudioManager.STREAM_ALARM, target, 0)
        } catch (_: Exception) { }
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val soundRes = Sfx.resId(Prefs.soundName(this))
        player = if (Build.VERSION.SDK_INT >= 26) {
            MediaPlayer.create(this, soundRes, attrs, 0).apply {
                isLooping = true
                start()
            }
        } else {
            val afd = resources.openRawResourceFd(soundRes)
            MediaPlayer().apply {
                setAudioAttributes(attrs)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                isLooping = true
                prepare()
                start()
            }
        }
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        val pattern = longArrayOf(0, 600, 600, 600)
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION") vibrator?.vibrate(pattern, -1)
        }

        silenceHandler = android.os.Handler(android.os.Looper.getMainLooper())

        // gentle ramp-in: 20% -> 100% over ~2.5s
        try {
            player?.setVolume(0.2f, 0.2f)
            var step = 0
            val ramp = object : Runnable {
                override fun run() {
                    step++
                    val v = (0.2f + 0.8f * (step / 12f)).coerceAtMost(1f)
                    try { player?.setVolume(v, v) } catch (_: Exception) { }
                    if (step < 12) silenceHandler?.postDelayed(this, 200)
                }
            }
            silenceHandler?.postDelayed(ramp, 200)
        } catch (_: Exception) { }

        // auto-silence after 2 minutes so it never screams forever
        silenceHandler?.postDelayed({
            try { player?.pause() } catch (_: Exception) { }
            try { vibrator?.cancel() } catch (_: Exception) { }
            findViewById<TextView>(android.R.id.text1)?.let { }
        }, 120_000L)
    }

    private fun stopEverything(ack: Boolean) {
        silenceHandler?.removeCallbacksAndMessages(null)
        try { player?.stop() } catch (_: Exception) { }
        try { player?.release() } catch (_: Exception) { }
        player = null
        vibrator?.cancel()
        if (origVolume >= 0) {
            try {
                val am = getSystemService(AUDIO_SERVICE) as AudioManager
                am.setStreamVolume(AudioManager.STREAM_ALARM, origVolume, 0)
            } catch (_: Exception) { }
            origVolume = -1
        }
        if (ack && alertId.isNotEmpty()) {
            Thread {
                try { Net.call(this, "ack", org.json.JSONObject().put("alert_id", alertId)) } catch (_: Exception) { }
            }.start()
        }
        finish()
    }

    override fun onBackPressed() { stopEverything(ack = true) }

    override fun onDestroy() {
        try { player?.release() } catch (_: Exception) { }
        super.onDestroy()
    }
}
