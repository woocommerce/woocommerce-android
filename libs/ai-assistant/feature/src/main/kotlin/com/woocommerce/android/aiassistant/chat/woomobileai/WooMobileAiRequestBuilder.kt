package com.woocommerce.android.aiassistant.chat.woomobileai

import com.woocommerce.android.aiassistant.chat.openai.toOpenAi
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ChatRequest
import com.woocommerce.android.aiassistant.core.chat.ToolDefinition
import javax.inject.Inject

internal class WooMobileAiRequestBuilder @Inject constructor() {
    fun build(request: ChatRequest): WooMobileAiRequestEnvelope = WooMobileAiRequestEnvelope(
        model = WOO_MOBILE_AI_MODEL_ID,
        stream = true,
        messages = request.messages.map(AssistantMessage::toOpenAi),
        tools = request.tools.takeIf { it.isNotEmpty() }?.map(ToolDefinition::toOpenAi),
        streamOptions = WooMobileAiStreamOptions(includeUsage = true),
    )

    private companion object {
        const val WOO_MOBILE_AI_MODEL_ID = "gpt-5.1"
    }
}
