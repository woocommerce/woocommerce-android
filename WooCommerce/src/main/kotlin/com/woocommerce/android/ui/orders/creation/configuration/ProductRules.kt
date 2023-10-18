package com.woocommerce.android.ui.orders.creation.configuration

import android.os.Parcelable
import com.woocommerce.android.extensions.sumByFloat
import com.woocommerce.android.ui.orders.creation.OrderCreationProduct
import kotlinx.parcelize.Parcelize

@Parcelize
class ProductRules private constructor(
    val itemRules: Map<String, ItemRules>,
    val childrenRules: Map<Long, Map<String, ItemRules>>? = null
) : Parcelable {
    fun isConfigurable(): Boolean {
        return itemRules.any { it.value.isConfigurable() } || childrenRules?.any {
            it.value.any { childrenRules ->
                childrenRules.value.isConfigurable()
            }
        } ?: false
    }

    class Builder {
        private val rules = mutableMapOf<String, ItemRules>()
        private val childrenRules = mutableMapOf<Long, MutableMap<String, ItemRules>>()

        fun setQuantityRules(quantityMin: Long?, quantityMax: Long?) {
            rules[QuantityRule.KEY] = QuantityRule(quantityMin = quantityMin, quantityMax = quantityMax)
        }

        fun setChildQuantityRules(itemId: Long, quantityMin: Long?, quantityMax: Long?, quantityDefault: Long?) {
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
            return ProductRules(rules, itemChildrenRules)
        }
    }
}

interface ItemRules : Parcelable {
    fun getInitialValue(): String?
    fun isConfigurable(): Boolean
}

@Parcelize
class QuantityRule(val quantityMin: Long?, val quantityMax: Long?, val quantityDefault: Long? = null) : ItemRules {

    companion object {
        const val KEY = "quantity_rule"
    }

    override fun getInitialValue(): String? = quantityDefault?.toString()
    override fun isConfigurable(): Boolean = quantityMin != quantityMax
}

@Parcelize
class OptionalRule : ItemRules {
    companion object {
        const val KEY = "optional_rule"
    }

    override fun getInitialValue(): String? = null
    override fun isConfigurable(): Boolean = true
}

@Parcelize
class ProductConfiguration(
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
            return ProductConfiguration(itemConfiguration, childrenConfiguration)
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
