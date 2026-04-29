package com.woocommerce.android.aiassistant.di

import com.woocommerce.android.aiassistant.core.chat.AssistantToolHandler
import com.woocommerce.android.aiassistant.core.chat.ChatService
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.loop.AgenticLoop
import com.woocommerce.android.aiassistant.core.loop.AgenticLoopImpl
import com.woocommerce.android.aiassistant.core.loop.ConservativeRetryPolicy
import com.woocommerce.android.aiassistant.core.loop.HistoryBudgeter
import com.woocommerce.android.aiassistant.core.loop.RetryPolicy
import com.woocommerce.android.aiassistant.core.loop.SlidingWindowHistoryBudgeter
import com.woocommerce.android.aiassistant.core.loop.ToolCatalogSelector
import com.woocommerce.android.aiassistant.tools.DefaultToolCatalogSelector
import com.woocommerce.android.aiassistant.tools.WooCommerceToolRegistry
import com.woocommerce.android.aiassistant.tools.handlers.AnalyticsOrdersToolHandler
import com.woocommerce.android.aiassistant.tools.handlers.AnalyticsRevenueToolHandler
import com.woocommerce.android.aiassistant.tools.handlers.CustomersListToolHandler
import com.woocommerce.android.aiassistant.tools.handlers.OrdersBulkUpdateToolHandler
import com.woocommerce.android.aiassistant.tools.handlers.OrdersGetToolHandler
import com.woocommerce.android.aiassistant.tools.handlers.OrdersListToolHandler
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

    @Binds
    @Singleton
    abstract fun bindToolRegistry(impl: WooCommerceToolRegistry): ToolRegistry

    @Binds
    @IntoSet
    abstract fun bindOrdersListHandler(impl: OrdersListToolHandler): AssistantToolHandler

    @Binds
    @IntoSet
    abstract fun bindOrdersGetHandler(impl: OrdersGetToolHandler): AssistantToolHandler

    @Binds
    @IntoSet
    abstract fun bindOrdersUpdateHandler(impl: OrdersUpdateToolHandler): AssistantToolHandler

    @Binds
    @IntoSet
    abstract fun bindOrdersBulkUpdateHandler(impl: OrdersBulkUpdateToolHandler): AssistantToolHandler

    @Binds
    @IntoSet
    abstract fun bindProductsListHandler(impl: ProductsListToolHandler): AssistantToolHandler

    @Binds
    @IntoSet
    abstract fun bindProductsGetHandler(impl: ProductsGetToolHandler): AssistantToolHandler

    @Binds
    @IntoSet
    abstract fun bindProductsUpdateHandler(impl: ProductsUpdateToolHandler): AssistantToolHandler

    @Binds
    @IntoSet
    abstract fun bindProductsBulkUpdateHandler(impl: ProductsBulkUpdateToolHandler): AssistantToolHandler

    @Binds
    @IntoSet
    abstract fun bindProductVariationsListHandler(impl: ProductVariationsListToolHandler): AssistantToolHandler

    @Binds
    @IntoSet
    abstract fun bindAnalyticsRevenueHandler(impl: AnalyticsRevenueToolHandler): AssistantToolHandler

    @Binds
    @IntoSet
    abstract fun bindAnalyticsOrdersHandler(impl: AnalyticsOrdersToolHandler): AssistantToolHandler

    @Binds
    @IntoSet
    abstract fun bindShowCardsHandler(impl: ShowCardsToolHandler): AssistantToolHandler

    @Binds
    @IntoSet
    abstract fun bindCustomersListHandler(impl: CustomersListToolHandler): AssistantToolHandler

    companion object {
        @Provides
        @Singleton
        @AiAssistantJson
        fun provideAiAssistantJson(): Json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

        @Provides
        @Singleton
        fun provideHistoryBudgeter(): HistoryBudgeter = SlidingWindowHistoryBudgeter()

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
    }
}
