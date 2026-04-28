package com.woocommerce.android.aiassistant.core.chat

import kotlinx.coroutines.flow.Flow

/**
 * Single seam for AI completions. Concrete implementations own the transport,
 * JWT auth, and JSON shape so the loop can be swapped onto a different backend
 * without upstream changes.
 *
 * Implementations must:
 *  - emit [AssistantEvent]s and terminate the flow when the stream ends
 *  - normalize transport errors to [AssistantEvent.Failed] with the appropriate [AssistantErrorKind]
 *  - support coroutine cancellation: cancelling the collector aborts the underlying HTTP call
 */
interface ChatService {
    fun streamTurn(request: ChatRequest): Flow<AssistantEvent>
}
