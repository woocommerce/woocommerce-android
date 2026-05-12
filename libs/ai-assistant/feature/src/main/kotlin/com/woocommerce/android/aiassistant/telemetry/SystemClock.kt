package com.woocommerce.android.aiassistant.telemetry

import javax.inject.Inject

interface SystemClock {
    fun nowMs(): Long
}

class WallSystemClock @Inject constructor() : SystemClock {
    override fun nowMs(): Long = System.currentTimeMillis()
}
