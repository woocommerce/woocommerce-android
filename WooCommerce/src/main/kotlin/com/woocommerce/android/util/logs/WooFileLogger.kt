package com.woocommerce.android.util.logs

import android.content.Context
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.RollingLogEntries
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.util.helpers.logfile.LogFileCleaner
import org.wordpress.android.util.helpers.logfile.LogFileProvider
import org.wordpress.android.util.helpers.logfile.LogFileWriter
import java.io.File
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooFileLogger @Inject constructor(
    context: Context,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
    private val dispatchers: CoroutineDispatchers
) {
    private val logFileProvider = LogFileProvider.fromContext(context)
    private val logFileWriter = LogFileWriter(logFileProvider)
    private val logFileCleaner = LogFileCleaner(logFileProvider, MAX_LOG_FILES)

    private val logEntryConverter = LogEntryConverter()

    init {
        appCoroutineScope.launch(dispatchers.io) {
            logFileCleaner.clean()
        }
    }

    fun addEntry(logEntry: RollingLogEntries.LogEntry) {
        logFileWriter.write(logEntryConverter.toString(logEntry) + "\n")
    }

    suspend fun getLogFiles(): List<String> {
        return withContext(dispatchers.io) {
            logFileProvider.getLogFiles().map { it.name }
        }
    }

    fun getCurrentLogFile(): File = logFileProvider.getLogFiles().last()

    suspend fun getCurrentLogFileContent(): List<RollingLogEntries.LogEntry> =
        getLogFileContent(getCurrentLogFile().name).orEmpty()

    suspend fun getLogFileContent(fileName: String): List<RollingLogEntries.LogEntry>? {
        return withContext(dispatchers.io) {
            runCatching {
                logFileProvider.getLogFiles().find { it.name == fileName }?.readLines()?.map {
                    logEntryConverter.fromString(it)
                }
            }.onFailure {
                it.printStackTrace()
            }.getOrNull()
        }
    }

    companion object Companion {
        private const val MAX_LOG_FILES = 7
    }
}

private class LogEntryConverter {
    fun fromString(logEntry: String): RollingLogEntries.LogEntry {
        val parts = logEntry.split(" ", limit = 4)

        val timestamp = parts[0].toLongOrNull()
        val tag = WooLog.T.valueOf(parts[1])
        val level = WooLog.LogLevel.valueOf(parts[2])
        val text = if (parts.size > 3) parts[3].replace("\\n", "\n") else null

        return RollingLogEntries.LogEntry(
            tag = tag,
            level = level,
            text = text,
            logDate = Date(timestamp ?: System.currentTimeMillis())
        )
    }

    fun toString(logEntry: RollingLogEntries.LogEntry): String {
        return "${logEntry.logDate.time} ${logEntry.tag.name} ${logEntry.level.name} " +
            logEntry.text?.replace("\n", "\\n").orEmpty()
    }
}
