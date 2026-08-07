package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

interface ProductRestrictions {
    val restrictions: List<ProductRestriction>

    fun getRestriction(product: Product): ProductRestriction? {
        return restrictions.firstOrNull { restriction -> restriction(product) }
    }

    fun getUnsupportedRestriction(product: Product): ProductRestriction.Unsupported? {
        return restrictions.filterIsInstance<ProductRestriction.Unsupported>()
            .firstOrNull { restriction -> restriction(product) }
    }

    fun isProductHidden(product: Product): Boolean {
        return restrictions.any { restriction -> restriction is ProductRestriction.Hidden && restriction(product) }
    }
}
class OrderCreationProductRestrictions @Inject constructor() : ProductRestrictions {
    override val restrictions: List<ProductRestriction>
        get() = listOf(
            ProductRestriction.NonPurchasableProducts,
            ProductRestriction.VariableProductsWithNoVariations,
            ProductRestriction.ProductWithPriceNotSpecified,
            ProductRestriction.SubscriptionProducts,
        )
}

class ProductFilterProductRestrictions @Inject constructor() : ProductRestrictions {
    override val restrictions: List<ProductRestriction>
        get() = listOf(
            ProductRestriction.VariableProductsWithNoVariations,
            ProductRestriction.ProductWithPriceNotSpecified,
        )
}

@Parcelize
sealed class ProductRestriction : (Product) -> Boolean, Parcelable {
    @get:StringRes
    abstract val scanningMessage: Int

    abstract val scanningTrackingReason: String

    /**
     * A restriction which removes the product from the product list altogether.
     */
    sealed class Hidden : ProductRestriction()

    /**
     * A restriction on products the app can't sell at all. The product stays visible in the product list and
     * [reason] is shown as why it can't be selected.
     */
    sealed class Unsupported(@StringRes val reason: Int) : ProductRestriction() {
        override val scanningMessage: Int get() = reason
    }

    @Parcelize
    object NonPublishedProducts : Hidden() {
        override val scanningMessage get() = R.string.order_creation_barcode_scanning_unable_to_add_draft_product
        override val scanningTrackingReason get() = "Failed to add a product that is not published"

        override fun invoke(product: Product): Boolean {
            return product.status != ProductStatus.PUBLISH
        }
    }

    @Parcelize
    object NonPurchasableProducts : Hidden() {
        override val scanningMessage get() = R.string.order_creation_barcode_scanning_unable_to_add_draft_product
        override val scanningTrackingReason get() = "Failed to add a product that is not published"

        override fun invoke(product: Product): Boolean {
            return product.status != ProductStatus.PUBLISH && product.status != ProductStatus.PRIVATE
        }
    }

    @Parcelize
    object VariableProductsWithNoVariations : Hidden() {
        override val scanningMessage get() =
            R.string.order_creation_barcode_scanning_unable_to_add_product_with_no_variations
        override val scanningTrackingReason get() = "Failed to add a variable product with no variations"

        override fun invoke(product: Product): Boolean {
            return (product.isVariable() && product.numVariations == 0)
        }
    }

    @Parcelize
    object ProductWithPriceNotSpecified : Hidden() {
        override val scanningMessage get() =
            R.string.order_creation_barcode_scanning_unable_to_add_product_with_invalid_price
        override val scanningTrackingReason get() = "Failed to add a product whose price is not specified"

        override fun invoke(product: Product): Boolean {
            return product.price == null
        }
    }

    @Parcelize
    object SubscriptionProducts : Unsupported(R.string.product_selector_subscription_not_supported) {
        override val scanningTrackingReason get() = "Failed to add a subscription product"

        override fun invoke(product: Product): Boolean {
            return product.productType.isSubscriptionProduct()
        }
    }
}

private fun Product.isVariable() =
    productType == ProductType.VARIABLE || productType == ProductType.VARIABLE_SUBSCRIPTION
