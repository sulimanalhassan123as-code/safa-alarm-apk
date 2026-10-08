package com.safa.alarm

import android.content.Context

object Prefs {
    private const val FILE = "safa_alarm"
    private fun c(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun deviceKey(ctx: Context): String = c(ctx).getString("device_key", "") ?: ""
    fun setDeviceKey(ctx: Context, k: String) { c(ctx).edit().putString("device_key", k).apply() }

    fun seen(ctx: Context): MutableSet<String> = c(ctx).getStringSet("seen", HashSet()) ?: HashSet()
    fun markSeen(ctx: Context, id: String) {
        val s = HashSet(seen(ctx)); s.add(id); c(ctx).edit().putStringSet("seen", s).apply()
    }
    fun firstRun(ctx: Context): Boolean = c(ctx).getBoolean("first_run", true)
    fun setFirstRun(ctx: Context, b: Boolean) { c(ctx).edit().putBoolean("first_run", b).apply() }

    fun soundName(ctx: Context): String = c(ctx).getString("sound_name", "bells") ?: "bells"
    fun setSoundName(ctx: Context, n: String) { c(ctx).edit().putString("sound_name", n).apply() }
}
