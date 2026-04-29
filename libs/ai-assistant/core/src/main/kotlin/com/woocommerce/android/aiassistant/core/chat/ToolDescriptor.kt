package com.woocommerce.android.aiassistant.core.chat

import kotlinx.serialization.json.JsonObject

data class ToolDescriptor(
    val name: String,
    val description: String,
    val inputSchema: JsonObject,
    val safetyLevel: ToolSafetyLevel,
)
