package com.woocommerce.android.ui.products.addons

import com.google.gson.Gson
import com.woocommerce.android.model.Order.Item.Attribute
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.util.UnitTestUtils.jsonFileAs
import com.woocommerce.android.util.UnitTestUtils.jsonFileToString
import org.wordpress.android.fluxc.domain.Addon
import org.wordpress.android.fluxc.domain.Addon.HasAdjustablePrice.Price.Adjusted.*
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.addons.RemoteAddonDto
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.model.metadata.get
import org.wordpress.android.fluxc.model.order.LineItem
import org.wordpress.android.fluxc.network.rest.wpcom.wc.addons.mappers.RemoteAddonMapper

object AddonTestFixtures {
    val defaultOrderModel: OrderEntity by lazy {
        OrderTestUtils.generateOrder()
            .copy(lineItems = "mocks/order_items.json".jsonFileToString() ?: "")
    }

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

    val orderAttributesWithPercentageBasedItem by lazy {
        listOf(
            Attribute("Topping ($3,00)", "Peperoni"),
            Attribute("Topping ($4,00)", "Extra cheese"),
            Attribute("Topping ($3,00)", "Salami"),
            Attribute("Soda ($8,00)", "4"),
            Attribute("Delivery ($123,00)", "Test")
        )
    }

    val defaultWCProductModel by lazy {
        WCProductModel()
            .apply {
                attributes = "[]"
                status = "draft"
            }
    }

    val defaultProductAddonList by lazy {
        "mocks/product_addon_metadata.json".jsonFileToString()
            ?.let { Gson().fromJson(it, Array<WCMetaData>::class.java).toList() }
            ?.let { it[WCMetaData.AddOnsMetadataKeys.ADDONS_METADATA_KEY] }
            ?.let { RemoteAddonDto.fromMetaDataValue(it.value) }
            ?.map { RemoteAddonMapper.toDomain(it) }
            .orEmpty()
    }

