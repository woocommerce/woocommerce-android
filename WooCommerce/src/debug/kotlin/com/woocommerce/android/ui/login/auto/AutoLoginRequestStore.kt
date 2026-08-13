package com.woocommerce.android.ui.login.auto

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AutoLoginRequestStore internal constructor(
    private val rootDirectory: File,
    private val parser: AutoLoginRequestParser
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        parser: AutoLoginRequestParser
    ) : this(
        rootDirectory = File(context.noBackupFilesDir, ROOT_PATH),
        parser = parser
    )

    fun consume(): AutoLoginRequestParseResult {
        val readyFile = File(rootDirectory, REQUEST_READY_FILE)
        val bytes = try {
            readyFile
                .takeIf { it.isFile && it.length() in 1..MAX_PAYLOAD_BYTES.toLong() }
                ?.readBytes()
                ?.takeIf { it.size <= MAX_PAYLOAD_BYTES }
        } catch (_: Exception) {
            null
        }
        val deleted = readyFile.delete() || !readyFile.exists()
        if (bytes == null || !deleted) return AutoLoginRequestParseResult.Invalid

        return parser.parse(String(bytes, Charsets.UTF_8))
    }

    @Synchronized
    fun publish(status: AutoLoginStatus): Boolean {
        if (!rootDirectory.isDirectory && !rootDirectory.mkdirs()) return false

        val tempFile = File(rootDirectory, STATUS_TEMP_FILE)
        val readyFile = File(rootDirectory, STATUS_READY_FILE)
        return try {
            tempFile.writeText("${status.name}\n", Charsets.UTF_8)
            (!readyFile.exists() || readyFile.delete()) && tempFile.renameTo(readyFile)
        } catch (_: Exception) {
            false
        } finally {
            tempFile.delete()
        }
    }

    companion object {
        const val MAX_PAYLOAD_BYTES = 16_384

        private const val ROOT_PATH = "auto-login"
        private const val REQUEST_READY_FILE = "request.ready"
        private const val STATUS_TEMP_FILE = "status.tmp"
        private const val STATUS_READY_FILE = "status.ready"
    }
}
