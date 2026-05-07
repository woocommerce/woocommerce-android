package com.woocommerce.android.aiassistant.tools

import kotlinx.serialization.json.JsonObject

internal fun validateAllowedArguments(
    args: JsonObject,
    allowed: Set<String>,
    toolName: String,
): Result<Unit> {
    val unknown = args.keys - allowed
    return if (unknown.isEmpty()) {
        Result.success(Unit)
    } else {
        Result.failure(IllegalArgumentException("Unsupported $toolName argument(s): ${unknown.joinToString(", ")}"))
    }
}
