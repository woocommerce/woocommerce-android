package com.woocommerce.android.aiassistant.di

import com.woocommerce.android.aiassistant.core.chat.AssistantToolHandler
import com.woocommerce.android.aiassistant.core.chat.ChatService
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.loop.AgenticLoop
import com.woocommerce.android.aiassistant.core.loop.AgenticLoopImpl
import com.woocommerce.android.aiassistant.core.loop.ConservativeRetryPolicy
import com.woocommerce.android.aiassistant.core.loop.HistoryBudgeter
import com.woocommerce.android.aiassistant.core.loop.SlidingWindowHistoryBudgeter
import com.woocommerce.android.aiassistant.core.loop.RetryPolicy
import com.woocommerce.android.aiassistant.core.loop.ToolCatalogSelector
import com.woocommerce.android.aiassistant.tools.DefaultToolCatalogSelector
import com.woocommerce.android.aiassistant.tools.WooCommerceToolRegistry
import com.woocommerce.android.aiassistant.tools.handlers.AnalyticsOrdersToolHandler
import com.woocommerce.android.aiassistant.tools.handlers.AnalyticsRevenueToolHandler
import com.woocommerce.android.aiassistant.tools.handlers.CustomersListToolHandler
import com.woocommerce.android.aiassistant.tools.handlers.OrdersBulkUpdateToolHandler
import com.woocommerce.android.aiassistant.tools.handlers.OrdersUpdateToolHandler
import com.woocommerce.android.aiassistant.tools.handlers.ProductVariationsListToolHandler
import com.woocommerce.android.aiassistant.tools.handlers.ProductsBulkUpdateToolHandler
import com.woocommerce.android.aiassistant.tools.handlers.ProductsGetToolHandler
import com.woocommerce.android.aiassistant.tools.handlers.ProductsListToolHandler
import com.woocommerce.android.aiassistant.tools.handlers.ProductsUpdateToolHandler
import com.woocommerce.android.aiassistant.tools.handlers.ShowCardsToolHandler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AiAssistantModule {
    companion object {
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
        fun provideAgenticLoop(
            chatService: ChatService,
            toolRegistry: ToolRegistry,
            retryPolicy: RetryPolicy,
            historyBudgeter: HistoryBudgeter,
            @AiAssistantJson json: Json,
        ): AgenticLoop = AgenticLoopImpl(chatService, toolRegistry, retryPolicy, historyBudgeter, json)

        @Provides
        @Singleton
        fun provideToolCatalogSelector(): ToolCatalogSelector = DefaultToolCatalogSelector()

        @Provides
        @Singleton
        fun provideRetryPolicy(): RetryPolicy = ConservativeRetryPolicy

        @Provides
        @Singleton
        fun provideHistoryBudgeter(): HistoryBudgeter = SlidingWindowHistoryBudgeter(windowSize = 10)
    }
}
