package com.woocommerce.android.util.logs

import com.woocommerce.android.util.WooLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogEntry(
    val tag: WooLog.T,
    val level: WooLog.LogLevel,
    val text: String?,
    private val logDate: Date = Date()
) {
    override fun toString(): String {
        val logText = if (text.isNullOrEmpty()) "null" else text
        val logDateStr = formatter.format(logDate)
        return "[$logDateStr ${tag.name} ${level.name}] $logText"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LogEntry) return false

        if (tag != other.tag) return false
        if (level != other.level) return false
        if (text != other.text) return false
        if (formatter.format(logDate) != formatter.format(other.logDate)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tag.hashCode()
        result = 31 * result + level.hashCode()
        result = 31 * result + (text?.hashCode() ?: 0)
        result = 31 * result + formatter.format(logDate).hashCode()
        return result
    }

    companion object {
        private val formatter
            get() = SimpleDateFormat("MMM-dd kk:mm:ss:SSS", Locale.US)

        @Suppress("MagicNumber")
        fun fromString(log: String): LogEntry? {
            return runCatching {
                val firstParts = log.removePrefix("[").substringBefore("]")
                val parts = firstParts.split(" ")

                val logDate = formatter.parse(parts[0] + " " + parts[1])
                    ?: Date()
                val tagName = migrateDeprecatedTagNames(parts[2])
                val tag = WooLog.T.valueOf(tagName)
                val level = WooLog.LogLevel.valueOf(parts[3])

                val text = log.substringAfter("] ").takeIf { it.isNotEmpty() }?.trim()

                LogEntry(
                    tag = tag,
                    level = level,
                    text = text,
                    logDate = logDate
                )
            }.getOrNull()
        }

        // TODO WOOMOB-2212 remove this migration code after a couple of releases,
        //  logs are only kept for one week so the issue should fix itself by then.
        private val deprecatedTagNames = mapOf(
            "NOTIFS" to WooLog.T.NOTIFICATIONS.name
        )

        private fun migrateDeprecatedTagNames(tagName: String): String {
            return deprecatedTagNames[tagName] ?: tagName
        }
    }
}
