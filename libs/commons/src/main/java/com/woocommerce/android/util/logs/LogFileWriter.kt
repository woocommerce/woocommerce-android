package com.woocommerce.android.util.logs

import android.os.StatFs
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

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
    private val dispatchers: CoroutineDispatchers,
    private val availableDiskBytes: () -> Long = {
        if (!logsDirectory.exists()) {
            logsDirectory.mkdirs()
        }
        runCatching { StatFs(logsDirectory.absolutePath).availableBytes }.getOrDefault(0L)
    }
) {
    private var lastUsedFile: File? = null
    private val dateFormatter
        get() = SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.ROOT)
    private val mutex = Mutex()

    private val logFiles: Array<File>
        get() = logsDirectory.listFiles { file -> file.isFile && file.name.startsWith(LOG_FILE_NAME_PREFIX) }
            ?: emptyArray()

    @Volatile
    private var cachedDiskSpace: Long = Long.MAX_VALUE

    @Volatile
    private var lastDiskSpaceCheckTime: Long = 0L

    suspend fun writeLogs(logs: String) {
        val logFile = getLogFile()

        if (!hasEnoughDiskSpace()) {
            deleteOldestLogFiles()
            return
        }

        withContext(dispatchers.io) {
            mutex.withLock {
                if (logFile.length() >= MAX_LOG_FILE_SIZE_BYTES) {
                    logFile.delete()
                    logFile.createNewFile()
                }
                logFile.appendText("$logs\n")
            }
        }
    }

    suspend fun getCurrentLogFile(): File {
        return getLogFile()
    }

    suspend fun readFileContent(fileName: String): String? {
        ensureDirectoryExists()
        return withContext(dispatchers.io) {
            mutex.withLock {
                logFiles.find { it.name == fileName }
                    ?.readText()
            }
        }
    }

    suspend fun getLogFiles(): List<File> {
        ensureDirectoryExists()
        return withContext(dispatchers.io) {
            logFiles.sortedByDescending { it.lastModified() }
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
                mutex.withLock {
                    if (!file.exists()) {
                        file.createNewFile()
                    }
                }
            }
            return file
        }

        val today = dateFormatter.format(java.util.Date())
        val logFileName = "$LOG_FILE_NAME_PREFIX$today.txt"

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

    private suspend fun rotateLogFilesIfNeeded() {
        withContext(dispatchers.io) {
            val sortedLogFiles = logFiles.sortedByDescending { it.lastModified() }

            mutex.withLock {
                if (sortedLogFiles.size > maxLogFiles) {
                    sortedLogFiles.takeLast(sortedLogFiles.size - maxLogFiles).forEach { file ->
                        file.delete()
                    }
                }
            }
        }
    }

    private suspend fun hasEnoughDiskSpace(): Boolean {
        return withContext(dispatchers.io) {
            val now = System.currentTimeMillis()
            if (now - lastDiskSpaceCheckTime > DISK_SPACE_CHECK_INTERVAL_MS) {
                cachedDiskSpace = availableDiskBytes()
                lastDiskSpaceCheckTime = now
            }
            cachedDiskSpace >= MIN_DISK_SPACE_BYTES
        }
    }

    private suspend fun deleteOldestLogFiles() {
        withContext(dispatchers.io) {
            mutex.withLock {
                val sortedLogFiles = logFiles.sortedByDescending { it.lastModified() }

                if (sortedLogFiles.size > MIN_LOG_FILES_TO_KEEP) {
                    sortedLogFiles.drop(MIN_LOG_FILES_TO_KEEP).forEach { it.delete() }
                }
            }
        }
    }

    companion object {
        const val LOG_FILE_NAME_PREFIX = "log_"
        const val DATE_FORMAT_PATTERN = "yyyy-MM-dd"

        private const val MAX_LOG_FILE_SIZE_BYTES = 2L * 1024 * 1024
        private const val MIN_DISK_SPACE_BYTES = 10L * 1024 * 1024
        private const val DISK_SPACE_CHECK_INTERVAL_MS = 5_000L
        private const val MIN_LOG_FILES_TO_KEEP = 2
    }
}
