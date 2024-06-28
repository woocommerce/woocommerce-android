package com.woocommerce.android.ui.products.models

import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.ResourceProvider

fun QuantityRules.getProductProperty(
    resources: ResourceProvider,
    onClick: (() -> Unit)
): ProductProperty? {
    if (allRulesAreNull) {
        return null
    }

    var productProperty: ProductProperty
    if (hasAtLeastOneValidRule) {
        val properties: Map<String, String> = buildMap {
            putIfNotNullOrZero(resources.getString(R.string.min_quantity) to min?.toString())
            putIfNotNullOrZero(resources.getString(R.string.max_quantity) to max?.toString())
            if (size < 2) {
                putIfNotNullOrZero(resources.getString(R.string.group_of) to groupOf?.toString())
            }
        }

        productProperty = ProductProperty.PropertyGroup(
            title = R.string.product_quantity_rules_title,
            icon = R.drawable.ic_gridicons_product,
            properties = properties,
            showTitle = true,
            onClick = onClick
        )
    } else {
        productProperty = ProductProperty.ComplexProperty(
            title = R.string.product_quantity_rules_title,
            value = resources.getString(R.string.no_quantity_rules),
            icon = R.drawable.ic_gridicons_product,
            onClick = onClick
        )
    }

    return productProperty
}

private fun MutableMap<String, String>.putIfNotNullOrZero(vararg pairs: Pair<String, String?>) = apply {
    pairs.forEach { pair ->
        pair.second?.takeIf { it != "0" }?.let { put(pair.first, it) }
    }
}

private val QuantityRules.hasAtLeastOneValidRule: Boolean
    get() = (min ?: 0) > 0 || (max ?: 0) > 0 || (groupOf ?: 0) > 0

private val QuantityRules.allRulesAreNull: Boolean
    get() = min == null && max == null && groupOf == null
