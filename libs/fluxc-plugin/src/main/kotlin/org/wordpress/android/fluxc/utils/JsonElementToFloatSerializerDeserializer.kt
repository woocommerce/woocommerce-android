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

class JsonElementToFloatSerializerDeserializer : JsonSerializer<Float?>, JsonDeserializer<Float?> {
    override fun serialize(
        src: Float?,
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
    ): Float? {
        return try {
            json?.asFloat
        } catch (e: Exception) {
            null
        }
    }
}
