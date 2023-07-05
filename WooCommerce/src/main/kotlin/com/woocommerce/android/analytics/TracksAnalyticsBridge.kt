package com.woocommerce.android.analytics

import com.woocommerce.shared.library.AnalyticsBridge
import javax.inject.Inject

class TracksAnalyticsBridge @Inject constructor() : AnalyticsBridge {

    override fun sendEvent(event: String) {
        AnalyticsTracker.track(event)
    }
}
