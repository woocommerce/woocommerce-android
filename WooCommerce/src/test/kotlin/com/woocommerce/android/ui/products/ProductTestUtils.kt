package com.woocommerce.android.ui.products

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariant
import com.woocommerce.android.model.toAppModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductVariationModel

object ProductTestUtils {
    fun generateProduct(productId: Long = 1L): Product {
        return WCProductModel(2).apply {
            dateCreated = "2018-01-05T05:14:30Z"
            localSiteId = 1
            remoteProductId = productId
            status = "publish"
            price = "20.00"
            salePrice = "10.00"
            regularPrice = "30.00"
            averageRating = "3.0"
            name = "product 1"
            description = "product 1 description"
            images = "[]"
            downloads = "[]"
            weight = "10"
            length = "1"
            width = "2"
            height = "3"
            variations = "[]"
            attributes = "[]"
        }.toAppModel()
    }

    fun generateProductList(): List<Product> {
        with(ArrayList<Product>()) {
            add(generateProduct(1))
            add(generateProduct(2))
            add(generateProduct(3))
            add(generateProduct(4))
            add(generateProduct(5))
            return this
        }
    }

    private fun generateProductVariant(
        productId: Long = 1L,
        variantId: Long = 1L
    ): ProductVariant {
        return WCProductVariationModel(2).apply {
            dateCreated = "2018-01-05T05:14:30Z"
            localSiteId = 1
            remoteProductId = productId
            remoteVariationId = variantId
            price = "10.00"
            image = ""
            attributes = ""
        }.toAppModel().also { it.priceWithCurrency = "$10.00" }
    }

    fun generateProductVariantList(productId: Long = 1L): List<ProductVariant> {
        with(ArrayList<ProductVariant>()) {
            add(generateProductVariant(productId, 1))
            add(generateProductVariant(productId, 2))
            add(generateProductVariant(productId, 3))
            add(generateProductVariant(productId, 4))
            add(generateProductVariant(productId, 5))
            return this
        }
    }
}
