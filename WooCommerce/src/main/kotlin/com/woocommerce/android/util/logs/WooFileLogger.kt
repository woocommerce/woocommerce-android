package com.woocommerce.android.util.logs

import android.content.Context
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.RollingLogEntries
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.util.helpers.logfile.LogFileCleaner
import org.wordpress.android.util.helpers.logfile.LogFileProvider
import org.wordpress.android.util.helpers.logfile.LogFileWriter
import java.io.File
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

    init {
        appCoroutineScope.launch(dispatchers.io) {
            logFileCleaner.clean()
        }
    }

    fun addEntry(logEntry: RollingLogEntries.LogEntry) {
        logFileWriter.write("${LOG_ENTRY_PREFIX}$logEntry\n")
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
                logFileProvider.getLogFiles().find { it.name == fileName }?.readText()?.let {
                    it.split("\n${LOG_ENTRY_PREFIX}").map {
                        RollingLogEntries.LogEntry(it.removePrefix(LOG_ENTRY_PREFIX))
                    }
                }
            }.onFailure {
                it.printStackTrace()
            }.getOrNull()
        }
    }

    companion object Companion {
        private const val MAX_LOG_FILES = 7
        private const val LOG_ENTRY_PREFIX = "--"
    }
}
