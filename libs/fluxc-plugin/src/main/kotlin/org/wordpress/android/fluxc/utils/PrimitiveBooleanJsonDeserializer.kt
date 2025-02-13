package org.wordpress.android.fluxc.utils

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import java.lang.reflect.Type

@Suppress("SwallowedException")
class PrimitiveBooleanJsonDeserializer : JsonDeserializer<Boolean?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Boolean? {
        return try {
            if (json == null || json.isJsonNull) return null
            (json as? JsonPrimitive)?.let { jsonPrimitive ->
                when {
                    jsonPrimitive.isBoolean -> jsonPrimitive.asBoolean
                    jsonPrimitive.isNumber -> tryIntToBoolean(jsonPrimitive)
                    jsonPrimitive.isString -> tryStringToBoolean(jsonPrimitive)
                    else -> null
                }
            }
        } catch (e: IllegalStateException) {
            null
        } catch (e: ClassCastException) {
            null
        } catch (e: UnsupportedOperationException) {
            null
        }
    }

    private fun tryIntToBoolean(json: JsonElement?): Boolean? {
        return try {
            return when (json?.asInt) {
                0 -> false
                1 -> true
                else -> null
            }
        } catch (e: IllegalStateException) {
            null
        } catch (e: ClassCastException) {
            null
        } catch (e: UnsupportedOperationException) {
            null
        }
    }

    private fun tryStringToBoolean(json: JsonElement?): Boolean? {
        return try {
            val stringValue = json?.asString?.replace(" ", "") ?: return null
            stringValue.toBooleanStrictOrNull() ?: run {
                val intValue = stringValue.toInt()
                val intPrimitive = JsonPrimitive(intValue)
                tryIntToBoolean(intPrimitive)
            }
        } catch (e: IllegalStateException) {
            null
        } catch (e: ClassCastException) {
            null
        } catch (e: UnsupportedOperationException) {
            null
        }
    }
}
