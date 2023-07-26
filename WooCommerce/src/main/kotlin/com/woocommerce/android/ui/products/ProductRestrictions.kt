package com.woocommerce.android.ui.products

import android.os.Parcelable
import com.woocommerce.android.model.Product
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

interface ProductRestrictions {
    val restrictions: List<ProductRestriction>
    fun isProductRestricted(product: Product): Boolean {
        return restrictions.map { restriction -> restriction(product) }
            .fold(true) { acc, result -> acc && result }
    }
}

class CouponsProductRestrictions @Inject constructor() : ProductRestrictions {

    override val restrictions: List<ProductRestriction>
        get() = listOf(
            ProductRestriction.NonPublishedProducts,
            ProductRestriction.NoVariableProductsWithNoVariations,
            ProductRestriction.ProductWithPriceSpecified,
        )
}

class OrderCreationProductRestrictions @Inject constructor() : ProductRestrictions {

    override val restrictions: List<ProductRestriction>
        get() = listOf(
            ProductRestriction.NonPublishedProducts,
            ProductRestriction.NoVariableProductsWithNoVariations,
            ProductRestriction.ProductWithPriceSpecified,
        )
}

class ProductFilterProductRestrictions @Inject constructor() : ProductRestrictions {

    override val restrictions: List<ProductRestriction>
        get() = listOf(
            ProductRestriction.NonPublishedProducts,
            ProductRestriction.NoVariableProductsWithNoVariations,
            ProductRestriction.ProductWithPriceSpecified,
        )
}

@Parcelize
sealed class ProductRestriction : (Product) -> Boolean, Parcelable {
    @Parcelize
    object NonPublishedProducts : ProductRestriction() {
        override fun invoke(product: Product): Boolean {
            return product.status != ProductStatus.PUBLISH
        }
    }

    @Parcelize
    object NoVariableProductsWithNoVariations : ProductRestriction() {
        override fun invoke(product: Product): Boolean {
            return !(product.isVariable() && product.numVariations == 0)
        }
    }

    @Parcelize
    object ProductWithPriceSpecified : ProductRestriction() {
        override fun invoke(product: Product): Boolean {
            return product.price != null
        }

    }
}

private fun Product.isVariable() = productType == ProductType.VARIABLE || productType == ProductType.VARIABLE_SUBSCRIPTION
