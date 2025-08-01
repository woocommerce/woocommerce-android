package com.woocommerce.android.util.logs

import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * A simple utility class for writing logs to files.
 *
 * It creates a new log file for each day and rotates log files when the number of files exceeds a specified limit.
 * The log files are stored in a specified directory.
 * The log file names are prefixed with "log_" followed by the date in "yyyy-MM-dd" format.
 */
class LogFileWriter(
    private val logsDirectory: File,
    private val maxLogFiles: Int,
    private val dispatchers: CoroutineDispatchers
) {
    private var lastUsedFile: File? = null
    private val dateFormatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)

    suspend fun writeLogs(logs: String) {
        val logFile = getLogFile()
        withContext(dispatchers.io) {
            logFile.appendText("$logs\n")
        }
    }

    suspend fun getCurrentLogFile(): File {
        return getLogFile()
    }

    suspend fun getLogFiles(): List<File> {
        ensureDirectoryExists()
        return withContext(dispatchers.io) {
            logsDirectory.listFiles { file -> file.isFile && file.name.startsWith("log_") }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        }
    }

    private suspend fun ensureDirectoryExists() {
        withContext(dispatchers.io) {
            if (!logsDirectory.exists()) {
                logsDirectory.mkdirs()
            }
        }
    }

    private suspend fun getLogFile(): File {
        suspend fun ensureFileExists(file: File): File {
            ensureDirectoryExists()
            withContext(dispatchers.io) {
                if (!file.exists()) {
                    file.createNewFile()
                }
            }
            return file
        }

        val today = dateFormatter.format(java.util.Date())
        val logFileName = "log_$today.txt"

        lastUsedFile?.let {
            if (it.name == logFileName) {
                ensureFileExists(it)
                return it
            }
        }

        val logFile = File(logsDirectory, logFileName)
        ensureFileExists(logFile)
        lastUsedFile = logFile

        rotateLogFilesIfNeeded()

        return logFile
    }

    private fun rotateLogFilesIfNeeded() {
        val logFiles = logsDirectory.listFiles { file -> file.isFile && file.name.startsWith("log_") }
            ?.sortedByDescending { it.lastModified() } ?: return

        if (logFiles.size > maxLogFiles) {
            logFiles.takeLast(logFiles.size - maxLogFiles).forEach { file ->
                file.delete()
            }
        }
    }
}
