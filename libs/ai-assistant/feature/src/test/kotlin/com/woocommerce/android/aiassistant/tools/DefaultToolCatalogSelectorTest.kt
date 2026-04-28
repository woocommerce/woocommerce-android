package com.woocommerce.android.aiassistant.tools

import com.woocommerce.android.aiassistant.core.loop.ToolScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DefaultToolCatalogSelectorTest {

    private val selector = DefaultToolCatalogSelector()
    private val catalog = WooCommerceToolRegistry.CATALOG

    @Test
    fun `when GLOBAL scope is selected, then all 13 tools are returned in catalog order`() {
        val snapshot = selector.select(ToolScope.GLOBAL, catalog)

        assertThat(snapshot.scope).isEqualTo(ToolScope.GLOBAL)
        assertThat(snapshot.tools.map { it.name }).containsExactly(
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
    fun `when ORDERS scope is selected, then only order tools are returned and product-only tools are excluded`() {
        val snapshot = selector.select(ToolScope.ORDERS, catalog)

        assertThat(snapshot.scope).isEqualTo(ToolScope.ORDERS)
        assertThat(snapshot.tools.map { it.name }).containsExactly(
            "orders_list",
            "orders_get",
            "orders_update",
            "orders_bulk_update",
            "analytics_orders",
            "show_cards",
        )
        assertThat(snapshot.tools.map { it.name }).doesNotContain(
            "products_list",
            "products_get",
            "products_update",
            "products_bulk_update",
            "product_variations_list",
            "analytics_revenue",
            "customers_list",
        )
    }

    @Test
    fun `when PRODUCTS scope is selected, then only product tools are returned`() {
        val snapshot = selector.select(ToolScope.PRODUCTS, catalog)

        assertThat(snapshot.scope).isEqualTo(ToolScope.PRODUCTS)
        assertThat(snapshot.tools.map { it.name }).containsExactly(
            "products_list",
            "products_get",
            "products_update",
            "products_bulk_update",
            "product_variations_list",
            "analytics_revenue",
            "show_cards",
        )
    }

    @Test
    fun `when ANALYTICS scope is selected, then only analytics tools are returned`() {
        val snapshot = selector.select(ToolScope.ANALYTICS, catalog)

        assertThat(snapshot.scope).isEqualTo(ToolScope.ANALYTICS)
        assertThat(snapshot.tools.map { it.name }).containsExactly(
            "orders_list",
            "products_list",
            "analytics_revenue",
            "analytics_orders",
            "show_cards",
        )
    }

    @Test
    fun `given same scope and catalog, when select is called repeatedly, then equal snapshots are returned`() {
        val firstSnapshot = selector.select(ToolScope.ORDERS, catalog)
        val secondSnapshot = selector.select(ToolScope.ORDERS, catalog)

        assertThat(firstSnapshot).isEqualTo(secondSnapshot)
    }
}
