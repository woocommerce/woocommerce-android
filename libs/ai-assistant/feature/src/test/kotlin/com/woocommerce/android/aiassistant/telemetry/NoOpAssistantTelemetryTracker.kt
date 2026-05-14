package com.woocommerce.android.aiassistant.telemetry

import com.automattic.eventhorizon.Trackable

internal class NoOpAssistantTelemetryTracker : AssistantTelemetryTracker {
    override fun track(event: Trackable) = Unit
}
