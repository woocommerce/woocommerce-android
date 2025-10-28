package com.woocommerce.android.util.crashlogging

import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class EncryptedLogsFileProvider @Inject constructor(
    private val wooLog: WooLog,
    private val dispatchers: CoroutineDispatchers
) {
    suspend fun provide(): File {
        return withContext(dispatchers.io) {
            runCatching {
                val logs = wooLog.getCurrentLogEntries().take(LOG_ENTRIES_LIMIT)
                File.createTempFile("log", "").apply {
                    appendText(logs.joinToString("\n"))
                }
            }.getOrElse { exception ->
                WooLog.e(WooLog.T.UTILS, "Could not create log file: ${exception.message}")
                File.createTempFile("log_empty_file_due_to_error", "")
            }
        }
    }

    companion object {
        private const val LOG_ENTRIES_LIMIT = 1000
    }
}
