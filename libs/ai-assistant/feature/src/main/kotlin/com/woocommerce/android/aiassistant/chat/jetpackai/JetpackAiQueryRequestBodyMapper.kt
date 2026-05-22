package com.woocommerce.android.aiassistant.chat.jetpackai

import com.woocommerce.android.aiassistant.chat.openai.OpenAiRequestBody
import com.woocommerce.android.aiassistant.chat.openai.OpenAiRequestBodyMapper
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class JetpackAiQueryRequestBodyMapper(
    private val featureName: String,
) : OpenAiRequestBodyMapper {
    override fun mapToJson(canonical: OpenAiRequestBody, json: Json): String =
        json.encodeToString(
            JetpackAiQueryRequestBody(
                feature = featureName,
                model = canonical.model,
                stream = canonical.stream,
                messages = canonical.messages,
                tools = canonical.tools,
            )
        )
}
