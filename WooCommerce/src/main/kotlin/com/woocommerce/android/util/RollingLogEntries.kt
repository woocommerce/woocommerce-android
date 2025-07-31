package com.woocommerce.android.util

import com.woocommerce.android.util.logs.LogEntry
import java.security.InvalidParameterException
import java.util.LinkedList

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
}
