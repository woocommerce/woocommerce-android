package com.woocommerce.android.ui.products.addons

import com.woocommerce.android.model.Order.Item.Attribute
import com.woocommerce.android.model.toAppModel
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

    val defaultOrderAttributes by lazy {
        listOf(
            Attribute("Topping ($3,00)", "Peperoni"),
            Attribute("Topping ($4,00)", "Extra cheese"),
            Attribute("Topping ($3,00)", "Salami"),
            Attribute("Soda ($8,00)", "4"),
            Attribute("Delivery ($5,00)", "Yes")
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

    val defaultProductAddonList by lazy {
        defaultWCProductModel
            .toAppModel()
            .addons
    }
}
