package org.wordpress.android.fluxc.utils

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken.NULL
import com.google.gson.stream.JsonToken.STRING
import com.google.gson.stream.JsonWriter
import com.google.gson.stream.MalformedJsonException

class NullStringJsonAdapter : TypeAdapter<String>() {
    override fun write(out: JsonWriter, value: String?) {
        val defaultSerializeNullValue = out.serializeNulls
        out.serializeNulls = true
        if (value == null) {
            out.nullValue()
        } else {
            out.value(value)
        }
        out.serializeNulls = defaultSerializeNullValue
    }

    override fun read(input: JsonReader): String? {
        return when (val token = input.peek()) {
            STRING -> input.nextString()
            NULL -> {
                input.nextNull()
                null
            }
            else -> throw MalformedJsonException("Unexpected token: $token")
        }
    }
}
