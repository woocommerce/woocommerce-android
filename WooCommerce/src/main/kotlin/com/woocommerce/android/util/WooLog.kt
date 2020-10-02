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
import org.wordpress.android.util.AppLog as WordPressAppLog

typealias LogListener = (T, LogLevel, String) -> Unit

/**
 * Simple wrapper for Android log calls, enables registering listeners for log events.
 *
 * Simplified version of [org.wordpress.android.util.AppLog].
 */
object WooLog {
    // T for Tag
    enum class T {
        MY_STORE,
        ORDERS,
        PRODUCTS,
        NOTIFS,
        UTILS,
        DEVICE,
        SUPPORT,
        WP,
        NOTIFICATIONS,
        LOGIN,
        REVIEWS,
        MEDIA
    }

    // Breaking convention to be consistent with org.wordpress.android.util.AppLog
    @Suppress("EnumEntryName")
    enum class LogLevel { v, d, i, w, e }

    const val TAG = "WooCommerce"
    private const val MAX_ENTRIES = 99
    private val logEntries = LogEntryList()
    private val listeners = ArrayList<LogListener>(0)

    init {
        // add listener for WP app log so we can capture login & FluxC logs
        WordPressAppLog.addListener { tag, logLevel, message ->
            addWPLogEntry(tag, logLevel, message)
        }
    }

    private fun addWPLogEntry(wpTag: WordPressAppLog.T, wpLogLevel: WordPressAppLog.LogLevel, wpMessage: String) {
        val wooLogLevel = when (wpLogLevel) {
            WordPressAppLog.LogLevel.v -> LogLevel.v
            WordPressAppLog.LogLevel.d -> LogLevel.d
            WordPressAppLog.LogLevel.i -> LogLevel.i
            WordPressAppLog.LogLevel.w -> LogLevel.w
            WordPressAppLog.LogLevel.e -> LogLevel.e
        }

        addEntry(T.WP, wooLogLevel, wpTag.name + " " + wpMessage)
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
        Log.v("$TAG-$tag", message)
        addEntry(tag, LogLevel.v, message)
    }

    /**
     * Sends a DEBUG log message
     * @param tag Used to identify the source of a log message.
     * It usually identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     */
    fun d(tag: T, message: String) {
        Log.d("$TAG-$tag", message)
        addEntry(tag, LogLevel.d, message)
    }

    /**
     * Sends a INFO log message
     * @param tag Used to identify the source of a log message.
     * It usually identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     */
    fun i(tag: T, message: String) {
        Log.i("$TAG-$tag", message)
        addEntry(tag, LogLevel.i, message)
    }

    /**
     * Sends a WARN log message
     * @param tag Used to identify the source of a log message.
     * It usually identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     */
    fun w(tag: T, message: String) {
        Log.w("$TAG-$tag", message)
        addEntry(tag, LogLevel.w, message)
    }

    /**
     * Sends a ERROR log message
     * @param tag Used to identify the source of a log message.
     * It usually identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     */
    fun e(tag: T, message: String) {
        Log.e("$TAG-$tag", message)
        addEntry(tag, LogLevel.e, message)
    }

    /**
     * Send a ERROR log message and log the exception.
     * @param tag Used to identify the source of a log message.
     * It usually identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     * @param tr An exception to log
     */
    fun e(tag: T, message: String, tr: Throwable?) {
        tr?.let { throwable ->
            Log.e("$TAG-$tag", message, throwable)
            addEntry(tag, LogLevel.e, message + " - exception: " + throwable.message)
            addEntry(tag, LogLevel.e, "StackTrace: " + getStringStackTrace(throwable))
        } ?: e(tag, message)
    }

    /**
     * Sends a ERROR log message and the exception with StackTrace
     * @param tag Used to identify the source of a log message. It usually identifies the class or activity where the
     * log call occurs.
     * @param tr An exception to log to get StackTrace
     */
    fun e(tag: T, tr: Throwable) {
        Log.e("$TAG-$tag", tr.message, tr)
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
        Log.e("$TAG-$tag", logText)
        addEntry(tag, LogLevel.w, logText)
    }

    private fun addEntry(tag: T, level: LogLevel, text: String) {
        // add to list of entries
        val entry = LogEntry(tag, level, text)
        logEntries.addEntry(entry)

        // Call our listeners if any
        for (listener in listeners) {
            listener(tag, level, text)
        }
    }

    private fun getStringStackTrace(throwable: Throwable): String {
        val errors = StringWriter()
        throwable.printStackTrace(PrintWriter(errors))
        return errors.toString()
    }

    fun toHtmlList(isDarkTheme: Boolean) = logEntries.toHtmlList(isDarkTheme)

    override fun toString() = logEntries.toString()

    /**
     * Individual log entry
     */
    private class LogEntry internal constructor(
        internal val tag: T,
        internal val level: LogLevel,
        internal val text: String?
    ) {
        internal val logDate: java.util.Date = DateTimeUtils.nowUTC()

        override fun toString(): String {
            val logText = if (text.isNullOrEmpty()) "null" else text
            val logDateStr = SimpleDateFormat("MMM-dd kk:mm", Locale.US).format(logDate)
            return "[$logDateStr ${tag.name} ${level.name}] $logText"
        }
    }

    /**
     * Fix-sized list of log entries
     */
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

        /**
         * Returns the log entries as an array of html-formatted strings - this enables us to display
         * a formatted log in [com.woocommerce.android.support.WooLogViewerActivity]
         */
        fun toHtmlList(isDarkTheme: Boolean): ArrayList<String> {
            val list = ArrayList<String>()
            // work with a copy of the log entries in case they're modified while traversing them
            val entries = mutableListOf<LogEntry>().also { it.addAll(this) }
            for (entry in entries) {
                // same colors as WPAndroid
                val color = if (isDarkTheme) {
                    "white"
                } else when (entry.level) {
                    LogLevel.v -> "grey"
                    LogLevel.d -> "teal"
                    LogLevel.i -> "black"
                    LogLevel.w -> "purple"
                    LogLevel.e -> "red"
                }
                list.add("<font color='$color'>$entry</font>")
            }
            return list
        }

        /**
         * Returns the log entries as a single string with each entry on a new line
         */
        override fun toString(): String {
            val sb = StringBuilder()
            val entries = mutableListOf<LogEntry>().also { it.addAll(this) }
            for (entry in entries) {
                sb.append("${entry}\n")
            }
            return sb.toString()
        }
    }
}
