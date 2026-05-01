package com.woocommerce.android.aiassistant.core.safety

import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import kotlinx.coroutines.CompletableDeferred
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class SafetyOrchestratorImpl(
    private val requestIdFactory: () -> String = { UUID.randomUUID().toString() },
) : SafetyOrchestrator {
    private val pending = ConcurrentHashMap<String, CompletableDeferred<ConfirmationResult>>()

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
        val deferred = pending[requestId]
            ?: return ConfirmationResult(requestId, ConfirmationDecision.CANCELLED)

        return try {
            deferred.await()
        } finally {
            pending.remove(requestId, deferred)
        }
    }

    override fun resolve(result: ConfirmationResult): Boolean {
        val deferred = pending[result.requestId] ?: return false
        return deferred.complete(result)
    }

    override fun cancelPending(requestId: String): Boolean {
        val deferred = pending.remove(requestId) ?: return false
        return deferred.complete(ConfirmationResult(requestId, ConfirmationDecision.CANCELLED))
    }
}
