package com.woocommerce.android.ui.woopos.home.items

sealed class WooPosItemSelectionViewState(
    open val id: Long,
    open val name: String
) {
    sealed class Product(
        override val id: Long,
        override val name: String,
        open val price: String,
    ) : WooPosItemSelectionViewState(id, name) {
        data class Simple(
            override val id: Long,
            override val name: String,
            override val price: String,
            val imageUrl: String?,
        ) : Product(id, name, price)

        data class Variable(
            override val id: Long,
            override val name: String,
            override val price: String,
            val imageUrl: String?,
            val numOfVariations: Int,
            val variationIds: List<Long>,
        ) : Product(id, name, price)
    }

    data class Variation(
        override val id: Long,
        override val name: String,
        val productId: Long,
        val price: String,
        val imageUrl: String?,
    ) : WooPosItemSelectionViewState(id, name)
}
