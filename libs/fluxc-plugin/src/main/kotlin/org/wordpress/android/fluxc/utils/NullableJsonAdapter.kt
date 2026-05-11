package org.wordpress.android.fluxc.utils

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken.NULL
import com.google.gson.stream.JsonToken.STRING
import com.google.gson.stream.JsonWriter
import com.google.gson.stream.MalformedJsonException
import java.math.BigDecimal

abstract class NullableJsonAdapter<T : Any> : TypeAdapter<T>() {
    override fun write(out: JsonWriter, value: T?) {
        val defaultSerializeNullValue = out.serializeNulls
        out.serializeNulls = true
        try {
            if (value == null) {
                out.nullValue()
            } else {
                writeValue(out, value)
            }
        } finally {
            out.serializeNulls = defaultSerializeNullValue
        }
    }

    override fun read(input: JsonReader): T? {
        return if (input.peek() == NULL) {
            input.nextNull()
            null
        } else {
            readValue(input)
        }
    }

    protected abstract fun writeValue(out: JsonWriter, value: T)

    protected abstract fun readValue(input: JsonReader): T?
}

class NullStringJsonAdapter : NullableJsonAdapter<String>() {
    override fun writeValue(out: JsonWriter, value: String) {
        out.value(value)
    }

    override fun readValue(input: JsonReader): String {
        return if (input.peek() == STRING) {
            input.nextString()
        } else {
            throw MalformedJsonException("Unexpected token: ${input.peek()}")
        }
    }
}

class NullBigDecimalJsonAdapter : NullableJsonAdapter<BigDecimal>() {
    override fun writeValue(out: JsonWriter, value: BigDecimal) {
        out.value(value)
    }

    override fun readValue(input: JsonReader): BigDecimal? {
        return input.nextString().toBigDecimalOrNull()
    }
}
