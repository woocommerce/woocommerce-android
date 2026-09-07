package com.woocommerce.android.extensions

import com.google.gson.JsonObject

/**
 * Reads [key] as a String, returning null when it's missing, JSON null, or not a primitive.
 * Lets us parse JSON manually instead of mapping it onto a reflectively-instantiated model
 * class, which R8 full mode would otherwise strip.
 */
fun JsonObject.stringOrNull(key: String): String? =
    get(key)?.takeIf { it.isJsonPrimitive }?.asString

/**
 * Reads [key] as a Long, returning null when it's missing, JSON null, not a primitive, or
 * not parseable as a number.
 */
fun JsonObject.longOrNull(key: String): Long? {
    val element = get(key)?.takeIf { it.isJsonPrimitive } ?: return null
    return runCatching { element.asLong }.getOrNull()
}
