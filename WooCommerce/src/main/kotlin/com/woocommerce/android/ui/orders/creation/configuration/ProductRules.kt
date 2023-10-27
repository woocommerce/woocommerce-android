package com.woocommerce.android.ui.orders.creation.configuration

import android.os.Parcelable
import com.woocommerce.android.extensions.sumByFloat
import com.woocommerce.android.ui.orders.creation.OrderCreationProduct
import com.woocommerce.android.ui.products.ProductType
import kotlinx.parcelize.Parcelize

@Parcelize
class ProductRules private constructor(
    val productType: ProductType,
    val itemRules: Map<String, ItemRules>,
    val childrenRules: Map<Long, Map<String, ItemRules>>? = null
) : Parcelable {
    class Builder {
        var productType: ProductType = ProductType.OTHER
        private val rules = mutableMapOf<String, ItemRules>()
        private val childrenRules = mutableMapOf<Long, MutableMap<String, ItemRules>>()

        fun setQuantityRules(quantityMin: Float?, quantityMax: Float?) {
            rules[QuantityRule.KEY] = QuantityRule(quantityMin = quantityMin, quantityMax = quantityMax)
        }

        fun setChildQuantityRules(itemId: Long, quantityMin: Float?, quantityMax: Float?, quantityDefault: Float?) {
            val childRules = childrenRules.getOrPut(itemId) { mutableMapOf() }
            childRules[QuantityRule.KEY] = QuantityRule(
                quantityMin = quantityMin,
                quantityMax = quantityMax,
                quantityDefault = quantityDefault
            )
        }

        fun setChildOptional(itemId: Long) {
            val childRules = childrenRules.getOrPut(itemId) { mutableMapOf() }
            childRules[OptionalRule.KEY] = OptionalRule()
        }

        fun build(): ProductRules {
            val itemChildrenRules = if (childrenRules.isEmpty()) null else childrenRules
            return ProductRules(productType, rules, itemChildrenRules)
        }
    }
}

interface ItemRules : Parcelable {
    fun getInitialValue(): String?
}

@Parcelize
class QuantityRule(val quantityMin: Float?, val quantityMax: Float?, val quantityDefault: Float? = null) : ItemRules {

    companion object {
        const val KEY = "quantity_rule"
    }

    override fun getInitialValue(): String? = quantityDefault?.toString()
}

@Parcelize
class OptionalRule : ItemRules {
    companion object {
        const val KEY = "optional_rule"
    }

    override fun getInitialValue(): String? = null
}

@Parcelize
class ProductConfiguration(
    val configurationType: ConfigurationType,
    val configuration: MutableMap<String, String?>,
    val childrenConfiguration: MutableMap<Long, MutableMap<String, String?>>? = null
) : Parcelable {
    companion object {
        fun getConfiguration(
            rules: ProductRules,
            children: List<OrderCreationProduct.ProductItem>? = null
        ): ProductConfiguration {
            val itemConfiguration = rules.itemRules.mapValues { it.value.getInitialValue() }.toMutableMap()
            val childrenConfiguration = rules.childrenRules?.mapValues { childrenRules ->
                childrenRules.value.mapValues { it.value.getInitialValue() }.toMutableMap()
            }?.toMutableMap()
            if (children != null && rules.itemRules.containsKey(QuantityRule.KEY)) {
                val childrenQuantity = children.sumByFloat { childItem -> childItem.item.quantity }
                itemConfiguration[QuantityRule.KEY] = childrenQuantity.toString()
            }
            children?.filter { child -> child.item.configurationKey != null }?.forEach { child ->
                childrenConfiguration?.get(child.item.configurationKey)?.let { configuration ->
                    if (configuration.containsKey(QuantityRule.KEY)) {
                        configuration[QuantityRule.KEY] = (child.item.quantity / child.item.quantity).toString()
                    }
                }
            }
            val configurationType = rules.productType.getConfigurationType()
            return ProductConfiguration(configurationType, itemConfiguration, childrenConfiguration)
        }
    }

    fun needsConfiguration(): Boolean {
        val itemNeedsConfiguration = configuration.any { entry -> entry.value == null }
        val childrenNeedsConfiguration = childrenConfiguration?.any {
            it.value.any { entry -> entry.value == null }
        } ?: false
        return itemNeedsConfiguration || childrenNeedsConfiguration
    }

    fun updateChildrenConfiguration(itemId: Long, ruleKey: String, value: String) {
        childrenConfiguration?.get(itemId)?.set(ruleKey, value)
    }
}

enum class ConfigurationType { BUNDLE, UNKNOWN }

fun ProductType.getConfigurationType(): ConfigurationType {
    return when (this) {
        ProductType.BUNDLE -> ConfigurationType.BUNDLE
        else -> ConfigurationType.UNKNOWN
    }
}
