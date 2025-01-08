package com.woocommerce.android.ui.woopos.home.items

sealed class WooPosItemNavigationData(open val id: Long) {
    data class VariableProductData(
        override val id: Long,
        val name: String,
        val numOfVariations: Int,
    ) : WooPosItemNavigationData(id)
}
