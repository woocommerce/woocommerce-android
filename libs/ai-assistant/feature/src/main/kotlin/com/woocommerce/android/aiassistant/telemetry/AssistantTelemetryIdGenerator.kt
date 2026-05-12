package com.woocommerce.android.aiassistant.telemetry

import java.util.UUID
import javax.inject.Inject

interface AssistantTelemetryIdGenerator {
    fun nextId(): String
}

class WallAssistantTelemetryIdGenerator @Inject constructor() : AssistantTelemetryIdGenerator {
    override fun nextId(): String = UUID.randomUUID().toString()
}
