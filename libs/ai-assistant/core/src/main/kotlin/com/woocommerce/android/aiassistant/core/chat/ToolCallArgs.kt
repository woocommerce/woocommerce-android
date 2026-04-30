package com.woocommerce.android.aiassistant.core.chat

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

inline fun <reified T> ToolCall.parseArgs(json: Json): Result<T> = runCatching {
    json.decodeFromJsonElement<T>(arguments)
}
