package org.wordpress.android.fluxc.utils

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken.NULL
import com.google.gson.stream.JsonToken.NUMBER
import com.google.gson.stream.JsonToken.STRING
import com.google.gson.stream.JsonWriter
import com.google.gson.stream.MalformedJsonException

class NullJsonAdapter : TypeAdapter<Any>() {
    override fun write(out: JsonWriter, value: Any?) {
        val defaultSerializeNullValue = out.serializeNulls
        out.serializeNulls = true
        when (value) {
            null -> out.nullValue()
            is String -> out.value(value)
            is Number -> out.value(value)
            else -> throw MalformedJsonException("Unexpected value: $value")
        }
        out.serializeNulls = defaultSerializeNullValue
    }

    override fun read(input: JsonReader): Any? {
        return when (val token = input.peek()) {
            STRING -> input.nextString()
            NUMBER -> input.nextString().toBigDecimalOrNull()
            NULL -> {
                input.nextNull()
                null
            }
            else -> throw MalformedJsonException("Unexpected token: $token")
        }
    }
}
