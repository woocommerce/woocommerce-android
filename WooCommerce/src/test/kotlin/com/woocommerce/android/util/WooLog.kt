package com.woocommerce.android.util

import com.woocommerce.android.util.WooLog.LogLevel
import com.woocommerce.android.util.WooLog.T
import java.io.PrintWriter
import java.io.StringWriter
import java.util.ArrayList

typealias LogListener = (T, LogLevel, String) -> Unit

/**
 * Simple wrapper for Android log calls, enables registering listeners for log events.
 *
 * Testing version: replaces android.util.Log calls with System.out, since the unit tests don't have access
 * to Android Framework classes.
 */
object WooLog {
    // T for Tag
    enum class T {
        DASHBOARD,
        ORDERS,
        UTILS
    }

    // Breaking convention to be consistent with org.wordpress.android.util.AppLog
    @Suppress("EnumEntryName")
    enum class LogLevel { v, d, i, w, e }

    const val TAG = "WooCommerce"
    private val listeners = ArrayList<LogListener>(0)

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
        System.out.println("v - $TAG-$tag - $message")
        addEntry(tag, LogLevel.v, message)
    }

    /**
     * Sends a DEBUG log message
     * @param tag Used to identify the source of a log message.
     * It usually identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     */
    fun d(tag: T, message: String) {
        System.out.println("d - $TAG-$tag - $message")
        addEntry(tag, LogLevel.d, message)
    }

    /**
     * Sends a INFO log message
     * @param tag Used to identify the source of a log message.
     * It usually identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     */
    fun i(tag: T, message: String) {
        System.out.println("i - $TAG-$tag - $message")
        addEntry(tag, LogLevel.i, message)
    }

    /**
     * Sends a WARN log message
     * @param tag Used to identify the source of a log message.
     * It usually identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     */
    fun w(tag: T, message: String) {
        System.out.println("w - $TAG-$tag - $message")
        addEntry(tag, LogLevel.w, message)
    }

    /**
     * Sends a ERROR log message
     * @param tag Used to identify the source of a log message.
     * It usually identifies the class or activity where the log call occurs.
     * @param message The message you would like logged.
     */
    fun e(tag: T, message: String) {
        System.out.println("e - $TAG-$tag - $message")
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
        System.out.println("e - $TAG-$tag - $message")
        System.out.println(tr)
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
        System.out.println("e - " + TAG + "-" + tag.toString() + " - " + tr.message)
        System.out.println(tr)
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
        System.out.println("e - $TAG-$tag - $logText")
        addEntry(tag, LogLevel.w, logText)
    }

    private fun addEntry(tag: T, level: LogLevel, text: String) {
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
}
