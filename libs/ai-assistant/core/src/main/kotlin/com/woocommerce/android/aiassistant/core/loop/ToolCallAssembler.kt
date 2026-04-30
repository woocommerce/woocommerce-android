package com.woocommerce.android.aiassistant.core.loop

import com.woocommerce.android.aiassistant.core.chat.AssistantEvent
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

internal class ToolCallAssembler(private val json: Json) {
    fun assemble(deltas: List<AssistantEvent.ToolCallDelta>): List<AssemblyResult> {
        if (deltas.isEmpty()) return emptyList()

        return deltas
            .groupBy { it.index }
            .toSortedMap()
            .values
            .map { indexDeltas -> assembleOne(indexDeltas) }
    }

    private fun assembleOne(indexDeltas: List<AssistantEvent.ToolCallDelta>): AssemblyResult {
        val id = indexDeltas.firstNotNullOfOrNull { it.id }
            ?: return AssemblyResult.MalformedArguments("unknown", "unknown", "")
        val name = indexDeltas.firstNotNullOfOrNull { it.name }
            ?: return AssemblyResult.MalformedArguments(id, "unknown", "")

        val rawArguments = indexDeltas.mapNotNull { it.argumentsDelta }.joinToString("")
        val arguments: JsonObject = when {
            rawArguments.isBlank() -> buildJsonObject {}
            else -> runCatching { json.parseToJsonElement(rawArguments).jsonObject }
                .getOrElse { return AssemblyResult.MalformedArguments(id, name, rawArguments) }
        }

        return AssemblyResult.Success(ToolCall(id = id, name = name, arguments = arguments))
    }

    sealed interface AssemblyResult {
        data class Success(val call: ToolCall) : AssemblyResult
        data class MalformedArguments(
            val callId: String,
            val toolName: String,
            val raw: String,
        ) : AssemblyResult
    }
}
