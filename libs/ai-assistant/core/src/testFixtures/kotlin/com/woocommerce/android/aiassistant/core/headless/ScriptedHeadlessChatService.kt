package com.woocommerce.android.aiassistant.core.headless

import com.woocommerce.android.aiassistant.core.chat.AssistantEvent
import com.woocommerce.android.aiassistant.core.chat.ChatRequest
import com.woocommerce.android.aiassistant.core.chat.ChatService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ScriptedHeadlessChatService(
    private val responses: List<List<AssistantEvent>>,
) : ChatService {
    private var calls = 0
    val requests = mutableListOf<ChatRequest>()

    override fun streamTurn(request: ChatRequest): Flow<AssistantEvent> = flow {
        requests += request
        val response = responses[minOf(calls, responses.lastIndex)]
        calls++
        response.forEach { emit(it) }
    }
}
