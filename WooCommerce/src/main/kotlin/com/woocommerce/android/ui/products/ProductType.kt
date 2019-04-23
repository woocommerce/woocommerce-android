package com.woocommerce.android.ui.products

enum class ProductType {
    SIMPLE,
    GROUPED,
    EXTERNAL,
    VARIABLE,
    VARIATION;

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
