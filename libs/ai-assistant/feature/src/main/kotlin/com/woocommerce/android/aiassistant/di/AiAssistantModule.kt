package com.woocommerce.android.aiassistant.di

import com.woocommerce.android.aiassistant.core.chat.AssistantToolHandler
import com.woocommerce.android.aiassistant.core.chat.ChatService
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.loop.AgenticLoop
import com.woocommerce.android.aiassistant.core.loop.AgenticLoopImpl
import com.woocommerce.android.aiassistant.core.loop.ConservativeRetryPolicy
import com.woocommerce.android.aiassistant.core.loop.RetryPolicy
import com.woocommerce.android.aiassistant.core.loop.ToolCatalogSelector
import com.woocommerce.android.aiassistant.tools.DefaultToolCatalogSelector
import com.woocommerce.android.aiassistant.tools.WooCommerceToolRegistry
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
    }

    @Provides
    @Singleton
    fun provideAgenticLoop(
        chatService: ChatService,
        toolRegistry: ToolRegistry,
        retryPolicy: RetryPolicy,
        @AiAssistantJson json: Json,
    ): AgenticLoop = AgenticLoopImpl(chatService, toolRegistry, retryPolicy, json)

    @Provides
    @Singleton
    fun provideToolRegistry(
        handlers: Map<String, AssistantToolHandler>,
    ): ToolRegistry = WooCommerceToolRegistry(handlers)

    @Provides
    @Singleton
    fun provideToolCatalogSelector(): ToolCatalogSelector = DefaultToolCatalogSelector()

    @Provides
    @Singleton
    fun provideToolHandlers(): Map<String, AssistantToolHandler> = emptyMap()

    @Provides
    @Singleton
    fun provideRetryPolicy(): RetryPolicy = ConservativeRetryPolicy
}
