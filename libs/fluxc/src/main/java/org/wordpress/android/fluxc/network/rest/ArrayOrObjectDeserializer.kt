package org.wordpress.android.fluxc.network.rest

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Deserializes a JSON value that can be either an array or an object with numeric string keys.
 * Examples:
 * - Array: ["item1", "item2"]
 * - Object with numeric keys: {"0": "item1", "1": "item2"}
 * - Non-ordered numeric keys example: {"2":"a","1":"b","10":"z"} -> ["b", "a", "z"]
 *
 * This deserializer handles both cases and converts them into an Array<T>.
 * - For arrays: elements are deserialized in their natural order.
 * - For objects: keys must be numeric strings (0, 1, 2, ...). Values are read by sorting keys numerically
 * (0, 1, 2, ...), not by insertion order. Any non-numeric key results in a JsonParseException.
 * If the JSON value is null, null is returned.
 */
class ArrayOrObjectDeserializer : JsonDeserializer<Array<*>> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Array<*>? {
        val componentType: Type = when (typeOfT) {
            is GenericArrayType -> typeOfT.genericComponentType
            is Class<*> -> if (typeOfT.isArray) {
                typeOfT.componentType
            } else {
                throw JsonParseException("Expected array: $typeOfT")
            }

            else -> throw JsonParseException("Unsupported type: $typeOfT")
        }
        val raw: Class<*> = when (componentType) {
            is Class<*> -> componentType
            is ParameterizedType -> componentType.rawType as Class<*>
            else -> Any::class.java
        }
        val items: List<Any?> = when {
            json.isJsonArray -> json.asJsonArray.map { context.deserialize(it, componentType) }
            json.isJsonObject -> json.asJsonObject.entrySet()
                .onEach {
                    it.key.toIntOrNull() ?: throw JsonParseException("Unexpected key: ${it.key}")
                }
                .sortedBy { it.key.toInt() }
                .map { context.deserialize(it.value, componentType) }

            json.isJsonNull -> return null
            else -> throw JsonParseException("Unexpected JSON for Array: $json")
        }
        val arr = java.lang.reflect.Array.newInstance(raw, items.size)
        items.forEachIndexed { i, v -> java.lang.reflect.Array.set(arr, i, v) }
        @Suppress("UNCHECKED_CAST")
        return arr as Array<*>
    }
}
