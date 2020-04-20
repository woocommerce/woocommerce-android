package com.woocommerce.android.ui.products

import androidx.annotation.StringRes
import com.woocommerce.android.R
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.CoreProductType

enum class ProductType(@StringRes val stringResource: Int = 0, val value: String = "") {
    SIMPLE(R.string.product_type_simple, CoreProductType.SIMPLE.value),
    GROUPED(R.string.product_type_grouped, CoreProductType.GROUPED.value),
    EXTERNAL(R.string.product_type_external, CoreProductType.EXTERNAL.value),
    VARIABLE(R.string.product_type_variable, CoreProductType.VARIABLE.value),
    VARIATION(R.string.product_type_variation, CoreProductType.VARIABLE.value);

    companion object {
        fun fromString(type: String): ProductType {
            return when (type.toLowerCase()) {
                "grouped" -> GROUPED
                "external" -> EXTERNAL
                "variable" -> VARIABLE
                "variation" -> VARIATION
                else -> SIMPLE
            }
        }
    }
}
