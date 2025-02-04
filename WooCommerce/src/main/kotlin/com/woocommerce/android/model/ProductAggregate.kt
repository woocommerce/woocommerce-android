package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Container class for product and any additional details that are stored as product metadata.
 *
 * For now, the additional details include subscription details only.
 *
 * @param product The product.
 * @param subscription The subscription details.
 */
@Parcelize
data class ProductAggregate(
    val product: Product,
    val subscription: SubscriptionDetails? = null
) : Parcelable {
    val remoteId: Long
        get() = product.remoteId

    val hasShipping: Boolean
        get() = product.hasShipping || subscription?.oneTimeShipping == true

    fun isSame(other: ProductAggregate): Boolean {
        return product.isSameProduct(other.product) && subscription == other.subscription
    }

    fun merge(other: ProductAggregate): ProductAggregate {
        return copy(
            product = product.mergeProduct(other.product),
            subscription = other.subscription
        )
    }
}
