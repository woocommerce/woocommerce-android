package com.woocommerce.android.ui.orders.creation.configuration

import com.google.gson.Gson
import com.woocommerce.android.extensions.sumByFloat
import com.woocommerce.android.ui.orders.creation.OrderCreationProduct
import com.woocommerce.android.ui.products.variations.VariationDetailRepository
import javax.inject.Inject

class GetProductConfiguration @Inject constructor(
    private val variationDetailRepository: VariationDetailRepository,
    private val gson: Gson
) {
    suspend operator fun invoke(
        rules: ProductRules,
        children: List<OrderCreationProduct.ProductItem>? = null,
        parentQuantity: Float = 1f
    ): ProductConfiguration {
        val itemConfiguration = rules.itemRules.mapValues { it.value.getInitialValue() }.toMutableMap()
        val childrenConfiguration = rules.childrenRules?.mapValues { childrenRules ->
            childrenRules.value.mapValues { it.value.getInitialValue() }.toMutableMap()
        }?.toMutableMap()
        if (children != null && rules.itemRules.containsKey(QuantityRule.KEY)) {
            val childrenQuantity = children.sumByFloat { childItem -> childItem.item.quantity }
            itemConfiguration[QuantityRule.KEY] = childrenQuantity.toString()
        }
        val filteredChildren = children?.filter { child -> child.item.configurationKey != null }
        filteredChildren?.forEach { child ->
            childrenConfiguration?.get(child.item.configurationKey)?.let { configuration ->
                saveQuantityConfiguration(configuration, child, parentQuantity)
                saveOptionalConfiguration(configuration)
                saveVariableConfiguration(configuration, child)
            }
        }
        val configurationType = rules.productType.getConfigurationType()
        return ProductConfiguration(rules, configurationType, itemConfiguration, childrenConfiguration)
    }

    private fun saveQuantityConfiguration(
        configuration: MutableMap<String, String?>,
        child: OrderCreationProduct.ProductItem,
        parentQuantity: Float
    ) {
        if (configuration.containsKey(QuantityRule.KEY)) {
            configuration[QuantityRule.KEY] = (child.item.quantity / parentQuantity).toString()
        }
    }

    private fun saveOptionalConfiguration(
        configuration: MutableMap<String, String?>
    ) {
        if (configuration.containsKey(OptionalRule.KEY)) {
            configuration[OptionalRule.KEY] = true.toString()
        }
    }
    private suspend fun saveVariableConfiguration(
        configuration: MutableMap<String, String?>,
        child: OrderCreationProduct.ProductItem
    ) {
        if (child.item.isVariation && configuration.containsKey(VariableProductRule.KEY)) {
            variationDetailRepository.getVariationOrNull(
                child.item.productId,
                child.item.variationId
            )?.let {
                val value = mapOf<String, Any?>(
                    VariableProductRule.VARIATION_ID to child.item.variationId,
                    VariableProductRule.VARIATION_ATTRIBUTES to it.attributes
                )
                configuration[VariableProductRule.KEY] = gson.toJson(value)
            }
        }
    }
}
