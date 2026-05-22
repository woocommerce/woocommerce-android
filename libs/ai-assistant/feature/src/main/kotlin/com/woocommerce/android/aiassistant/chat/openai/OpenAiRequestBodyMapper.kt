package com.woocommerce.android.aiassistant.chat.openai

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal interface OpenAiRequestBodyMapper {
    fun mapToJson(canonical: OpenAiRequestBody, json: Json): String
}

internal object IdentityOpenAiRequestBodyMapper : OpenAiRequestBodyMapper {
    override fun mapToJson(canonical: OpenAiRequestBody, json: Json): String =
        json.encodeToString(canonical)
}
