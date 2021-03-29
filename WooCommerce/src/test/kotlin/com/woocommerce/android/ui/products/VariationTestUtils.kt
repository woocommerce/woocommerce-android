package com.woocommerce.android.ui.products

import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.products.ProductBackorderStatus.Yes
import com.woocommerce.android.ui.products.ProductStockStatus.InStock

fun generateVariation(): ProductVariation {
    return ProductVariation(
        backorderStatus = Yes,
        description = "",
        height = 0f,
        image = null,
        isDownloadable = false,
        isPurchasable = true,
        isSaleScheduled = false,
        isStockManaged = true,
        isVirtual = false,
        isVisible = true,
        length = 0f,
        options = emptyList(),
        priceWithCurrency = null,
        regularPrice = null,
        remoteProductId = 1,
        remoteVariationId = 2,
        saleEndDateGmt = null,
        salePrice = null,
        saleStartDateGmt = null,
        shippingClass = "",
        shippingClassId = 0,
        sku = "sku",
        stockQuantity = 1,
        stockStatus = InStock,
        weight = 0f,
        width = 0f
    )
}
