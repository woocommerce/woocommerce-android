package com.woocommerce.android.aiassistant.tools

import com.woocommerce.android.aiassistant.core.chat.AssistantToolHandler
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.safety.WooCommerceConfirmationPreviewBuilder
import com.woocommerce.android.aiassistant.tools.analytics.AnalyticsOrdersToolHandler
import com.woocommerce.android.aiassistant.tools.customers.CustomersListToolHandler
import com.woocommerce.android.aiassistant.tools.handlers.ShowCardsToolHandler
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsResolver
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
        AnalyticsOrdersToolHandler(mock(), mock()),
        ShowCardsToolHandler(mock<ShowCardsResolver>()),
        CustomersListToolHandler(mock()),
    )

    @Test
    fun `when all handlers are aggregated, then 13 expected tool names are present`() {
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
            "analytics_orders",
            "show_cards",
            "customers_list",
        )
    }

    @Test
    fun `when descriptors are inspected, then write tools are UNSAFE and read tools are SAFE`() {
        val byName = allHandlers.associateBy { it.descriptor.name }
        assertThat(byName.keys).contains("analytics_orders")
        assertThat(byName.keys).doesNotContain("analytics_revenue")
        val writeToolNames = setOf(
            "orders_update",
            "orders_bulk_update",
            "products_update",
            "products_bulk_update",
            "product_variations_update",
        )
        val bulkWriteToolNames = setOf("orders_bulk_update", "products_bulk_update")

        writeToolNames.forEach { name ->
            assertThat(byName.getValue(name).descriptor.safetyLevel)
                .`as`("$name should be UNSAFE")
                .isEqualTo(ToolSafetyLevel.UNSAFE)
        }

        bulkWriteToolNames.forEach { name ->
            assertThat(byName.getValue(name).descriptor.safetyLevel)
                .`as`("$name bulk writes should be UNSAFE")
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
    fun `when descriptors are inspected, then no deletion tools are present`() {
        val forbiddenNameParts = listOf("delete", "remove", "destructive")
        val names = allHandlers.map { it.descriptor.name }

        forbiddenNameParts.forEach { forbidden ->
            assertThat(names.filter { it.contains(forbidden) })
                .`as`("catalog should not contain $forbidden tools")
                .isEmpty()
        }
    }

    @Test
    fun `when descriptors are retrieved, then write-tool descriptions include allowlisted fields and constraints`() {
        val byName = allHandlers.associateBy { it.descriptor.name }

        val ordersUpdate = byName.getValue("orders_update").descriptor.description
        assertThat(ordersUpdate).contains("status")
        assertThat(ordersUpdate).contains("customer emails")

        val ordersBulkUpdate = byName.getValue("orders_bulk_update").descriptor.description
        assertThat(ordersBulkUpdate).contains("status")
        assertThat(ordersBulkUpdate).contains("Bulk writes require confirmation")

        val ordersList = byName.getValue("orders_list").descriptor.description
        assertThat(ordersList).contains("analytics_orders")
        assertThat(ordersList).doesNotContain("analytics_revenue")

        val productsUpdate = byName.getValue("products_update").descriptor.description
        assertThat(productsUpdate).contains("regular_price")
        assertThat(productsUpdate).contains("stock_quantity")

        val productsBulkUpdate = byName.getValue("products_bulk_update").descriptor.description
        assertThat(productsBulkUpdate).contains("regular_price")
        assertThat(productsBulkUpdate).contains("Bulk writes require confirmation")
    }

    @Test
    fun `when descriptors are inspected, then every UNSAFE tool has a dedicated confirmation preview`() {
        val confirmationPreviewBuilder = WooCommerceConfirmationPreviewBuilder()
        val unsafeToolNames = allHandlers
            .map { it.descriptor }
            .filter { it.safetyLevel == ToolSafetyLevel.UNSAFE }
            .map { it.name }

        assertThat(unsafeToolNames).allMatch(confirmationPreviewBuilder::supportsDedicatedPreview)
    }
}
