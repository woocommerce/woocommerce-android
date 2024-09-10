package com.woocommerce.android.ui.products

import androidx.annotation.StringRes
import com.woocommerce.android.R
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductType
import java.util.Locale

enum class ProductType(@StringRes val stringResource: Int = 0, val value: String = "") {
    SIMPLE(R.string.product_type_simple, CoreProductType.SIMPLE.value),
    GROUPED(R.string.product_type_grouped, CoreProductType.GROUPED.value),
    EXTERNAL(R.string.product_type_external, CoreProductType.EXTERNAL.value),
    VARIABLE(R.string.product_type_variable, CoreProductType.VARIABLE.value),
    SUBSCRIPTION(R.string.product_type_subscription, "subscription"),
    VARIABLE_SUBSCRIPTION(R.string.product_type_variable_subscription, "variable-subscription"),
    BUNDLE(R.string.product_type_bundle, CoreProductType.BUNDLE.value),
    COMPOSITE(R.string.product_type_composite, "composite"),
    VARIATION(R.string.product_type_variation, "variation"),
    OTHER;

    fun isVariableProduct() = this == VARIABLE || this == VARIABLE_SUBSCRIPTION

    companion object {
        val FILTERABLE_VALUES =
            setOf(SIMPLE, GROUPED, EXTERNAL, VARIABLE, SUBSCRIPTION, VARIABLE_SUBSCRIPTION, BUNDLE, COMPOSITE)

        fun fromString(type: String): ProductType {
            return when (type.lowercase(Locale.US)) {
                "grouped" -> GROUPED
                "external" -> EXTERNAL
                "variable" -> VARIABLE
                "simple" -> SIMPLE
                "subscription" -> SUBSCRIPTION
                "variable-subscription" -> VARIABLE_SUBSCRIPTION
                "bundle" -> BUNDLE
                "composite" -> COMPOSITE
                "variation" -> VARIATION
                else -> OTHER
            }
        }
    }
}
