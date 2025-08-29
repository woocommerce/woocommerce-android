package org.wordpress.android.fluxc.network.discovery

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

/**
 * Deserializes the namespaces field that can be either an array of strings or an object with numeric keys.
 * By default WordPress sites return namespaces as an array: ["namespace1", "namespace2"]
 * While others return it as an object: {"0": "namespace1", "1": "namespace2"} when some plugins are installed.
 * This deserializer handles both cases and converts them to a List<String>.
 */
class NamespacesDeserializer : JsonDeserializer<List<String>?> {
    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): List<String> {
        return if (json.isJsonArray) {
            json.getAsJsonArray().mapNotNull {
                if (it.isJsonPrimitive && it.getAsJsonPrimitive().isString) {
                    it.asString
                } else {
                    null
                }
            }
        } else if (json.isJsonObject) {
            buildList {
                val jsonObject = json.getAsJsonObject()

                for (key in jsonObject.keySet()) {
                    val element = jsonObject.get(key)
                    if (element.isJsonPrimitive && element.getAsJsonPrimitive().isString) {
                        add(element.asString)
                    }
                }
            }
        } else {
            throw JsonParseException("Unexpected JSON type for namespaces")
        }
    }
}
