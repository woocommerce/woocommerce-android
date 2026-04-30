package com.woocommerce.android.aiassistant.di

import com.woocommerce.android.aiassistant.core.chat.ChatService
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.loop.AgenticLoop
import com.woocommerce.android.aiassistant.core.loop.AgenticLoopImpl
import com.woocommerce.android.aiassistant.core.loop.ConservativeRetryPolicy
import com.woocommerce.android.aiassistant.core.loop.HistoryBudgeter
import com.woocommerce.android.aiassistant.core.loop.RetryPolicy
import com.woocommerce.android.aiassistant.core.loop.SlidingWindowHistoryBudgeter
import com.woocommerce.android.aiassistant.core.loop.ToolCatalogSelector
import com.woocommerce.android.aiassistant.core.safety.SafetyOrchestrator
import com.woocommerce.android.aiassistant.core.safety.SafetyOrchestratorImpl
import com.woocommerce.android.aiassistant.tools.DefaultToolCatalogSelector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AiAssistantModule {
    @Provides
    @Singleton
    @AiAssistantJson
    fun provideAiAssistantJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = true
    }

    @Provides
    @Singleton
    @Suppress("LongParameterList")
    fun provideAgenticLoop(
        chatService: ChatService,
        toolRegistry: ToolRegistry,
        retryPolicy: RetryPolicy,
        historyBudgeter: HistoryBudgeter,
        safetyOrchestrator: SafetyOrchestrator,
        @AiAssistantJson json: Json,
    ): AgenticLoop = AgenticLoopImpl(
        chatService,
        toolRegistry,
        retryPolicy,
        historyBudgeter,
        safetyOrchestrator,
        json,
    )

    @Provides
    @Singleton
    fun provideToolCatalogSelector(): ToolCatalogSelector = DefaultToolCatalogSelector()

    @Provides
    @Singleton
    fun provideRetryPolicy(): RetryPolicy = ConservativeRetryPolicy

    @Provides
    @Singleton
    fun provideHistoryBudgeter(): HistoryBudgeter = SlidingWindowHistoryBudgeter(windowSize = 10)

    @Provides
    @Singleton
    fun provideSafetyOrchestrator(): SafetyOrchestrator = SafetyOrchestratorImpl()
}
