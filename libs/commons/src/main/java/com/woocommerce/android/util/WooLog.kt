package com.woocommerce.android.util

import android.util.Log
import com.woocommerce.android.util.logs.LogEntry
import com.woocommerce.android.util.logs.WooFileLogger
import java.io.PrintWriter
import java.io.StringWriter
import org.wordpress.android.util.AppLog as WordPressAppLog

/**
 * Simple wrapper for Android log calls, enables registering listeners for log events.
 *
 * Simplified version of [org.wordpress.android.util.AppLog].
 *
 * Note: If you want to reference these enum values in tests, you must duplicate them here: WooPosProductsDataSourceTest.kt
 */
object WooLog {
    // T for Tag
    enum class T {
        DASHBOARD,
        ORDERS,
        PRODUCTS,
        NOTIFICATIONS,
        UTILS,
        DEVICE,
        SUPPORT,
        WP,
        LOGIN,
        REVIEWS,
        MEDIA,
        CARD_READER,
        SITE_PICKER,
        COUPONS,
        JITM,
        PLUGINS,
        IAP,
        STORE_CREATION,
        ONBOARDING,
        WOO_TRIAL,
        AI,
        BARCODE_SCANNER,
        THEMES,
        BLAZE,
        GOOGLE_ADS,
        POS,
        CUSTOM_FIELDS,
        SHIPPING_LABELS
    }

    // Breaking convention to be consistent with org.wordpress.android.util.AppLog
    @Suppress("EnumEntryName")
    enum class LogLevel { v, d, i, w, e }

    const val TAG = "WooCommerce"

    private val tempListBeforeInit = mutableListOf<LogEntry>()

    private lateinit var fileLogger: WooFileLogger

    fun init(fileLogger: WooFileLogger) {
        // add listener for WP app log so we can capture login & FluxC logs
        WordPressAppLog.addListener { tag, logLevel, message ->
            addWPLogEntry(tag, logLevel, message)
        }

        this.fileLogger = fileLogger
        tempListBeforeInit.forEach { entry ->
            fileLogger.addEntry(entry)
        }
        tempListBeforeInit.clear()
    }

    suspend fun getCurrentLogEntries(): List<LogEntry> {
        fileLogger.forceFlush()
        return fileLogger.getCurrentLogFileContent()
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
        val entry = LogEntry(tag, level, text)
        if (::fileLogger.isInitialized) {
            fileLogger.addEntry(entry)
        } else {
            // If WooLog is called before initialization, store the entry temporarily
            // Can happen when invoking WooLog from ContentProviders
            tempListBeforeInit.add(entry)
        }
    }

    private fun getStringStackTrace(throwable: Throwable): String {
        val errors = StringWriter()
        throwable.printStackTrace(PrintWriter(errors))
        return errors.toString()
    }
}
