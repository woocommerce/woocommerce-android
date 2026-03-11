package com.woocommerce.android.ui.woopos.home.items

sealed class WooPosItemSelectionViewState(
    open val id: Long,
    open val name: String
) {
    val uniqueKey: String
        get() = when (this) {
            is Product.Simple -> "simple_$id"
            is Product.Variable -> "variable_$id"
            is Product.Variation -> "variation_$id"
            is Product.VariationSearchResult -> "variation_search_$id"
            is Coupon -> "coupon_$id"
        }
    sealed class Product(
        override val id: Long,
        override val name: String,
        open val price: String,
        open val imageUrl: String?,
    ) : WooPosItemSelectionViewState(id, name) {
        data class Simple(
            override val id: Long,
            override val name: String,
            override val price: String,
            override val imageUrl: String?,
        ) : Product(id, name, price, imageUrl)

        data class Variable(
            override val id: Long,
            override val name: String,
            override val price: String,
            override val imageUrl: String?,
            val numOfVariations: Int,
            val variationIds: List<Long>,
        ) : Product(id, name, price, imageUrl)

        data class Variation(
            override val id: Long,
            override val name: String,
            override val price: String,
            override val imageUrl: String?,
            val productId: Long,
        ) : Product(id, name, price, imageUrl)

        data class VariationSearchResult(
            override val id: Long,
            override val name: String,
            override val price: String,
            override val imageUrl: String?,
            val productId: Long,
            val parentProductName: String,
        ) : Product(id, name, price, imageUrl)
    }

    data class Coupon(
        override val id: Long,
        override val name: String,
        val summary: String,
        val expiredState: ExpiredState,
    ) : WooPosItemSelectionViewState(id, name) {
        sealed class ExpiredState {
            data class Expired(val formattedDate: String) : ExpiredState()
            data object NotExpired : ExpiredState()
        }
    }
}
