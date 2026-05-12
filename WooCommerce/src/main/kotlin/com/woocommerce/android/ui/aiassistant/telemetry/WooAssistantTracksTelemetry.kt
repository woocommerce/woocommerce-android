package com.woocommerce.android.ui.aiassistant.telemetry

import com.automattic.eventhorizon.Trackable
import com.woocommerce.android.aiassistant.telemetry.AssistantTelemetry
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

internal class WooAssistantTracksTelemetry @Inject constructor(
    private val tracker: AnalyticsTrackerWrapper,
) : AssistantTelemetry {
    override fun track(event: Trackable) {
        tracker.track(event)
    }
}
