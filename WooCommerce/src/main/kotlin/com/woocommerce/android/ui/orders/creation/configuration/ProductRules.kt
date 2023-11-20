package com.woocommerce.android.ui.orders.creation.configuration

import android.os.Parcelable
import com.google.gson.Gson
import com.woocommerce.android.R
import com.woocommerce.android.extensions.formatToString
import com.woocommerce.android.extensions.sumByFloat
import com.woocommerce.android.model.VariantOption
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.ResourceProvider
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

        fun setChildVariableRules(itemId: Long, attributesDefault: List<VariantOption>?, variationIds: List<Long>?) {
            val childRules = childrenRules.getOrPut(itemId) { mutableMapOf() }
            val defaultAttributes = if (attributesDefault.isNullOrEmpty()) null else attributesDefault
            val allowedVariations = if (variationIds.isNullOrEmpty()) null else variationIds
            childRules[VariableProductRule.KEY] = VariableProductRule(defaultAttributes, allowedVariations)
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

    fun getRuleBounds(resourceProvider: ResourceProvider): String {
        return when {
            quantityMin != null && quantityMax != null && quantityMin == quantityMax -> {
                StringUtils.getQuantityString(
                    resourceProvider = resourceProvider,
                    quantity = quantityMin.toInt(),
                    default = R.string.configuration_quantity_item_plural,
                    one = R.string.configuration_quantity_item
                )
            }

            quantityMin != null && quantityMax != null && quantityMin != quantityMax -> resourceProvider.getString(
                R.string.configuration_quantity_between,
                quantityMin.formatToString(),
                quantityMax.formatToString()
            )

            quantityMin != null -> {
                StringUtils.getQuantityString(
                    resourceProvider = resourceProvider,
                    quantity = quantityMin.toInt(),
                    default = R.string.configuration_quantity_more_than_plural,
                    one = R.string.configuration_quantity_more_than
                )
            }

            quantityMax != null -> {
                resourceProvider.getString(
                    R.string.configuration_quantity_less_than,
                    quantityMax.formatToString()
                )
            }

            else -> StringUtils.EMPTY
        }
    }
}

@Parcelize
class OptionalRule : ItemRules {
    companion object {
        const val KEY = "optional_rule"
    }

    override fun getInitialValue(): String? = null
}

@Parcelize
class VariableProductRule(
    val attributesDefault: List<VariantOption>?,
    val variationIds: List<Long>?
) : ItemRules {
    companion object {
        const val KEY = "variable_product_rule"
        const val VARIATION_ID = "variation_id"
        const val VARIATION_ATTRIBUTES = "attributes"
    }

    override fun getInitialValue(): String? {
        return if (variationIds != null && variationIds.size == 1 && attributesDefault != null) {
            val value = mapOf<String, Any?>(
                VARIATION_ID to variationIds.first(),
                VARIATION_ATTRIBUTES to attributesDefault
            )
            Gson().toJson(value)
        } else null
    }
}

@Parcelize
class ProductConfiguration(
    val rules: ProductRules,
    val configurationType: ConfigurationType,
    val configuration: MutableMap<String, String?>,
    val childrenConfiguration: MutableMap<Long, MutableMap<String, String?>>? = null
) : Parcelable {
    companion object {
        const val PARENT_KEY = -1L
    }
    fun getConfigurationIssues(resourceProvider: ResourceProvider): Map<Long, String> {
        val result = mutableMapOf<Long, String>()

        if (rules.itemRules.containsKey(QuantityRule.KEY)) {
            val itemQuantityRule = rules.itemRules[QuantityRule.KEY] as QuantityRule
            val childrenQuantity = childrenConfiguration?.values
                ?.filter { map -> map.containsKey(QuantityRule.KEY) }
                ?.sumByFloat { map -> map[QuantityRule.KEY]?.toFloatOrNull() ?: 0f } ?: 0f

            val isMinimumOutOfBounds = childrenQuantity < (itemQuantityRule.quantityMin ?: Float.NEGATIVE_INFINITY)
            val isMaximumOutOfBounds = childrenQuantity > (itemQuantityRule.quantityMax ?: Float.MAX_VALUE)
            if (isMinimumOutOfBounds || isMaximumOutOfBounds) {
                val ruleBounds = itemQuantityRule.getRuleBounds(resourceProvider)
                result[PARENT_KEY] = resourceProvider.getString(R.string.configuration_quantity_rule_issue, ruleBounds)
            }
        }
        childrenConfiguration?.forEach { entry ->
            val itemQuantity = entry.value[QuantityRule.KEY]?.toFloatOrNull() ?: 0f
            val isVariable = entry.value.containsKey(VariableProductRule.KEY)
            val attributesAreNullOrEmpty = entry.value[VariableProductRule.KEY].isNullOrEmpty()

            if (itemQuantity > 0f && isVariable && attributesAreNullOrEmpty) {
                result[entry.key] = resourceProvider.getString(R.string.configuration_variable_selection)
            }
        }
        return result
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
