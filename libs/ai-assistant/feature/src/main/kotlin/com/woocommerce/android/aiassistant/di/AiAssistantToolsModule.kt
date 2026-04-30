package com.woocommerce.android.aiassistant.di

import com.woocommerce.android.aiassistant.core.chat.AssistantToolHandler
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.tools.WooCommerceToolRegistry
import com.woocommerce.android.aiassistant.tools.analytics.AnalyticsOrdersToolHandler
import com.woocommerce.android.aiassistant.tools.analytics.AnalyticsRevenueToolHandler
import com.woocommerce.android.aiassistant.tools.handlers.CustomersListToolHandler
import com.woocommerce.android.aiassistant.tools.handlers.ShowCardsToolHandler
import com.woocommerce.android.aiassistant.tools.orders.OrdersBulkUpdateToolHandler
import com.woocommerce.android.aiassistant.tools.orders.OrdersGetToolHandler
import com.woocommerce.android.aiassistant.tools.orders.OrdersListToolHandler
import com.woocommerce.android.aiassistant.tools.orders.OrdersUpdateToolHandler
import com.woocommerce.android.aiassistant.tools.products.ProductVariationsToolHandler
import com.woocommerce.android.aiassistant.tools.products.ProductVariationsUpdateToolHandler
import com.woocommerce.android.aiassistant.tools.products.ProductsBulkUpdateToolHandler
import com.woocommerce.android.aiassistant.tools.products.ProductsGetToolHandler
import com.woocommerce.android.aiassistant.tools.products.ProductsListToolHandler
import com.woocommerce.android.aiassistant.tools.products.ProductsUpdateToolHandler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AiAssistantToolsModule {
    @Binds
    @IntoSet
    abstract fun bindOrdersListHandler(handler: OrdersListToolHandler): AssistantToolHandler

    @Binds
    @IntoSet
    abstract fun bindOrdersGetHandler(handler: OrdersGetToolHandler): AssistantToolHandler

    @Binds
    @Singleton
    abstract fun bindToolRegistry(impl: WooCommerceToolRegistry): ToolRegistry

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
    abstract fun bindProductVariationsListHandler(impl: ProductVariationsToolHandler): AssistantToolHandler

    @Binds
    @IntoSet
    abstract fun bindProductVariationsUpdateHandler(impl: ProductVariationsUpdateToolHandler): AssistantToolHandler

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
}
