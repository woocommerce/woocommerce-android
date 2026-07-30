package com.woocommerce.android.util.logs

import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Bundles every stored log file into a single zip archive so all of them can be shared at once.
 *
 * The archive is rebuilt on each call and kept outside the logs directory, so it is never picked up
 * by [LogFileWriter]'s rotation.
 */
class LogFilesArchiver(
    private val archiveDirectory: File,
    private val dispatchers: CoroutineDispatchers,
) {
    suspend fun archive(logFiles: List<File>, deviceInfo: String): File? {
        if (logFiles.isEmpty()) return null

        return withContext(dispatchers.io) {
            runCatching {
                if (!archiveDirectory.exists()) {
                    archiveDirectory.mkdirs()
                }

                val archive = File(archiveDirectory, ARCHIVE_FILE_NAME)
                ZipOutputStream(archive.outputStream().buffered()).use { output ->
                    logFiles.forEach { logFile ->
                        output.putNextEntry(ZipEntry(logFile.name))
                        logFile.inputStream().buffered().use { it.copyTo(output) }
                        output.closeEntry()
                    }

                    output.putNextEntry(ZipEntry(DEVICE_INFO_FILE_NAME))
                    output.write(deviceInfo.toByteArray())
                    output.closeEntry()
                }
                archive
            }.onFailure {
                WooLog.e(WooLog.T.UTILS, "Could not create the log archive", it)
            }.getOrNull()
        }
    }

    companion object {
        const val ARCHIVE_FILE_NAME = "woocommerce-logs.zip"
        const val DEVICE_INFO_FILE_NAME = "device_info.txt"
    }
}
