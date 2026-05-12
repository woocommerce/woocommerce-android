package com.woocommerce.android.aiassistant.telemetry

import com.automattic.eventhorizon.AiAssistantErrorKindValue
import com.woocommerce.android.aiassistant.core.chat.AssistantError
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AssistantErrorKindMapperTest {
    @Test
    fun `when network error maps, then error kind is network`() {
        assertThat(AssistantErrorKindMapper.map(AssistantError.Network())).isEqualTo(AiAssistantErrorKindValue.Network)
    }

    @Test
    fun `when auth error maps, then error kind is auth`() {
        assertThat(AssistantErrorKindMapper.map(AssistantError.Auth())).isEqualTo(AiAssistantErrorKindValue.Auth)
    }

    @Test
    fun `when rate limit error maps, then error kind is rate limited`() {
        assertThat(AssistantErrorKindMapper.map(AssistantError.RateLimit()))
            .isEqualTo(AiAssistantErrorKindValue.RateLimited)
    }

    @Test
    fun `when timeout error maps, then error kind is timeout`() {
        assertThat(AssistantErrorKindMapper.map(AssistantError.Timeout())).isEqualTo(AiAssistantErrorKindValue.Timeout)
    }

    @Test
    fun `when bad request error maps, then error kind is validation error`() {
        assertThat(AssistantErrorKindMapper.map(AssistantError.BadRequest()))
            .isEqualTo(AiAssistantErrorKindValue.ValidationError)
    }

    @Test
    fun `when upstream failure maps, then error kind is server error`() {
        assertThat(AssistantErrorKindMapper.map(AssistantError.UpstreamFailure()))
            .isEqualTo(AiAssistantErrorKindValue.ServerError)
    }

    @Test
    fun `when tool failed error maps, then error kind is server error`() {
        assertThat(AssistantErrorKindMapper.map(AssistantError.ToolFailed("orders_update")))
            .isEqualTo(AiAssistantErrorKindValue.ServerError)
    }

    @Test
    fun `when invalid tool call error maps, then error kind is validation error`() {
        assertThat(AssistantErrorKindMapper.map(AssistantError.InvalidToolCall("orders_update")))
            .isEqualTo(AiAssistantErrorKindValue.ValidationError)
    }

    @Test
    fun `when unknown outcome error maps, then error kind is unknown`() {
        assertThat(AssistantErrorKindMapper.map(AssistantError.OutcomeUnknown("orders_update")))
            .isEqualTo(AiAssistantErrorKindValue.Unknown)
    }

    @Test
    fun `when cancelled error maps, then error kind is cancelled`() {
        assertThat(
            AssistantErrorKindMapper.map(AssistantError.Cancelled)
        ).isEqualTo(AiAssistantErrorKindValue.Cancelled)
    }

    @Test
    fun `when unknown error maps, then error kind is unknown`() {
        assertThat(AssistantErrorKindMapper.map(AssistantError.Unknown())).isEqualTo(AiAssistantErrorKindValue.Unknown)
    }
}
