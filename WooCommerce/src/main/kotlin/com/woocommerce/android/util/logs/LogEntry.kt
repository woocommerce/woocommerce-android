package com.woocommerce.android.util.logs

import com.woocommerce.android.util.WooLog
import org.wordpress.android.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogEntry {
    val tag: WooLog.T
    val level: WooLog.LogLevel
    val text: String?

    val logDate: Date

    constructor(tag: WooLog.T, level: WooLog.LogLevel, text: String?) {
        this.tag = tag
        this.level = level
        this.text = text
        @Suppress("DEPRECATION")
        this.logDate = DateTimeUtils.nowUTC()
    }

    constructor(content: String) {
        val firstParts = content.removePrefix("[").substringBefore("]")
        val parts = firstParts.split(" ")

        logDate = SimpleDateFormat("MMM-dd kk:mm:ss:SSS", Locale.US).parse(parts[0] + " " + parts[1])
            ?: Date()
        tag = WooLog.T.valueOf(parts[2])
        level = WooLog.LogLevel.valueOf(parts[3])

        text = content.substringAfter("] ").takeIf { it.isNotEmpty() }?.trim()
    }

    override fun toString(): String {
        val logText = if (text.isNullOrEmpty()) "null" else text
        val logDateStr = SimpleDateFormat("MMM-dd kk:mm:ss:SSS", Locale.US).format(logDate)
        return "[$logDateStr ${tag.name} ${level.name}] $logText"
    }
}
