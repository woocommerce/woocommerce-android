package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

interface ProductRestrictions {
    val restrictions: List<ProductRestriction>

    fun isProductBlocked(product: Product): Boolean {
        return restrictions.any { restriction -> restriction(product) }
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
    /**
     * A restriction which removes the product from the product list altogether.
     */
    sealed class Hidden : ProductRestriction()

    /**
     * A restriction on products the app can't sell at all. The product stays visible in the product list and
     * [reason] is shown as why it can't be selected.
     */
    sealed class Unsupported(@StringRes val reason: Int) : ProductRestriction()

    @Parcelize
    object NonPublishedProducts : Hidden() {
        override fun invoke(product: Product): Boolean {
            return product.status != ProductStatus.PUBLISH
        }
    }

    @Parcelize
    object NonPurchasableProducts : Hidden() {
        override fun invoke(product: Product): Boolean {
            return product.status != ProductStatus.PUBLISH && product.status != ProductStatus.PRIVATE
        }
    }

    @Parcelize
    object VariableProductsWithNoVariations : Hidden() {
        override fun invoke(product: Product): Boolean {
            return (product.isVariable() && product.numVariations == 0)
        }
    }

    @Parcelize
    object ProductWithPriceNotSpecified : Hidden() {
        override fun invoke(product: Product): Boolean {
            return product.price == null
        }
    }

    @Parcelize
    object SubscriptionProducts : Unsupported(R.string.product_selector_subscription_not_supported) {
        override fun invoke(product: Product): Boolean {
            return product.productType.isSubscriptionProduct()
        }
    }
}

private fun Product.isVariable() =
    productType == ProductType.VARIABLE || productType == ProductType.VARIABLE_SUBSCRIPTION
