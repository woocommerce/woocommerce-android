package com.woocommerce.android.ui.aiassistant.telemetry

import com.automattic.eventhorizon.Trackable
import com.woocommerce.android.aiassistant.telemetry.AssistantTelemetryTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

internal class WooAssistantAnalyticsTracker @Inject constructor(
    private val tracker: AnalyticsTrackerWrapper,
) : AssistantTelemetryTracker {
    override fun track(event: Trackable) {
        tracker.track(event)
    }
}
