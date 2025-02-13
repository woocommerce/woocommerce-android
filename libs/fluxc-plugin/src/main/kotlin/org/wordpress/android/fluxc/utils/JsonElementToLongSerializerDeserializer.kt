package org.wordpress.android.fluxc.utils

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.Exception
import java.lang.reflect.Type

class JsonElementToLongSerializerDeserializer : JsonSerializer<Long?>, JsonDeserializer<Long?> {
    override fun serialize(
        src: Long?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return src?.let { JsonPrimitive(it) } ?: JsonNull.INSTANCE
    }

    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Long? {
        return try {
            json?.asLong
        } catch (e: Exception) {
            null
        }
    }
}

