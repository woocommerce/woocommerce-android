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
    OTHER;

    companion object {
        fun fromString(type: String): ProductType {
            return when (type.toLowerCase(Locale.US)) {
                "grouped" -> GROUPED
                "external" -> EXTERNAL
                "variable" -> VARIABLE
                "simple" -> SIMPLE
                else -> OTHER
            }
        }

        fun isGroupedOrExternalProduct(type: ProductType) = type == GROUPED || type == EXTERNAL
    }
}
