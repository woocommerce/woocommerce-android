package com.woocommerce.android.wear.crashlogging

import com.automattic.android.tracks.crashlogging.CrashLogging
import org.wordpress.android.fluxc.logging.FluxCCrashLogger

class FluxCCrashLoggerImpl(private val crashLogging: CrashLogging) : FluxCCrashLogger {
    override fun recordEvent(message: String, category: String?) {
        crashLogging.recordEvent(message, category)
    }

    override fun recordException(exception: Throwable, category: String?) {
        crashLogging.recordException(exception, category)
    }

    override fun sendReport(exception: Throwable?, tags: Map<String, String>, message: String?) {
        crashLogging.sendReport(exception, tags, message)
    }
}
