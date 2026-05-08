package com.woocommerce.android.aiassistant.core.headless

import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.safety.ConfirmationDecision
import com.woocommerce.android.aiassistant.core.safety.ConfirmationRequest
import com.woocommerce.android.aiassistant.core.safety.ConfirmationResult
import com.woocommerce.android.aiassistant.core.safety.SafetyDecision
import com.woocommerce.android.aiassistant.core.safety.SafetyOrchestrator

class ScriptedHeadlessSafetyOrchestrator(
    private val defaultDecision: ConfirmationDecision = ConfirmationDecision.CANCELLED,
    private val decisionsByToolCallId: Map<String, ConfirmationDecision> = emptyMap(),
    private val requestIdFactory: (ToolCall) -> String = { call -> "${call.id}-confirmation" },
) : SafetyOrchestrator {
    val requests = mutableListOf<ConfirmationRequest>()
    val results = mutableListOf<ConfirmationResult>()

    override suspend fun evaluate(
        call: ToolCall,
        descriptor: ToolDescriptor,
    ): SafetyDecision {
        if (descriptor.safetyLevel == ToolSafetyLevel.SAFE) {
            return SafetyDecision.Execute
        }

        val request = ConfirmationRequest(
            id = requestIdFactory(call),
            toolCallId = call.id,
            toolName = call.name,
            arguments = call.arguments,
            safetyLevel = descriptor.safetyLevel,
        )
        requests += request
        return SafetyDecision.RequireConfirmation(request)
    }

    override suspend fun awaitResult(requestId: String): ConfirmationResult {
        val request = requests.firstOrNull { it.id == requestId }
        val result = ConfirmationResult(
            requestId = requestId,
            decision = request?.let { decisionsByToolCallId[it.toolCallId] } ?: defaultDecision,
        )
        results += result
        return result
    }

    override fun resolve(result: ConfirmationResult): Boolean {
        results += result
        return requests.any { it.id == result.requestId }
    }

    override fun cancelPending(requestId: String): Boolean =
        resolve(ConfirmationResult(requestId, ConfirmationDecision.CANCELLED))
}
