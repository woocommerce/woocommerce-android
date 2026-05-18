package com.woocommerce.android.aiassistant.telemetry

import com.automattic.eventhorizon.Trackable

interface AssistantTelemetryTracker {
    fun track(event: Trackable)
}
