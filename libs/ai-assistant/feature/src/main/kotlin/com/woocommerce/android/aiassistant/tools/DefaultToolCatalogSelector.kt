package com.woocommerce.android.aiassistant.tools

import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.loop.CatalogSnapshot
import com.woocommerce.android.aiassistant.core.loop.ToolCatalogSelector
import com.woocommerce.android.aiassistant.core.loop.ToolScope

class DefaultToolCatalogSelector : ToolCatalogSelector {

    override fun select(scope: ToolScope, fullRegistry: List<ToolDescriptor>): CatalogSnapshot {
        val allowedToolNames = when (scope) {
            ToolScope.GLOBAL -> null
            ToolScope.ORDERS -> ORDERS_TOOL_NAMES
            ToolScope.PRODUCTS -> PRODUCTS_TOOL_NAMES
            ToolScope.ANALYTICS -> ANALYTICS_TOOL_NAMES
        }

        val tools = if (allowedToolNames == null) {
            fullRegistry
        } else {
            fullRegistry.filter { it.name in allowedToolNames }
        }

        return CatalogSnapshot(scope = scope, tools = tools)
    }

    private companion object {
        val ORDERS_TOOL_NAMES = setOf(
            "orders_list",
            "orders_get",
            "orders_update",
            "orders_bulk_update",
            "analytics_orders",
            "show_cards",
        )

        val PRODUCTS_TOOL_NAMES = setOf(
            "products_list",
            "products_get",
            "products_update",
            "products_bulk_update",
            "product_variations_list",
            "product_variations_update",
            "analytics_revenue",
            "show_cards",
        )

        val ANALYTICS_TOOL_NAMES = setOf(
            "analytics_revenue",
            "analytics_orders",
            "orders_list",
            "products_list",
            "show_cards",
        )
    }
}
