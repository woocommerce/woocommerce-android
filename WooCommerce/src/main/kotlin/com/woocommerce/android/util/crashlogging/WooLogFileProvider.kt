package com.woocommerce.android.util.crashlogging

import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.inject.Inject

class WooLogFileProvider @Inject constructor(private val wooLog: WooLog) {
    fun provide(): File {
        return File.createTempFile("log", "").apply {
            val logs = runBlocking { wooLog.getCurrentLogEntries().take(LOG_ENTRIES_LIMIT) }
            appendText(logs.joinToString("\n"))
        }
    }

    companion object {
        private const val LOG_ENTRIES_LIMIT = 1000
    }
}
