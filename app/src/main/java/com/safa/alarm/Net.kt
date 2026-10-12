package com.safa.alarm

import android.content.Context
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object Net {
    // SAFA ALARM SYSTEM — DO NOT CHANGE THIS ENDPOINT (Oct 12, 2026, owner-confirmed).
    // The ORIGINAL alarm chain is official: the Safa website writes alerts on the Zola app
    // (zola-0cd423e5) and its safaAlarm function MIRRORS every alert here to the ORIGINAL
    // endpoint below. This APK polls here every 20s. Do not repoint this APK, do not build
    // new alarm bots or new alarm workflows — the original chain handles everything.
    // GitHub Actions builds are blocked (account billing lock), so the installed APK is the
    // only build that matters. See README.md before touching anything.
    const val ENDPOINT = "https://superagent-b0de149e.base44.app/functions/safaAlarm"
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
