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
            val logs = wooLog.getCurrentLogEntries().take(LOG_ENTRIES_LIMIT)

            File.createTempFile("log", "").apply {
                appendText(logs.joinToString("\n"))
            }
        }
    }

    companion object {
        private const val LOG_ENTRIES_LIMIT = 1000
    }
}
