package com.woocommerce.android.ui.woopos.lock

/** Persisted monotonic time belongs to one boot. Never use wall time to reduce a PIN wait. */
internal data class WooPosPINAttemptState(
    val failures: Long = 0,
    val bootCount: Int = 0,
    val lastFailureMillis: Long = 0,
    val waitMillis: Long = 0,
) {
    init {
        require(failures >= 0 && bootCount >= 0 && lastFailureMillis >= 0)
        require(waitMillis in 0..HOUR)
    }

    fun at(time: WooPosPINTime): WooPosPINAttemptState = when {
        failures == 0L -> this
        bootCount != time.bootCount || time.elapsedMillis < lastFailureMillis ->
            copy(bootCount = time.bootCount, lastFailureMillis = time.elapsedMillis)
        time.elapsedMillis - lastFailureMillis >= QUIET_PERIOD -> WooPosPINAttemptState()
        else -> this
    }

    fun remainingMillis(time: WooPosPINTime): Long =
        (waitMillis - (time.elapsedMillis - lastFailureMillis).coerceAtLeast(0)).coerceAtLeast(0)

    fun failed(time: WooPosPINTime): WooPosPINAttemptState {
        val count = if (failures == Long.MAX_VALUE) HOURLY_THRESHOLD else failures + 1
        val wait = when {
            count % ATTEMPT_GROUP != 0L -> 0L
            count == FIRST_THRESHOLD -> FIRST_WAIT
            count == SECOND_THRESHOLD -> SECOND_WAIT
            count == THIRD_THRESHOLD -> THIRD_WAIT
            else -> HOUR
        }
        return WooPosPINAttemptState(count, time.bootCount, time.elapsedMillis, wait)
    }

    companion object {
        private const val ATTEMPT_GROUP = 5L
        private const val FIRST_THRESHOLD = 5L
        private const val SECOND_THRESHOLD = 10L
        private const val THIRD_THRESHOLD = 15L
        private const val HOURLY_THRESHOLD = 20L
        private const val FIRST_WAIT = 30_000L
        private const val SECOND_WAIT = 300_000L
        private const val THIRD_WAIT = 1_800_000L
        private const val HOUR = 3_600_000L
        const val QUIET_PERIOD = 24 * HOUR
    }
}

data class WooPosPINTime(val bootCount: Int, val elapsedMillis: Long) {
    init {
        require(bootCount >= 0 && elapsedMillis >= 0)
    }
}
