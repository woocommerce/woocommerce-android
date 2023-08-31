package com.woocommerce.android.ui.orders.creation.bundle

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class OrderItemRules private constructor(
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

        fun build(): OrderItemRules {
            val itemChildrenRules = if (childrenRules.isEmpty()) null else childrenRules
            return OrderItemRules(rules, itemChildrenRules)
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
class OrderItemConfiguration(
    val configuration: Map<String, String?>,
    val childrenConfiguration: Map<Long, Map<String, String?>>? = null
) : Parcelable {
    fun needsConfiguration(): Boolean {
        val itemNeedsConfiguration = configuration.any { entry -> entry.value == null }
        val childrenNeedsConfiguration = childrenConfiguration?.any {
            it.value.any { entry -> entry.value == null }
        } ?: false
        return itemNeedsConfiguration || childrenNeedsConfiguration
    }
}
