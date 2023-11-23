package com.woocommerce.android.ui.orders.creation

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.configuration.ConfigurationType
import com.woocommerce.android.ui.orders.creation.configuration.OptionalRule
import com.woocommerce.android.ui.orders.creation.configuration.ProductConfiguration
import com.woocommerce.android.ui.orders.creation.configuration.QuantityRule
import com.woocommerce.android.ui.orders.creation.configuration.VariableProductRule
import com.woocommerce.android.ui.products.GetBundledProducts
import kotlinx.coroutines.flow.first
import org.wordpress.android.fluxc.utils.putIfNotNull
import javax.inject.Inject

class ListItemMapper @Inject constructor(
    private val getBundledProducts: GetBundledProducts,
    private val gson: Gson
) {

    private val mapType = object : TypeToken<Map<String, JsonElement>>() {}.type
    suspend fun toRawListItem(item: Order.Item): Map<String, Any> {
        return buildMap {
            item.itemId.takeIf { it != 0L }?.let { put("id", it) }
            put("name", item.name)
            put("product_id", item.productId)
            put("variation_id", item.variationId)
            put("quantity", item.quantity.toString())
            put("subtotal", item.subtotal.toString())
            put("total", item.total.toString())
            item.configuration?.let {
                getConfiguration(item.productId, it)?.let { keyConfigurationPair ->
                    put(keyConfigurationPair.first, keyConfigurationPair.second)
                }
            }
        }
    }

    private suspend fun getConfiguration(
        productId: Long,
        configuration: ProductConfiguration
    ): Pair<String, MutableList<Map<String, Any>>>? {
        return when (configuration.configurationType) {
            ConfigurationType.BUNDLE -> {
                val result = mutableListOf<Map<String, Any>>()
                val childrenProducts = getBundledProducts(productId).first().associateBy { it.id }
                configuration.childrenConfiguration?.let { idConfigurationPair ->
                    for (childConfiguration in idConfigurationPair) {
                        val item = childrenProducts[childConfiguration.key] ?: continue
                        val rawChildConfiguration = buildMap {
                            put("bundled_item_id", item.id)
                            put("product_id", item.bundledProductId)
                            childConfiguration.value.forEach { rule ->
                                when (rule.key) {
                                    QuantityRule.KEY -> putIfNotNull("quantity" to rule.value)
                                    OptionalRule.KEY -> putIfNotNull(
                                        "optional_selected" to rule.value?.toBooleanStrictOrNull()
                                    )
                                    VariableProductRule.KEY -> {
                                        if (rule.value != null) {
                                            gson.fromJson<Map<String, JsonElement>>(rule.value, mapType).forEach {
                                                putIfNotNull(it.key to it.value)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        result.add(rawChildConfiguration)
                    }
                }
                Pair("bundle_configuration", result)
            }
            ConfigurationType.UNKNOWN -> null
        }
    }
}
