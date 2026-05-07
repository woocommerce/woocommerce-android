package com.woocommerce.android.aiassistant.core.safety

import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor

interface SafetyOrchestrator {
    suspend fun evaluate(
        call: ToolCall,
        descriptor: ToolDescriptor,
    ): SafetyDecision

    suspend fun awaitResult(requestId: String): ConfirmationResult

    fun resolve(result: ConfirmationResult): Boolean

    fun cancelPending(requestId: String): Boolean

    fun confirm(requestId: String): Boolean =
        resolve(ConfirmationResult(requestId, ConfirmationDecision.CONFIRMED))

    fun cancel(requestId: String): Boolean =
        resolve(ConfirmationResult(requestId, ConfirmationDecision.CANCELLED))
}

sealed interface SafetyDecision {
    data object Execute : SafetyDecision
    data class RequireConfirmation(val request: ConfirmationRequest) : SafetyDecision
}
