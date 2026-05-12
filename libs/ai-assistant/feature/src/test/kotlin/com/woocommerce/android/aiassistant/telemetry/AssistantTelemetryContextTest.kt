package com.woocommerce.android.aiassistant.telemetry

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AssistantTelemetryContextTest {
    @Test
    fun `context exposes only three string ids`() {
        val context = AssistantTelemetryContext("conversation", "request", "message")

        assertThat(context.conversationId).isEqualTo("conversation")
        assertThat(context.requestId).isEqualTo("request")
        assertThat(context.messageId).isEqualTo("message")
    }
}