    val defaultAddonsList by lazy {
        listOf(
            Addon.Checkbox(
                name = "Topping",
                description = "Pizza topping",
                position = 0,
                required = false,
                titleFormat = Addon.TitleFormat.Label,
                options = listOf(
                    emptyAddonOptions.copy(
                        label = "Peperoni",
                        price = Addon.HasAdjustablePrice.Price.Adjusted(
                            value = "3",
                            priceType = PriceType.FlatFee
                        )
                    ),
                    emptyAddonOptions.copy(
                        label = "Extra cheese",
                        price = Addon.HasAdjustablePrice.Price.Adjusted(
                            value = "4",
                            priceType = PriceType.FlatFee
                        )
                    ),
                    emptyAddonOptions.copy(
                        label = "Salami",
                        price = Addon.HasAdjustablePrice.Price.Adjusted(
                            value = "3",
                            priceType = PriceType.FlatFee
                        )
                    ),
                    emptyAddonOptions.copy(
                        label = "Ham",
                        price = Addon.HasAdjustablePrice.Price.Adjusted(
                            value = "3",
                            priceType = PriceType.FlatFee
                        )
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
                options = listOf(
                    Addon.HasOptions.Option(
                        label = "Peperoni",
                        price = Addon.HasAdjustablePrice.Price.Adjusted(
                            priceType = PriceType.FlatFee,
                            value = "3"
                        ),
                        image = ""
                    )
                )
            ),
            emptyProductAddon.copy(
                name = "Topping",
                description = "Pizza topping",
                options = listOf(
                    Addon.HasOptions.Option(
                        label = "Extra cheese",
                        price = Addon.HasAdjustablePrice.Price.Adjusted(
                            priceType = PriceType.FlatFee,
                            value = "4"
                        ),
                        image = ""
                    )
                )
            ),
            emptyProductAddon.copy(
                name = "Topping",
                description = "Pizza topping",
                options = listOf(
                    Addon.HasOptions.Option(
                        label = "Salami",
                        price = Addon.HasAdjustablePrice.Price.Adjusted(
                            priceType = PriceType.FlatFee,
                            value = "3"
                        ),
                        image = ""
                    )
                ),
            ),
            emptyProductAddon.copy(
                name = "Soda",
                options = listOf(
                    Addon.HasOptions.Option(
                        label = "4",
                        price = Addon.HasAdjustablePrice.Price.Adjusted(
                            priceType = PriceType.FlatFee,
                            value = "$8,00",
                        ),
                        image = ""
                    )
                ),
            ),
            emptyProductAddon.copy(
                name = "Delivery",
                options = listOf(
                    Addon.HasOptions.Option(
                        label = "Yes",
                        price = Addon.HasAdjustablePrice.Price.Adjusted(
                            priceType = PriceType.FlatFee,
                            value = "5",
                        ),
                        image = ""
                    )
                ),
            )
        )
    }

    val orderedAddonWithPercentageBasedDeliveryOptionList by lazy {
        listOf(
            emptyProductAddon.copy(
                name = "Topping",
                description = "Pizza topping",
                options = listOf(
                    Addon.HasOptions.Option(
                        label = "Peperoni",
                        price = Addon.HasAdjustablePrice.Price.Adjusted(
                            priceType = PriceType.FlatFee,
                            value = "3"
                        ),
                        image = ""
                    )
                )
            ),
            emptyProductAddon.copy(
                name = "Topping",
                description = "Pizza topping",
                options = listOf(
                    Addon.HasOptions.Option(
                        label = "Extra cheese",
                        price = Addon.HasAdjustablePrice.Price.Adjusted(
                            priceType = PriceType.FlatFee,
                            value = "4"
                        ),
                        image = ""
                    )
                )
            ),
            emptyProductAddon.copy(
                name = "Topping",
                description = "Pizza topping",
                options = listOf(
                    Addon.HasOptions.Option(
                        label = "Salami",
                        price = Addon.HasAdjustablePrice.Price.Adjusted(
                            priceType = PriceType.FlatFee,
                            value = "3"
                        ),
                        image = ""
                    )
                ),
            ),
            emptyProductAddon.copy(
                name = "Soda",
                options = listOf(
                    Addon.HasOptions.Option(
                        label = "4",
                        price = Addon.HasAdjustablePrice.Price.Adjusted(
                            priceType = PriceType.FlatFee,
                            value = "$8,00",
                        ),
                        image = ""
                    )
                ),
            ),
            emptyProductAddon.copy(
                name = "Delivery",
                options = listOf(
                    Addon.HasOptions.Option(
                        label = "Test",
                        price = Addon.HasAdjustablePrice.Price.Adjusted(
                            priceType = PriceType.FlatFee,
                            value = "$123,00",
                        ),
                        image = ""
                    )
                ),
            )
        )
    }

    val listWithSingleAddonAndTwoValidOptions by lazy {
        listOf(
            emptyProductAddon.copy(
                name = "test-name",
                options = listOf(
                    Addon.HasOptions.Option(
                        price = Addon.HasAdjustablePrice.Price.Adjusted(
                            priceType = PriceType.FlatFee,
                            value = "test-price-2"
                        ),
                        label = "test-label-2",
                        image = "test-image-2"
                    ),
                    Addon.HasOptions.Option(
                        price = Addon.HasAdjustablePrice.Price.Adjusted(
                            priceType = PriceType.FlatFee,
                            value = "test-price"
                        ),
                        label = "test-label",
                        image = "test-image"
                    )
                )
            )
        )
    }

    val emptyProductAddon by lazy {
        Addon.Checkbox(
            name = "",
            required = false,
            description = null,
            position = 0,
            titleFormat = Addon.TitleFormat.Label,
            options = emptyList()
        )
    }

    val emptyAddonOptions by lazy {
        Addon.HasOptions.Option(
            label = "",
            image = "",
            price = Addon.HasAdjustablePrice.Price.Adjusted(
                priceType = PriceType.FlatFee,
                value = ""
            )
        )
    }
}
