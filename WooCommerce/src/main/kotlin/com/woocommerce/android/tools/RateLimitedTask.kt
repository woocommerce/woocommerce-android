package com.woocommerce.android.tools

import org.wordpress.android.util.DateTimeUtils
import java.util.Date

abstract class RateLimitedTask(private val minRateInSeconds: Int) {
    private var lastUpdate: Date? = null

    @Synchronized fun forceRun(): Boolean {
        if (run()) {
            lastUpdate = Date()
            return true
        }
        return false
    }

    @Synchronized fun runIfNotLimited(): Boolean {
        val now = Date()
        if (lastUpdate == null || DateTimeUtils.secondsBetween(now, lastUpdate) >= minRateInSeconds) {
            if (run()) {
                lastUpdate = now
                return true
            }
        }
        return false
    }

    protected abstract fun run(): Boolean
}
