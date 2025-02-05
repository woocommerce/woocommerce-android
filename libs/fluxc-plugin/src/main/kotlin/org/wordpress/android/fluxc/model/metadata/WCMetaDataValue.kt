package org.wordpress.android.fluxc.model.metadata

import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

sealed class WCMetaDataValue {
    abstract val isPrimitive: Boolean
    abstract val stringValue: String?
    internal abstract val jsonValue: JsonElement

    override fun toString(): String = stringValue.toString()

    data class StringValue(override val stringValue: String?) : WCMetaDataValue() {
        override val isPrimitive: Boolean
            get() = true
        override val jsonValue: JsonElement
            get() = stringValue?.let { JsonPrimitive(it) } ?: JsonNull.INSTANCE
    }

    data class NumberValue(val number: Number) : WCMetaDataValue() {
        override val isPrimitive: Boolean
            get() = true
        override val stringValue: String
            get() = number.toString()
        override val jsonValue: JsonElement
            get() = JsonPrimitive(number)
    }

    data class BooleanValue(val boolean: Boolean) : WCMetaDataValue() {
        override val isPrimitive: Boolean
            get() = true
        override val stringValue: String
            get() = boolean.toString()
        override val jsonValue: JsonElement
            get() = JsonPrimitive(boolean)
    }

    data class JsonObjectValue(override val jsonValue: JsonObject) : WCMetaDataValue() {
        override val isPrimitive: Boolean
            get() = false
        override val stringValue: String
            get() = jsonValue.toString()
    }

    data class JsonArrayValue(override val jsonValue: JsonArray) : WCMetaDataValue() {
        override val isPrimitive: Boolean
            get() = false
        override val stringValue: String
            get() = jsonValue.toString()
    }

    internal class WCMetaDataValueJsonAdapter : JsonDeserializer<WCMetaDataValue>,
        JsonSerializer<WCMetaDataValue> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): WCMetaDataValue? {
            return json?.let { fromJsonElement(it) }
        }

        override fun serialize(
            src: WCMetaDataValue?,
            typeOfSrc: Type?,
            context: JsonSerializationContext?
        ): JsonElement {
            return src?.jsonValue ?: JsonNull.INSTANCE
        }
    }

    companion object {
        operator fun invoke(value: String?): WCMetaDataValue {
            if (value == null) return StringValue(null)

            return runCatching { JsonParser().parse(value) }
                .getOrElse { JsonPrimitive(value) }
                .let { fromJsonElement(it) }
        }
        operator fun invoke(value: Number): WCMetaDataValue = NumberValue(value)
        operator fun invoke(value: Boolean): WCMetaDataValue = BooleanValue(value)

        internal fun fromJsonElement(element: JsonElement): WCMetaDataValue {
            return when {
                element.isJsonPrimitive -> {
                    val primitive = element.asJsonPrimitive
                    when {
                        primitive.isBoolean -> BooleanValue(primitive.asBoolean)
                        primitive.isNumber -> NumberValue(primitive.asNumber)
                        else -> StringValue(primitive.asString)
                    }
                }

                element.isJsonObject -> JsonObjectValue(element.asJsonObject)
                element.isJsonArray -> JsonArrayValue(element.asJsonArray)
                element.isJsonNull -> StringValue(null)
                else -> StringValue(element.toString())
            }
        }
    }
}
