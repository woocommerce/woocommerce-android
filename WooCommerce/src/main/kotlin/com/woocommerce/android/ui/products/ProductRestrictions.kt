package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.annotation.StringRes
import com.woocommerce.android.model.Product
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

interface ProductRestrictions {
    val restrictions: List<ProductRestriction>
    fun isProductRestricted(product: Product): Boolean {
        return restrictions.map { restriction -> restriction(product) }
            .fold(false) { acc, result -> acc || result }
    }

    /**
     * Restricted products which stay in the product list, so that the reason can be shown to the user.
     */
    fun getNonSelectableRestriction(product: Product): ProductRestriction? {
        return restrictions.firstOrNull { restriction ->
            restriction.nonSelectableReason != null && restriction(product)
        }
    }

    /**
     * Restricted products which are removed from the product list altogether.
     */
    fun isProductHidden(product: Product): Boolean {
        return restrictions.any { restriction ->
            restriction.nonSelectableReason == null && restriction(product)
        }
    }
}
class OrderCreationProductRestrictions @Inject constructor() : ProductRestrictions {
    override val restrictions: List<ProductRestriction>
        get() = listOf(
            ProductRestriction.NonPurchasableProducts,
            ProductRestriction.VariableProductsWithNoVariations,
            ProductRestriction.ProductWithPriceNotSpecified,
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
     * When set, the product stays visible in the product selector and this is shown as the reason why it can't be
     * selected. Restrictions without a reason remove the product from the list instead.
     */
    @get:StringRes
    open val nonSelectableReason: Int?
        get() = null

    @Parcelize
    object NonPublishedProducts : ProductRestriction() {
        override fun invoke(product: Product): Boolean {
            return product.status != ProductStatus.PUBLISH
        }
    }

    @Parcelize
    object NonPurchasableProducts : ProductRestriction() {
        override fun invoke(product: Product): Boolean {
            return product.status != ProductStatus.PUBLISH && product.status != ProductStatus.PRIVATE
        }
    }

    @Parcelize
    object VariableProductsWithNoVariations : ProductRestriction() {
        override fun invoke(product: Product): Boolean {
            return (product.isVariable() && product.numVariations == 0)
        }
    }

    @Parcelize
    object ProductWithPriceNotSpecified : ProductRestriction() {
        override fun invoke(product: Product): Boolean {
            return product.price == null
        }
    }
}

private fun Product.isVariable() =
    productType == ProductType.VARIABLE || productType == ProductType.VARIABLE_SUBSCRIPTION
