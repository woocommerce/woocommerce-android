package com.woocommerce.android.aiassistant.telemetry

import com.automattic.eventhorizon.Trackable

internal class NoOpAssistantTelemetry : AssistantTelemetry {
    override fun track(event: Trackable) = Unit
}
