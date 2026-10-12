package com.safa.alarm

import android.content.Context
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object Net {
    // SAFA ALARM SYSTEM — connection point to the Safa Glass & Aluminum website chat.
    // Backend moved home to the Zola app (zola-0cd423e5) on Oct 8, 2026 — this APK was
    // repointed accordingly. The safaAlarm function there implements the full protocol:
    // register / poll / ack / test (secret 'safa-alarm-2026') + alert (public, website).
    // If you are an AI agent editing the Safa system: do NOT change this URL without
    // also updating the SafaAlert / SafaDevice entities and the Safa Owner Alarm workflow.
    const val ENDPOINT = "https://zola-0cd423e5.base44.app/functions/safaAlarm"
    val SECRET: String get() = BuildConfig.SECRET
    const val ADMIN_URL = "https://safa-glass-admin.vercel.app"

    fun call(ctx: Context, action: String, extra: JSONObject? = null): JSONObject {
        val body = JSONObject()
        body.put("action", action)
        body.put("device_key", Prefs.deviceKey(ctx))
        body.put("secret", SECRET)
        if (extra != null) for (k in extra.keys()) body.put(k, extra.get(k))
        val conn = URL(ENDPOINT).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
        val code = conn.responseCode
        val txt = (if (code in 200..299) conn.inputStream else conn.errorStream)
            .bufferedReader().use { it.readText() }
        conn.disconnect()
        return if (txt.isEmpty()) JSONObject() else JSONObject(txt)
    }
}
