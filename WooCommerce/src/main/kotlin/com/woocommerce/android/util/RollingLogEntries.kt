package com.woocommerce.android.util

import com.woocommerce.android.util.RollingLogEntries.LogEntry
import com.woocommerce.android.util.WooLog.LogLevel
import com.woocommerce.android.util.WooLog.T
import org.wordpress.android.util.DateTimeUtils
import java.security.InvalidParameterException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedList
import java.util.Locale

/**
 * Fix-sized list of log entries
 */
class RollingLogEntries(private val limit: Int) : LinkedList<LogEntry>() {
    init {
        if (limit <= 0) throw InvalidParameterException("The limit must be greater than 0")
    }

    @Synchronized
    override fun add(element: LogEntry): Boolean {
        if (size == limit) {
            removeFirst()
        }
        return super.add(element)
    }

    /**
     * Returns the log entries as a single string with each entry on a new line. Works with a copy of the log
     * entries in case they're modified while traversing them.
     */
    override fun toString() = toList().joinToString("\n")

    /**
     * Individual log entry
     */
    class LogEntry(
        val tag: T,
        val level: LogLevel,
        val text: String?
    ) {
        @Suppress("DEPRECATION")
        private val logDate: Date = DateTimeUtils.nowUTC()

        override fun toString(): String {
            val logText = if (text.isNullOrEmpty()) "null" else text
            val logDateStr = SimpleDateFormat("MMM-dd kk:mm:ss:SSS", Locale.US).format(logDate)
            return "[$logDateStr ${tag.name} ${level.name}] $logText"
        }
    }
}
