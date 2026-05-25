package com.woocommerce.android.aiassistant.headless

object WooAiSmokeLiveRunGate {
    const val PROPERTY_NAME = "wooAiSmokeRunLive"
    const val DISABLED_REASON = "Run with -PwooAiSmokeRunLive=true to run live Woo AI smoke tests."

    fun isEnabled(): Boolean =
        System.getProperty(PROPERTY_NAME) == "true"
}
