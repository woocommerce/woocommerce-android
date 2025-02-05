package org.wordpress.android.fluxc.utils

import com.google.gson.JsonElement

const val EMPTY_JSON_ARRAY = "[]"

fun JsonElement?.isElementNullOrEmpty(): Boolean {
    return this?.let {
        when{
            this.isJsonObject -> this.asJsonObject.size() == 0
            this.isJsonArray -> this.asJsonArray.size()== 0
            this.isJsonPrimitive && this.asJsonPrimitive.isString -> this.asString.isEmpty()
            this.isJsonNull -> true
            else -> false
        }
    } ?: true
}
