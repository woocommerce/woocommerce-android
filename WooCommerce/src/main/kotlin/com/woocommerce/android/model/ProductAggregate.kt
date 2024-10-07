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
    val subscription: SubscriptionDetails?
) : Parcelable {
    val hasShipping: Boolean
        get() = product.hasShipping || subscription?.oneTimeShipping == true

    fun isSame(other: ProductAggregate): Boolean {
        return product.isSameProduct(other.product) && subscription == other.subscription
    }
}
