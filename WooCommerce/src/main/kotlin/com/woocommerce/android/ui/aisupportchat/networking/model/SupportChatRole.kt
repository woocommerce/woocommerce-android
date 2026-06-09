package com.woocommerce.android.ui.aisupportchat.networking.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import java.lang.reflect.Type

/**
 * Role of a [SupportChatMessage]. Unknown values are decoded to [UNKNOWN] so the
 * client stays forward-compatible if the bot adds new roles server-side.
 */
@JsonAdapter(SupportChatRole.Deserializer::class)
enum class SupportChatRole(val wireValue: String) {
    USER("user"),
    BOT("bot"),
    UNKNOWN("");

    class Deserializer : JsonDeserializer<SupportChatRole> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): SupportChatRole {
            val raw = json.takeIf { it.isJsonPrimitive }?.asString
            return entries.firstOrNull { it.wireValue == raw } ?: UNKNOWN
        }
    }
}
