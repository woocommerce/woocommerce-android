package com.woocommerce.android.aiassistant.telemetry

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WallSystemClockTest {
    @Test
    fun `when current time is requested, then wall time is returned`() {
        val before = System.currentTimeMillis()
        val now = WallSystemClock().nowMs()
        val after = System.currentTimeMillis()

        assertThat(now).isBetween(before, after)
    }
}
