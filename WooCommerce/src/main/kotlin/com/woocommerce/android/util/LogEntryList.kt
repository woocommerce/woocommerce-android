package com.woocommerce.android.util

import com.woocommerce.android.util.LogEntryList.LogEntry
import com.woocommerce.android.util.WooLog.LogLevel
import com.woocommerce.android.util.WooLog.LogLevel.d
import com.woocommerce.android.util.WooLog.LogLevel.e
import com.woocommerce.android.util.WooLog.LogLevel.i
import com.woocommerce.android.util.WooLog.LogLevel.v
import com.woocommerce.android.util.WooLog.LogLevel.w
import com.woocommerce.android.util.WooLog.T
import org.wordpress.android.util.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fix-sized list of log entries
 */
class LogEntryList(private val maxEntries: Int) : LinkedList<LogEntry>() {
    @Synchronized
    override fun add(element: LogEntry): Boolean {
        if (size == maxEntries) {
            removeFirst()
        }
        return super.add(element)
    }

    /**
     * Returns the log entries as an array of html-formatted strings - this enables us to display
     * a formatted log in [com.woocommerce.android.support.WooLogViewerActivity]
     */
    fun toHtmlList(isDarkTheme: Boolean): List<String> {
        fun LogEntry.getColor(): String {
            return if (isDarkTheme) {
                "white"
            } else when (level) {
                v -> "grey"
                d -> "teal"
                i -> "black"
                w -> "purple"
                e -> "red"
            }
        }

        // work with a copy of the log entries in case they're modified while traversing them
        return toList().map { entry ->
            "<font color='${entry.getColor()}'>$entry</font>"
        }
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
        private val logDate: Date = DateTimeUtils.nowUTC()

        override fun toString(): String {
            val logText = if (text.isNullOrEmpty()) "null" else text
            val logDateStr = SimpleDateFormat("MMM-dd kk:mm", Locale.US).format(logDate)
            return "[$logDateStr ${tag.name} ${level.name}] $logText"
        }
    }
}
