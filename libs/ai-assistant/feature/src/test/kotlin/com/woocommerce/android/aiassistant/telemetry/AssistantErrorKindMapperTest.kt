package com.woocommerce.android.aiassistant.telemetry

import com.automattic.eventhorizon.AiAssistantErrorKindValue
import com.woocommerce.android.aiassistant.core.chat.AssistantError
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AssistantErrorKindMapperTest {
    @Test
    fun `Network maps to NETWORK`() {
        assertThat(AssistantErrorKindMapper.map(AssistantError.Network())).isEqualTo(AiAssistantErrorKindValue.Network)
    }

    @Test
    fun `Auth maps to AUTH`() {
        assertThat(AssistantErrorKindMapper.map(AssistantError.Auth())).isEqualTo(AiAssistantErrorKindValue.Auth)
    }

    @Test
    fun `RateLimit maps to RATE_LIMITED`() {
        assertThat(AssistantErrorKindMapper.map(AssistantError.RateLimit()))
            .isEqualTo(AiAssistantErrorKindValue.RateLimited)
    }

    @Test
    fun `Timeout maps to TIMEOUT`() {
        assertThat(AssistantErrorKindMapper.map(AssistantError.Timeout())).isEqualTo(AiAssistantErrorKindValue.Timeout)
    }

    @Test
    fun `BadRequest maps to VALIDATION_ERROR`() {
        assertThat(AssistantErrorKindMapper.map(AssistantError.BadRequest()))
            .isEqualTo(AiAssistantErrorKindValue.ValidationError)
    }

    @Test
    fun `UpstreamFailure maps to SERVER_ERROR`() {
        assertThat(AssistantErrorKindMapper.map(AssistantError.UpstreamFailure()))
            .isEqualTo(AiAssistantErrorKindValue.ServerError)
    }

    @Test
    fun `ToolFailed maps to SERVER_ERROR`() {
        assertThat(AssistantErrorKindMapper.map(AssistantError.ToolFailed("orders_update")))
            .isEqualTo(AiAssistantErrorKindValue.ServerError)
    }

    @Test
    fun `InvalidToolCall maps to VALIDATION_ERROR`() {
        assertThat(AssistantErrorKindMapper.map(AssistantError.InvalidToolCall("orders_update")))
            .isEqualTo(AiAssistantErrorKindValue.ValidationError)
    }

    @Test
    fun `OutcomeUnknown maps to UNKNOWN`() {
        assertThat(AssistantErrorKindMapper.map(AssistantError.OutcomeUnknown("orders_update")))
            .isEqualTo(AiAssistantErrorKindValue.Unknown)
    }

    @Test
    fun `Cancelled maps to CANCELLED`() {
        assertThat(AssistantErrorKindMapper.map(AssistantError.Cancelled)).isEqualTo(AiAssistantErrorKindValue.Cancelled)
    }

    @Test
    fun `Unknown maps to UNKNOWN`() {
        assertThat(AssistantErrorKindMapper.map(AssistantError.Unknown())).isEqualTo(AiAssistantErrorKindValue.Unknown)
    }
}
