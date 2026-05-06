package com.woocommerce.android.aiassistant.core.safety

import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import kotlinx.serialization.json.JsonObject

data class ConfirmationRequest(
    val id: String,
    val toolCallId: String,
    val toolName: String,
    val arguments: JsonObject,
    val safetyLevel: ToolSafetyLevel,
)

data class ConfirmationResult(
    val requestId: String,
    val decision: ConfirmationDecision,
)

enum class ConfirmationDecision {
    CONFIRMED,
    CANCELLED,
}
