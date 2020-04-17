package com.woocommerce.android.ui.products

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.ProductFilterOption.FilterProductType
import java.util.Locale

enum class ProductType(@StringRes val stringResource: Int = 0) {
    SIMPLE(R.string.product_type_simple),
    GROUPED(R.string.product_type_grouped),
    EXTERNAL(R.string.product_type_external),
    VARIABLE(R.string.product_type_variable),
    VARIATION(R.string.product_type_variation);

    companion object {
        fun fromString(type: String): ProductType {
            return when (type.toLowerCase(Locale.US)) {
                "grouped" -> GROUPED
                "external" -> EXTERNAL
                "variable" -> VARIABLE
                "variation" -> VARIATION
                else -> SIMPLE
            }
        }

        fun toFilterProductTypeList() = values()
                .map { FilterProductType(it.stringResource, it.name) }
                .toMutableList()
    }
}
