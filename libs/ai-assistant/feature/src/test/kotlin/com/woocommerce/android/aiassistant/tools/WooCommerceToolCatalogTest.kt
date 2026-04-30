package com.woocommerce.android.aiassistant.tools

import com.woocommerce.android.aiassistant.core.chat.AssistantToolHandler
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.tools.analytics.AnalyticsOrdersToolHandler
import com.woocommerce.android.aiassistant.tools.analytics.AnalyticsRevenueToolHandler
import com.woocommerce.android.aiassistant.tools.customers.CustomersListToolHandler
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock

class WooCommerceToolCatalogTest {

    private val allHandlers: Set<AssistantToolHandler> = setOf(
        OrdersListToolHandler(mock(), mock()),
        OrdersGetToolHandler(mock(), mock()),
        OrdersUpdateToolHandler(mock(), mock()),
        OrdersBulkUpdateToolHandler(mock(), mock()),
        ProductsListToolHandler(mock(), mock()),
        ProductsGetToolHandler(mock(), mock()),
        ProductsUpdateToolHandler(mock(), mock()),
        ProductsBulkUpdateToolHandler(mock(), mock()),
        ProductVariationsToolHandler(mock(), mock()),
        ProductVariationsUpdateToolHandler(mock(), mock()),
        AnalyticsRevenueToolHandler(mock(), mock()),
        AnalyticsOrdersToolHandler(mock(), mock()),
        ShowCardsToolHandler(),
        CustomersListToolHandler(mock()),
    )

    @Test
    fun `when all stub handlers are aggregated, then 14 expected tool names are present`() {
        val names = allHandlers.map { it.descriptor.name }

        assertThat(names).containsExactlyInAnyOrder(
            "orders_list",
            "orders_get",
            "orders_update",
            "orders_bulk_update",
            "products_list",
            "products_get",
            "products_update",
            "products_bulk_update",
            "product_variations_list",
            "product_variations_update",
            "analytics_revenue",
            "analytics_orders",
            "show_cards",
            "customers_list",
        )
    }

    @Test
    fun `when descriptors are inspected, then write tools are UNSAFE and read tools are SAFE`() {
        val byName = allHandlers.associateBy { it.descriptor.name }
        val writeToolNames = setOf(
            "orders_update",
            "orders_bulk_update",
            "products_update",
            "products_bulk_update",
            "product_variations_update",
        )

        writeToolNames.forEach { name ->
            assertThat(byName.getValue(name).descriptor.safetyLevel)
                .`as`("$name should be UNSAFE")
                .isEqualTo(ToolSafetyLevel.UNSAFE)
        }

        val readToolNames = byName.keys - writeToolNames
        readToolNames.forEach { name ->
            assertThat(byName.getValue(name).descriptor.safetyLevel)
                .`as`("$name should be SAFE")
                .isEqualTo(ToolSafetyLevel.SAFE)
        }
    }
}
