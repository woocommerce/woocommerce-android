package com.woocommerce.android.util

import android.util.Log
import com.woocommerce.android.util.WooLog.LogLevel
import com.woocommerce.android.util.WooLog.T
import org.wordpress.android.util.DateTimeUtils
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Locale
import java.util.NoSuchElementException

typealias LogListener = (T, LogLevel, String) -> Unit

/**
 * Simple wrapper for Android log calls, enables registering listeners for log events.
 *
 * Simplified version of [org.wordpress.android.util.AppLog].
 */
object WooLog {
    // T for Tag
    enum class T {
        DASHBOARD,
        ORDERS,
        UTILS,
        DEVICE
    }

    // Breaking convention to be consistent with org.wordpress.android.util.AppLog
    @Suppress("EnumEntryName")
    enum class LogLevel { v, d, i, w, e }

    const val TAG = "WooCommerce"
    private const val MAX_ENTRIES = 99
    private val logEntries = LogEntryList()
    private val listeners = ArrayList<LogListener>(0)

    private class LogEntry internal constructor(
        internal val logTag: T,
        internal val logLevel: LogLevel,
        logText: String?
    ) {
        internal val logText: String
        internal val logDate: java.util.Date = DateTimeUtils.nowUTC()

        init {
            if (logText == null) {
                this.logText = "null"
            } else {
                this.logText = logText
            }
        }

        private fun formatLogDate(): String {
            return SimpleDateFormat("MMM-dd kk:mm", Locale.US).format(logDate)
        }

        override fun toString(): String {
            val sb = StringBuilder()
            sb.append("[")
                    .append(formatLogDate()).append(" ")
                    .append(logTag.name).append(" ")
                    .append(logLevel.name)
                    .append("] ")
                    .append(logText)
            return sb.toString()
        }
    }

    private class LogEntryList : ArrayList<LogEntry>() {
        @Synchronized fun addEntry(entry: LogEntry): Boolean {
            if (size >= MAX_ENTRIES) {
                removeFirstEntry()
            }
            return add(entry)
        }

        private fun removeFirstEntry() {
            val it = iterator()
            if (it.hasNext()) {
                try {
                    remove(it.next())
                } catch (e: NoSuchElementException) {
                    // ignore
                }
            }
        }

        override fun toString(): String {
            val sb = StringBuilder()
            for (entry in logEntries) {
                sb.append((entry.toString()))
            }
            return sb.toString()
        }
    }

    fun addListener(listener: LogListener) {
        listeners.add(listener)
    }

    /**
     * Sends a VERBOSE log message
     * @param tag Used to identify the source of a log message.
     * It usually identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     */
    fun v(tag: T, message: String) {
        Log.v(TAG + "-" + tag.toString(), message)
        addEntry(tag, LogLevel.v, message)
    }

    /**
     * Sends a DEBUG log message
     * @param tag Used to identify the source of a log message.
     * It usually identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     */
    fun d(tag: T, message: String) {
        Log.d(TAG + "-" + tag.toString(), message)
        addEntry(tag, LogLevel.d, message)
    }

    /**
     * Sends a INFO log message
     * @param tag Used to identify the source of a log message.
     * It usually identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     */
    fun i(tag: T, message: String) {
        Log.i(TAG + "-" + tag.toString(), message)
        addEntry(tag, LogLevel.i, message)
    }

    /**
     * Sends a WARN log message
     * @param tag Used to identify the source of a log message.
     * It usually identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     */
    fun w(tag: T, message: String) {
        Log.w(TAG + "-" + tag.toString(), message)
        addEntry(tag, LogLevel.w, message)
    }

    /**
     * Sends a ERROR log message
     * @param tag Used to identify the source of a log message.
     * It usually identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     */
    fun e(tag: T, message: String) {
        Log.e(TAG + "-" + tag.toString(), message)
        addEntry(tag, LogLevel.e, message)
    }

    /**
     * Send a ERROR log message and log the exception.
     * @param tag Used to identify the source of a log message.
     * It usually identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     * @param tr An exception to log
     */
    fun e(tag: T, message: String, tr: Throwable) {
        Log.e(TAG + "-" + tag.toString(), message, tr)
        addEntry(tag, LogLevel.e, message + " - exception: " + tr.message)
        addEntry(tag, LogLevel.e, "StackTrace: " + getStringStackTrace(tr))
    }

    /**
     * Sends a ERROR log message and the exception with StackTrace
     * @param tag Used to identify the source of a log message. It usually identifies the class or activity where the
     * log call occurs.
     * @param tr An exception to log to get StackTrace
     */
    fun e(tag: T, tr: Throwable) {
        Log.e(TAG + "-" + tag.toString(), tr.message, tr)
        addEntry(tag, LogLevel.e, tr.message ?: "")
        addEntry(tag, LogLevel.e, "StackTrace: " + getStringStackTrace(tr))
    }

    /**
     * Sends a ERROR log message
     * @param tag Used to identify the source of a log message. It usually identifies the class or activity where the
     * log call occurs.
     * @param volleyErrorMsg
     * @param statusCode
     */
    fun e(tag: T, volleyErrorMsg: String, statusCode: Int) {
        if (volleyErrorMsg.isEmpty()) return

        val logText: String = if (statusCode == -1) {
            volleyErrorMsg
        } else {
            "$volleyErrorMsg, status $statusCode"
        }
        Log.e(TAG + "-" + tag.toString(), logText)
        addEntry(tag, LogLevel.w, logText)
    }

    private fun addEntry(tag: T, level: LogLevel, text: String) {
        // Call our listeners if any
        for (listener in listeners) {
            listener(tag, level, text)
        }

        // add to log entry list
        val entry = LogEntry(tag, level, text)
        logEntries.addEntry(entry)
    }

    private fun getStringStackTrace(throwable: Throwable): String {
        val errors = StringWriter()
        throwable.printStackTrace(PrintWriter(errors))
        return errors.toString()
    }

    override fun toString() = logEntries.toString()
}
