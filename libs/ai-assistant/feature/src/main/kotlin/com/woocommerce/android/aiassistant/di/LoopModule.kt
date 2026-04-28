package com.woocommerce.android.aiassistant.di

import com.woocommerce.android.aiassistant.core.chat.ChatService
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.loop.AgenticLoop
import com.woocommerce.android.aiassistant.core.loop.AgenticLoopImpl
import com.woocommerce.android.aiassistant.core.loop.ConservativeRetryPolicy
import com.woocommerce.android.aiassistant.core.loop.NoOpToolRegistry
import com.woocommerce.android.aiassistant.core.loop.RetryPolicy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object LoopModule {
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
    fun provideToolRegistry(): ToolRegistry = NoOpToolRegistry()

    @Provides
    @Singleton
    fun provideRetryPolicy(): RetryPolicy = ConservativeRetryPolicy
}
