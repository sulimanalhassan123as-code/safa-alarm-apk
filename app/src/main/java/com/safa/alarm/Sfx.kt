package com.safa.alarm

object Sfx {
    val OPTIONS = listOf(
        Triple("bells", "Soft Bells", "gentle two-note bell"),
        Triple("ring", "Gentle Ring", "warm old-phone ring"),
        Triple("chime", "Soft Chime", "rising musical chime")
    )

    fun resId(name: String): Int = when (name) {
        "ring" -> R.raw.ring
        "chime" -> R.raw.chime
        else -> R.raw.bells
    }

    fun displayName(name: String): String =
        OPTIONS.firstOrNull { it.first == name }?.second ?: "Soft Bells"
}
