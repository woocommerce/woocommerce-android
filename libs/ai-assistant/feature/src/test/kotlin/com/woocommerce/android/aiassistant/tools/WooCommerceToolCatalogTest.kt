package com.woocommerce.android.aiassistant.tools

import com.woocommerce.android.aiassistant.core.chat.AssistantToolHandler
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.tools.handlers.AnalyticsOrdersToolHandler
import com.woocommerce.android.aiassistant.tools.handlers.AnalyticsRevenueToolHandler
import com.woocommerce.android.aiassistant.tools.handlers.CustomersListToolHandler
import com.woocommerce.android.aiassistant.tools.handlers.OrdersBulkUpdateToolHandler
import com.woocommerce.android.aiassistant.tools.handlers.ProductsBulkUpdateToolHandler
import com.woocommerce.android.aiassistant.tools.handlers.ShowCardsToolHandler
import com.woocommerce.android.aiassistant.tools.orders.OrdersGetToolHandler
import com.woocommerce.android.aiassistant.tools.orders.OrdersListToolHandler
import com.woocommerce.android.aiassistant.tools.orders.OrdersUpdateToolHandler
import com.woocommerce.android.aiassistant.tools.products.ProductVariationsToolHandler
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
        OrdersBulkUpdateToolHandler(),
        ProductsListToolHandler(mock(), mock()),
        ProductsGetToolHandler(mock(), mock()),
        ProductsUpdateToolHandler(mock(), mock()),
        ProductsBulkUpdateToolHandler(),
        ProductVariationsToolHandler(mock(), mock()),
        AnalyticsRevenueToolHandler(),
        AnalyticsOrdersToolHandler(),
        ShowCardsToolHandler(),
        CustomersListToolHandler(),
    )

    @Test
    fun `when all stub handlers are aggregated, then 13 expected tool names are present`() {
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
            "analytics_revenue",
            "analytics_orders",
            "show_cards",
            "customers_list",
        )
    }

    @Test
    fun `when descriptors are inspected, then write tools are UNSAFE and read tools are SAFE`() {
        val byName = allHandlers.associateBy { it.descriptor.name }
        val writeToolNames = setOf("orders_update", "orders_bulk_update", "products_update", "products_bulk_update")

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

    @Test
    fun `when descriptors are retrieved, then write-tool descriptions include allowlisted fields and constraints`() {
        val byName = allHandlers.associateBy { it.descriptor.name }

        val ordersUpdate = byName.getValue("orders_update").descriptor.description
        assertThat(ordersUpdate).contains("status")
        assertThat(ordersUpdate).contains("refund")

        val ordersBulkUpdate = byName.getValue("orders_bulk_update").descriptor.description
        assertThat(ordersBulkUpdate).contains("status")
        assertThat(ordersBulkUpdate).contains("Bulk writes require confirmation")

        val productsUpdate = byName.getValue("products_update").descriptor.description
        assertThat(productsUpdate).contains("regular_price")
        assertThat(productsUpdate).contains("stock_quantity")

        val productsBulkUpdate = byName.getValue("products_bulk_update").descriptor.description
        assertThat(productsBulkUpdate).contains("regular_price")
        assertThat(productsBulkUpdate).contains("Bulk writes require confirmation")
    }
}
