package com.woocommerce.android.ui.woopos.util.analytics

import javax.inject.Inject

class WooPosAnalyticsTracker @Inject constructor() {
    fun trackEvent(analytics: WooPosAnalytics) {
        when (analytics) {
            is WooPosAnalytics.Error -> TODO()
            is WooPosAnalytics.Event -> TODO()
        }
    }
}
