package com.woocommerce.android.ui.products

import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.products.ProductBackorderStatus.Yes
import com.woocommerce.android.ui.products.ProductStockStatus.InStock

fun generateVariation(): ProductVariation {
    return ProductVariation(
        remoteProductId = 1,
        remoteVariationId = 2,
        sku = "sku",
        image = null,
        price = null,
        regularPrice = null,
        salePrice = null,
        saleEndDateGmt = null,
        saleStartDateGmt = null,
        isSaleScheduled = false,
        stockStatus = InStock,
        backorderStatus = Yes,
        stockQuantity = 1.0,
        priceWithCurrency = null,
        isPurchasable = true,
        isVirtual = false,
        isDownloadable = false,
        isStockManaged = true,
        description = "",
        isVisible = true,
        shippingClass = "",
        shippingClassId = 0,
        menuOrder = 0,
        attributes = emptyArray(),
        length = 0f,
        width = 0f,
        height = 0f,
        weight = 0f,
        minAllowedQuantity = null,
        maxAllowedQuantity = null,
        groupOfQuantity = null,
        overrideProductQuantities = false
    )
}
