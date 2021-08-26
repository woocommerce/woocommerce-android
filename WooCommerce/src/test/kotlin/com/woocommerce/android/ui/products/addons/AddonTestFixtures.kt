package com.woocommerce.android.ui.products.addons

import com.woocommerce.android.model.Order.Item.Attribute
import com.woocommerce.android.model.ProductAddon
import com.woocommerce.android.model.ProductAddon.PriceType.FlatFee
import com.woocommerce.android.model.ProductAddon.PriceType.QuantityBased
import com.woocommerce.android.model.ProductAddonOption
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.util.UnitTestUtils.jsonFileAs
import com.woocommerce.android.util.UnitTestUtils.jsonFileToString
import org.wordpress.android.fluxc.model.WCOrderModel.LineItem
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.persistence.entity.AddonEntity
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.*
import org.wordpress.android.fluxc.persistence.entity.AddonEntity.Type.*
import org.wordpress.android.fluxc.persistence.entity.AddonOptionEntity
import org.wordpress.android.fluxc.persistence.entity.AddonWithOptions

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

    val defaultAddonWithOptionsList by lazy {
        listOf(
            AddonWithOptions(
                defaultAddonEntity.copy(
                    name = "Topping",
                    description = "Pizza topping",
                    priceType = PriceType.FlatFee
                ),
                options = listOf(
                    emptyAddonOptions.copy(
                        label = "Peperoni",
                        price = "3",
                        image = "",
                        priceType = PriceType.FlatFee
                    ),
                    emptyAddonOptions.copy(
                        label = "Extra cheese",
                        price = "4",
                        image = "",
                        priceType = PriceType.FlatFee
                    ),
                    emptyAddonOptions.copy(
                        label = "Salami",
                        price = "3",
                        image = "",
                        priceType = PriceType.FlatFee
                    ),
                    emptyAddonOptions.copy(
                        label = "Ham",
                        price = "3",
                        image = "",
                        priceType = PriceType.FlatFee
                    )
                )
            )
        )
    }

    val defaultOrderedAddonList by lazy {
        listOf(
            emptyProductAddon.copy(
                name = "Topping",
                description = "Pizza topping",
                priceType = FlatFee,
                rawOptions = listOf(
                    ProductAddonOption(
                        priceType = FlatFee,
                        label = "Peperoni",
                        price = "3",
                        image = ""
                    )
                )
            ),
            emptyProductAddon.copy(
                name = "Topping",
                description = "Pizza topping",
                priceType = FlatFee,
                rawOptions = listOf(
                    ProductAddonOption(
                        priceType = FlatFee,
                        label = "Extra cheese",
                        price = "4",
                        image = ""
                    )
                )
            ),
            emptyProductAddon.copy(
                name = "Topping",
                description = "Pizza topping",
                priceType = FlatFee,
                rawOptions = listOf(
                    ProductAddonOption(
                        priceType = FlatFee,
                        label = "Salami",
                        price = "3",
                        image = ""
                    )
                )
            ),
            emptyProductAddon.copy(
                name = "Soda",
                priceType = FlatFee,
                price = "2",
                rawOptions = listOf(
                    ProductAddonOption(
                        priceType = FlatFee,
                        label = "4",
                        price = "$8,00",
                        image = ""
                    )
                )
            ),
            emptyProductAddon.copy(
                name = "Delivery",
                priceType = FlatFee,
                rawOptions = listOf(
                    ProductAddonOption(
                        priceType = FlatFee,
                        label = "Yes",
                        price = "5",
                        image = ""
                    )
                )
            )
        )
    }

    val listWithSingleAddonAndTwoValidOptions by lazy {
        listOf(
            emptyProductAddon.copy(
                name = "test-name",
                priceType = FlatFee,
                rawOptions = listOf(
                    ProductAddonOption(
                        QuantityBased,
                        "test-label-2",
                        "test-price-2",
                        "test-image-2"
                    ),
                    ProductAddonOption(
                        FlatFee,
                        "test-label",
                        "test-price",
                        "test-image"
                    ),
                )
            )
        )
    }

    val emptyProductAddon by lazy {
        ProductAddon(
            name = "",
            required = false,
            description = "",
            descriptionEnabled = false,
            max = 0,
            min = 0,
            position = 0,
            adjustPrice = false,
            titleFormat = null,
            restrictionsType = null,
            priceType = null,
            type = null,
            display = null,
            price = "",
            rawOptions = listOf()
        )
    }

    val defaultAddonEntity by lazy {
        AddonEntity(
            name = "",
            required = false,
            description = "",
            descriptionEnabled = true,
            max = 0,
            min = 0,
            position = 0,
            priceAdjusted = false,
            titleFormat = TitleFormat.Label,
            restrictions = Restrictions.AnyText,
            priceType = PriceType.FlatFee,
            type = Checkbox,
            display = Display.Select,
            price = ""
        )
    }

    val emptyAddonOptions by lazy {
        AddonOptionEntity(
            label = "",
            price = "",
            image = "",
            priceType = PriceType.FlatFee,
            addonLocalId = 0
        )
    }
}
