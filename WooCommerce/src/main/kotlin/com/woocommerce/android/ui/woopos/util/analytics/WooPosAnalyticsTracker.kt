package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.analytics.IAnalyticsEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WooPosAnalyticsTracker @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val commonPropertiesProvider: WooPosAnalyticsCommonPropertiesProvider
) {
    suspend fun track(analytics: IAnalyticsEvent) {
        withContext(Dispatchers.IO) {
            analytics.addProperties(commonPropertiesProvider.commonProperties)
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
