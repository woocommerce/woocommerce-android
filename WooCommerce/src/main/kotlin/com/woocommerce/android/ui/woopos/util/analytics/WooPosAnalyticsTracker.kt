package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WooPosAnalyticsTracker @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    suspend fun track(analytics: WooPosAnalytics) {
        withContext(Dispatchers.IO) {
            when (analytics) {
                is WooPosAnalytics.Event -> {
                    analyticsTrackerWrapper.track(
                        analytics,
                        analytics.properties
                    )
                }

                is WooPosAnalytics.Error -> {
                    analyticsTrackerWrapper.track(
                        analytics,
                        analytics.properties,
                        analytics.errorContext.simpleName,
                        analytics.errorType,
                        analytics.errorDescription
                    )
                }
            }
        }
    }
}
