package org.wordpress.android.fluxc.utils

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class NonNegativeDoubleJsonDeserializer : JsonDeserializer<Double?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Double? {
        return try {
            json?.asDouble?.let {
                if (it >= 0) it else null
            }
        } catch (e: NumberFormatException) {
            null
        }
    }
}
