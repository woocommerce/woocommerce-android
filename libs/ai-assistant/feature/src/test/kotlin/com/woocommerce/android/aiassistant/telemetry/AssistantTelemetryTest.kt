package com.woocommerce.android.aiassistant.telemetry

import com.woocommerce.android.aiassistant.core.chat.AssistantError
import com.woocommerce.android.aiassistant.core.chat.Diagnostics
import com.woocommerce.android.aiassistant.core.chat.ToolDiagnostics
import com.woocommerce.android.aiassistant.core.chat.ToolFailureKind
import com.woocommerce.android.aiassistant.core.chat.ToolFailureSource
import com.woocommerce.android.aiassistant.core.chat.TransportDiagnostics
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AssistantTelemetryTest {
    @Test
    fun `given transport diagnostics with raw snippet, when building telemetry event, then only allowlisted fields are used`() {
        val error = AssistantError.BadRequest(
            diagnostics = Diagnostics(
                transport = TransportDiagnostics(
                    httpStatus = 400,
                    requestId = "request-1",
                    retryAfterMs = 1_000L,
                    bodySnippet = "raw backend payload with bearer secret",
                )
            )
        )

        val event = error.toAssistantTelemetryEvent()

        assertThat(event).isEqualTo(
            AssistantTelemetryEvent(
                kind = AssistantTelemetryErrorKind.BAD_REQUEST,
                httpStatus = 400,
                requestId = "request-1",
                retryAfterMs = 1_000L,
            )
        )
        assertThat(event.toString()).doesNotContain("raw backend payload")
        assertThat(event.toString()).doesNotContain("secret")
    }

    @Test
    fun `given raw cause and tool diagnostics, when building telemetry event, then raw cause is excluded`() {
        val error = AssistantError.ToolFailed(
            toolName = "orders_update",
            diagnostics = Diagnostics(
                tool = ToolDiagnostics(
                    toolName = "orders_update",
                    failureKind = ToolFailureKind.DETERMINISTIC_FAILURE,
                    retryable = false,
                    source = ToolFailureSource.HANDLER_EXCEPTION,
                )
            ),
            cause = IllegalStateException("raw exception message"),
        )

        val event = error.toAssistantTelemetryEvent()

        assertThat(event).isEqualTo(
            AssistantTelemetryEvent(
                kind = AssistantTelemetryErrorKind.TOOL_FAILED,
                toolName = "orders_update",
                toolFailureKind = ToolFailureKind.DETERMINISTIC_FAILURE,
                toolRetryable = false,
            )
        )
        assertThat(event.toString()).doesNotContain("raw exception message")
        assertThat(event.toString()).doesNotContain("HANDLER_EXCEPTION")
    }
}
