package com.woocommerce.android.aiassistant.telemetry

import com.automattic.eventhorizon.Trackable

internal class RecordingAssistantTelemetryTracker : AssistantTelemetryTracker {
    val events = mutableListOf<Trackable>()

    override fun track(event: Trackable) {
        events += event
    }
}
