package com.woocommerce.android.ui.products.addons

import com.woocommerce.android.util.UnitTestUtils.jsonFileAs
import com.woocommerce.android.util.UnitTestUtils.jsonFileToString
import org.wordpress.android.fluxc.model.WCOrderModel.LineItem
import org.wordpress.android.fluxc.model.WCProductModel

object AddonTestFixtures {
    val defaultWCOrderItemList: List<LineItem> by lazy {
        "mocks/order_items.json"
            .jsonFileAs(Array<LineItem>::class.java)
            ?.toList()
            ?: emptyList()
    }

    val defaultOrderAddonsStringList by lazy {
        listOf(
            "Topping ($3,00)",
            "Topping ($4,00)",
            "Topping ($3,00)",
            "Soda ($8,00)",
            "Delivery ($5,00)"
        )
    }

    val defaultWCProductModel by lazy {
        WCProductModel()
            .apply {
                attributes = "[]"
                status = "draft"
                metadata = "mocks/product_addon_metadata.json".jsonFileToString() ?: ""
            }
    }
}
