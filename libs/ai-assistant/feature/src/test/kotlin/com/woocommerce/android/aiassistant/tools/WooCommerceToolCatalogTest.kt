package com.woocommerce.android.aiassistant.tools

import com.woocommerce.android.aiassistant.core.chat.AssistantToolHandler
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.safety.WooCommerceConfirmationPreviewBuilder
import com.woocommerce.android.aiassistant.tools.analytics.AnalyticsOrdersToolHandler
import com.woocommerce.android.aiassistant.tools.analytics.AnalyticsRevenueToolHandler
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
        AnalyticsRevenueToolHandler(mock(), mock()),
        AnalyticsOrdersToolHandler(mock(), mock()),
        ShowCardsToolHandler(mock<ShowCardsResolver>()),
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
    fun `when descriptors are inspected, then tool expansion issue adds no new tools`() {
        val names = allHandlers.map { it.descriptor.name }
        val modelVisibleDescriptions = allHandlers.joinToString(separator = "\n") { handler ->
            handler.descriptor.description
        }

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
        assertThat(names).doesNotContain(
            "order_notes_create",
            "order_notes_list",
            "analytics_top_products",
            "analytics_top_customers",
            "analytics_customers",
            "customers_get",
            "product_variations_bulk_update",
        )
        assertThat(modelVisibleDescriptions).doesNotContain(
            "order_notes_create",
            "order_notes_list",
            "analytics_top_products",
            "analytics_top_customers",
            "analytics_customers",
            "customers_get",
            "product_variations_bulk_update",
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
