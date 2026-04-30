package com.woocommerce.android.aiassistant.core.safety

import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred

class SafetyOrchestratorImpl(
    private val requestIdFactory: () -> String = { UUID.randomUUID().toString() },
) : SafetyOrchestrator {
    private val pending = ConcurrentHashMap<String, CompletableDeferred<ConfirmationResult>>()
    private val resolvedBeforeAwait = ConcurrentHashMap<String, ConfirmationResult>()

    override suspend fun evaluate(
        call: ToolCall,
        descriptor: ToolDescriptor,
    ): SafetyDecision {
        if (descriptor.safetyLevel == ToolSafetyLevel.SAFE) {
            return SafetyDecision.Execute
        }

        val request = ConfirmationRequest(
            id = requestIdFactory(),
            toolCallId = call.id,
            toolName = call.name,
            arguments = call.arguments,
            safetyLevel = descriptor.safetyLevel,
        )
        pending[request.id] = CompletableDeferred()
        return SafetyDecision.RequireConfirmation(request)
    }

    override suspend fun awaitResult(requestId: String): ConfirmationResult {
        resolvedBeforeAwait.remove(requestId)?.let { return it }
        val deferred = pending[requestId]
            ?: return ConfirmationResult(requestId, ConfirmationDecision.CANCELLED)

        return try {
            deferred.await()
        } finally {
            pending.remove(requestId)
            resolvedBeforeAwait.remove(requestId)
        }
    }

    override fun resolve(result: ConfirmationResult): Boolean {
        val deferred = pending.remove(result.requestId) ?: return false
        resolvedBeforeAwait[result.requestId] = result
        return deferred.complete(result)
    }
}
