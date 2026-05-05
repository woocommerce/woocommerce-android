package com.woocommerce.android.aiassistant.ui.cards

import com.woocommerce.android.aiassistant.di.AiAssistantJson
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsUiStructured
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import javax.inject.Inject

internal class AssistantCardUiStructuredParser @Inject constructor(
    @AiAssistantJson private val json: Json,
) {
    fun parse(uiStructured: JsonElement?): List<AssistantCardEntry> {
        if (uiStructured == null) return emptyList()

        val payload = try {
            json.decodeFromJsonElement<ShowCardsUiStructured>(uiStructured)
        } catch (_: SerializationException) {
            return emptyList()
        } catch (_: IllegalArgumentException) {
            return emptyList()
        }

        return AssistantCardPayloadParser.parseEntries(payload)
    }
}
