package com.woocommerce.android.aiassistant.telemetry

import javax.inject.Inject

internal interface SystemClock {
    fun nowMs(): Long
}

internal class WallSystemClock @Inject constructor() : SystemClock {
    override fun nowMs(): Long = System.currentTimeMillis()
}
