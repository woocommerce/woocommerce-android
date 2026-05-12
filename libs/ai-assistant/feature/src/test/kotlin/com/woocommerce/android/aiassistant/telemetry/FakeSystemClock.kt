package com.woocommerce.android.aiassistant.telemetry

internal class FakeSystemClock(initialMs: Long = 0L) : SystemClock {
    private var nowMs = initialMs

    override fun nowMs(): Long = nowMs

    fun advance(ms: Long) {
        nowMs += ms
    }
}
