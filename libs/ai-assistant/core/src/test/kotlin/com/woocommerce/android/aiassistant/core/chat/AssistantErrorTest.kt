package com.woocommerce.android.aiassistant.core.chat

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AssistantErrorTest {
    @Test
    fun `given each variant, when listed, then all ten kinds are present and distinct`() {
        val variants: List<AssistantError> = listOf(
            AssistantError.Network,
            AssistantError.Auth,
            AssistantError.RateLimit,
            AssistantError.Timeout,
            AssistantError.UpstreamFailure,
            AssistantError.ToolFailed(toolName = "create_order"),
            AssistantError.InvalidToolCall(toolName = "create_order"),
            AssistantError.OutcomeUnknown(toolName = "create_order"),
            AssistantError.Cancelled,
            AssistantError.Unknown(),
        )

        assertThat(variants).hasSize(10)
        assertThat(variants.map { it::class }.toSet()).hasSize(10)
    }

    @Test
    fun `given OutcomeUnknown, when compared to Network and Timeout, then it is a distinct type`() {
        val outcomeUnknown: AssistantError = AssistantError.OutcomeUnknown(toolName = "create_order")

        assertThat(outcomeUnknown).isNotEqualTo(AssistantError.Network)
        assertThat(outcomeUnknown).isNotEqualTo(AssistantError.Timeout)
        assertThat(outcomeUnknown::class).isNotEqualTo(AssistantError.Network::class)
        assertThat(outcomeUnknown::class).isNotEqualTo(AssistantError.Timeout::class)
    }

    @Test
    fun `given OutcomeUnknown, when constructed, then carries tool name`() {
        val error = AssistantError.OutcomeUnknown(toolName = "create_order")

        assertThat(error.toolName).isEqualTo("create_order")
    }

    @Test
    fun `given ToolFailed with cause, when constructed, then carries tool name and cause`() {
        val cause = IllegalStateException("boom")

        val error = AssistantError.ToolFailed(toolName = "update_product", cause = cause)

        assertThat(error.toolName).isEqualTo("update_product")
        assertThat(error.cause).isSameAs(cause)
    }

    @Test
    fun `given InvalidToolCall, when constructed, then carries tool name`() {
        val error = AssistantError.InvalidToolCall(toolName = "set_status")

        assertThat(error.toolName).isEqualTo("set_status")
    }

    @Test
    fun `given Unknown with cause, when constructed, then carries cause`() {
        val cause = RuntimeException("???")

        val error = AssistantError.Unknown(cause = cause)

        assertThat(error.cause).isSameAs(cause)
    }
}
