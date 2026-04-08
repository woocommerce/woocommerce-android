package org.wordpress.android.fluxc.network.rest

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonSyntaxException
import com.google.gson.internal.GsonTypes.getRawType
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.Array as JArray

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
 * (0, 1, 2, ...), not by insertion order. Any non-numeric key results in a JsonSyntaxException.
 * If the JSON value is null, null is returned.
 */
class ArrayOrObjectDeserializer : JsonDeserializer<Array<*>> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Array<*>? {
        val componentType: Type = (typeOfT as? GenericArrayType)?.genericComponentType
            ?: throw JsonSyntaxException("Expected a GenericArrayType, but got: $typeOfT")

        val items: List<Any?>? = jsonToList(json, componentType, context)

        return items?.listToArray(componentType)
    }

    private fun List<Any?>.listToArray(componentType: Type): Array<Any?> {
        val arr = JArray.newInstance(getRawType(componentType), size)
        forEachIndexed { i, v -> JArray.set(arr, i, v) }
        @Suppress("UNCHECKED_CAST")
        return arr as Array<Any?>
    }
}

/**
 * Deserializes a JSON value that can be either an array or an object with numeric string keys.
 * Examples:
 * - Array: ["item1", "item2"]
 * - Object with numeric keys: {"0": "item1", "1": "item2"}
 * - Non-ordered numeric keys example: {"2":"a","1":"b","10":"z"} -> ["b", "a", "z"]
 *
 * This deserializer handles both cases and converts them into a List<T>.
 * - For arrays: elements are deserialized in their natural order.
 * - For objects: keys must be numeric strings (0, 1, 2, ...). Values are read by sorting keys numerically
 * (0, 1, 2, ...), not by insertion order. Any non-numeric key results in a JsonParseException.
 * If the JSON value is null, null is returned.
 */
class ListOrObjectDeserializer : JsonDeserializer<List<*>> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): List<*>? {
        val itemType = (typeOfT as ParameterizedType).actualTypeArguments[0]
        return jsonToList(json, itemType, context)
    }
}

private fun jsonToList(
    json: JsonElement,
    itemType: Type,
    context: JsonDeserializationContext
): List<Any?>? {
    return when {
        json.isJsonArray -> json.asJsonArray.map { element ->
            context.deserialize(element, itemType)
        }

        json.isJsonObject -> json.asJsonObject.entrySet()
            .onEach { entry ->
                val key = entry.key
                key.toIntOrNull()
                    ?: throw JsonSyntaxException("Unexpected key in JSON object matching array: $key")
            }
            .sortedBy { it.key.toInt() }
            .map { entry ->
                context.deserialize(entry.value, itemType)
            }

        json.isJsonNull -> null
        else -> throw JsonSyntaxException("Unexpected JSON type for List, $json")
    }
}
