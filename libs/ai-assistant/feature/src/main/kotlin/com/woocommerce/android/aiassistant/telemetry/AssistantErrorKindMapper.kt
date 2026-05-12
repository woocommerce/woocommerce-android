package com.woocommerce.android.aiassistant.telemetry

import com.automattic.eventhorizon.AiAssistantErrorKindValue
import com.woocommerce.android.aiassistant.core.chat.AssistantError

object AssistantErrorKindMapper {
    fun map(error: AssistantError): AiAssistantErrorKindValue = when (error) {
        is AssistantError.Network -> AiAssistantErrorKindValue.Network
        is AssistantError.Auth -> AiAssistantErrorKindValue.Auth
        is AssistantError.RateLimit -> AiAssistantErrorKindValue.RateLimited
        is AssistantError.BadRequest -> AiAssistantErrorKindValue.ValidationError
        is AssistantError.Timeout -> AiAssistantErrorKindValue.Timeout
        is AssistantError.UpstreamFailure -> AiAssistantErrorKindValue.ServerError
        is AssistantError.ToolFailed -> AiAssistantErrorKindValue.ServerError
        is AssistantError.InvalidToolCall -> AiAssistantErrorKindValue.ValidationError
        is AssistantError.OutcomeUnknown -> AiAssistantErrorKindValue.Unknown
        AssistantError.Cancelled -> AiAssistantErrorKindValue.Cancelled
        is AssistantError.Unknown -> AiAssistantErrorKindValue.Unknown
    }
}
