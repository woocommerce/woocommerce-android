package com.woocommerce.android.aiassistant.telemetry

import com.automattic.eventhorizon.AiAssistantErrorKindValue
import com.woocommerce.android.aiassistant.core.chat.AssistantError
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AssistantErrorKindMapperTest {
    @Test
    fun `given each assistant error, when mapped, then bounded error kind is returned`() {
        val cases: List<Pair<AssistantError, AiAssistantErrorKindValue>> = listOf(
            AssistantError.Network() to AiAssistantErrorKindValue.Network,
            AssistantError.Auth() to AiAssistantErrorKindValue.Auth,
            AssistantError.RateLimit() to AiAssistantErrorKindValue.RateLimited,
            AssistantError.Timeout() to AiAssistantErrorKindValue.Timeout,
            AssistantError.BadRequest() to AiAssistantErrorKindValue.ValidationError,
            AssistantError.UpstreamFailure() to AiAssistantErrorKindValue.ServerError,
            AssistantError.ToolFailed("orders_update") to AiAssistantErrorKindValue.ServerError,
            AssistantError.InvalidToolCall("orders_update") to AiAssistantErrorKindValue.ValidationError,
            AssistantError.OutcomeUnknown("orders_update") to AiAssistantErrorKindValue.Unknown,
            AssistantError.Cancelled to AiAssistantErrorKindValue.Cancelled,
            AssistantError.Unknown() to AiAssistantErrorKindValue.Unknown,
        )

        cases.forEach { (error, expected) ->
            assertThat(AssistantErrorKindMapper.map(error))
                .describedAs(error::class.simpleName)
                .isEqualTo(expected)
        }
    }
}
