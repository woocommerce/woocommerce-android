package com.woocommerce.android.apifaker.models

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.JsonAdapter
import java.lang.reflect.Type

@JsonAdapter(ApiTypeSerializer::class)
internal sealed interface ApiType {
    companion object {
        fun defaultValues(): List<ApiType> = listOf(WPApi, WPCom, Custom(""))
    }

    data object WPApi : ApiType
    data object WPCom : ApiType

    data class Custom(val host: String) : ApiType
}

private class ApiTypeSerializer : JsonDeserializer<ApiType>, JsonSerializer<ApiType> {
    override fun serialize(
        src: ApiType?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement? {
        if (src == null) return null

        val jsonObject = JsonObject()
        when (src) {
            is ApiType.WPApi -> jsonObject.addProperty("type", "wp-api")
            is ApiType.WPCom -> jsonObject.addProperty("type", "wp-com")
            is ApiType.Custom -> {
                jsonObject.addProperty("type", "custom")
                jsonObject.addProperty("host", src.host)
            }
        }

        return jsonObject
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): ApiType? {
        if (json == null) return null

        val jsonObject = json.asJsonObject
        val type = jsonObject.get("type").asString

        return when (type) {
            "wp-api" -> ApiType.WPApi
            "wp-com" -> ApiType.WPCom
            "custom" -> {
                val host = jsonObject.get("host").asString
                ApiType.Custom(host)
            }
            else -> null
        }
    }
}
