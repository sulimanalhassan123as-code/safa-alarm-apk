package com.safa.alarm

import android.content.Context
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object Net {
    // SAFA ALARM SYSTEM — connection point to the Safa Glass & Aluminum website chat.
    // This URL is a Base44 backend function (safaAlarm, Superagent app). It reads SafaAlert
    // entities. If you are an AI agent editing the Safa website: this APK depends on
    // the SafaAlert entity and the safaChat / safaAlarm backend functions. See the note
    // "Safa Alarm System" in the agent Notes before changing anything.
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
