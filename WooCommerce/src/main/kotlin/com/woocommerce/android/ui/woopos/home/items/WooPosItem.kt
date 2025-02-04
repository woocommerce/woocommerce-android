package com.woocommerce.android.ui.woopos.home.items

sealed class WooPosItem(
    open val id: Long,
    open val name: String
) {
    data class SimpleProduct(
        override val id: Long,
        override val name: String,
        val price: String,
        val imageUrl: String?,
    ) : WooPosItem(id, name)

    data class VariableProduct(
        override val id: Long,
        override val name: String,
        val price: String,
        val imageUrl: String?,
        val numOfVariations: Int,
        val variationIds: List<Long>,
    ) : WooPosItem(id, name)

    data class Variation(
        override val id: Long,
        override val name: String,
        val productId: Long,
        val price: String,
        val imageUrl: String?,
    ) : WooPosItem(id, name)
}
