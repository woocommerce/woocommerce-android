package com.woocommerce.android.aiassistant.telemetry

internal data class AssistantTelemetryContext(
    val conversationId: String,
    val requestId: String,
    val messageId: String,
)
