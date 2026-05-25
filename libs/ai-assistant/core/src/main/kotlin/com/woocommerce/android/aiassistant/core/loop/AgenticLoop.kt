package com.woocommerce.android.aiassistant.core.loop

import com.woocommerce.android.aiassistant.core.history.ModelRequestHistory
import kotlinx.coroutines.flow.Flow

interface AgenticLoop {
    fun runTurn(
        conversationId: String,
        modelHistory: ModelRequestHistory,
        context: SessionContext,
    ): Flow<LoopEvent>
}
