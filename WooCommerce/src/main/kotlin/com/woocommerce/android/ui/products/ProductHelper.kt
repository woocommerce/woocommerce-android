package com.woocommerce.android.ui.products

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductBackorderStatus.NotAvailable
import com.woocommerce.android.ui.products.ProductStatus.PUBLISH
import com.woocommerce.android.ui.products.ProductStockStatus.InStock
import com.woocommerce.android.ui.products.ProductTaxStatus.None
import com.woocommerce.android.ui.products.ProductType.VARIABLE
import com.woocommerce.android.ui.products.settings.ProductCatalogVisibility.VISIBLE
import java.math.BigDecimal
import java.util.Date

object ProductHelper {
    /**
     * Simple helper which returns the variation ID if it's not null and non-zero, otherwise returns the product ID
     * if it's not null (and if it is, returns 0). This is useful when deciding whether to use a product ID or a
     * variation ID when looking up a product - we want to favor the variation ID when available because that will
     * get us the actual variation (the productId in this situation will be the ID of the parent product)
     */
    fun productOrVariationId(productId: Long?, variationId: Long?): Long {
        variationId?.let {
            if (it != 0L) {
                return it
            }
        }
        productId?.let {
            return it
        }
        return 0L
    }

    /**
     * Default Product for initial state of Product Add flow
     * */

    fun getDefaultNewProduct(productType: ProductType): Product {
        return Product(
            remoteId = 0L,
            name = "",
            description = "",
            shortDescription = "",
            type = productType.value,
            status = PUBLISH,
            catalogVisibility = VISIBLE,
            isFeatured = false,
            stockStatus = InStock,
            backorderStatus = NotAvailable,
            dateCreated = Date(),
            firstImageUrl = null,
            totalSales = 0,
            reviewsAllowed = true,
            isVirtual = productType == VARIABLE,
            ratingCount = 0,
            averageRating = 0f,
            permalink = "",
            externalUrl = "",
            buttonText = "",
            salePrice = BigDecimal.ZERO,
            regularPrice = BigDecimal.ZERO,
            taxClass = Product.TAX_CLASS_DEFAULT,
            isStockManaged = false,
            stockQuantity = 0.0,
            sku = "",
            slug = "",
            length = 0f,
            width = 0f,
            height = 0f,
            weight = 0f,
            shippingClass = "",
            shippingClassId = 0L,
            isDownloadable = false,
            downloadLimit = 0,
            downloadExpiry = 0,
            purchaseNote = "",
            numVariations = 0,
            images = listOf(),
            attributes = listOf(),
            saleEndDateGmt = null,
            saleStartDateGmt = null,
            isSoldIndividually = true,
            taxStatus = None,
            isSaleScheduled = false,
            menuOrder = 0,
            categories = listOf(),
            tags = listOf(),
            groupedProductIds = listOf(),
            crossSellProductIds = listOf(),
            upsellProductIds = listOf(),
            downloads = listOf()
        )
    }
}
