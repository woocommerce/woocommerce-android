package org.wordpress.android.fluxc.network.rest

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

/**
 * Parses fields that should be a JSON object but might come as an empty array (`[]`), null, or a primitive from the
 * API. This factory ensures that if the incoming JSON token for a field is not `BEGIN_OBJECT`, it's parsed as `null`
 * instead of causing a crash.
 */
class JsonObjectOrNullAdapterFactory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (!JsonObjectOrNull::class.java.isAssignableFrom(type.rawType)) return null

        val delegate: TypeAdapter<T> = gson.getDelegateAdapter(this, type)
        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T?) {
                delegate.write(out, value)
            }

            override fun read(`in`: JsonReader): T? = if (`in`.peek() == JsonToken.BEGIN_OBJECT) {
                delegate.read(`in`)
            } else {
                // BEGIN_ARRAY, NULL, primitive vs.
                `in`.skipValue()
                null
            }
        }.nullSafe()
    }
}
