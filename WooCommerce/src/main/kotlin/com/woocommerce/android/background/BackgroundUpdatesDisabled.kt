package com.woocommerce.android.background

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class BackgroundUpdatesDisabled @Inject constructor(
    private val getBackgroundRestrictions: GetBackgroundRestrictions,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    operator fun invoke() {
        if (getBackgroundRestrictions().isAnyRestrictionActive) {
            analyticsTrackerWrapper.track(AnalyticsEvent.BACKGROUND_UPDATES_DISABLED)
        }
    }
}
