package net.kustax.opendelivery.web.util

fun String.toLabel(): String =
    split("_").joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }

fun formatTimestamp(epochMs: Long): String {
    val s = epochMs / 1000L
    val y = 1970 + (s / 31557600).toInt()
    val remaining = s % 31557600
    val mon = (remaining / 2629800).toInt() + 1
    val d = ((remaining % 2629800) / 86400).toInt() + 1
    val h = ((s % 86400) / 3600).toInt()
    val m = ((s % 3600) / 60).toInt()
    return "$y-${mon.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')} ${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
}
