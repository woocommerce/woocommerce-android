package com.woocommerce.android.aiassistant.telemetry

import com.automattic.eventhorizon.Trackable

interface AssistantTelemetry {
    fun track(event: Trackable)
}
