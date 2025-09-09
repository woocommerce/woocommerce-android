package com.woocommerce.android.ui.woopos.util

import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModelVersion2

fun generateWooPosProduct(
    productId: Long = 1,
    parentId: Long? = null,
    productName: String = "Product 1",
    status: WooPosProductModelVersion2.WooPosProductStatus = WooPosProductModelVersion2.WooPosProductStatus.PUBLISH,
    amount: String = "10.0",
    productType: WooPosProductModelVersion2.WooPosProductType = WooPosProductModelVersion2.WooPosProductType.SIMPLE,
    isDownloadable: Boolean = false,
    images: List<WooPosProductModelVersion2.WooPosProductImage> = emptyList(),
    variationIds: List<Long> = emptyList(),
) = WooPosProductModelVersion2(
    remoteId = productId,
    name = productName,
    pricing = WooPosProductModelVersion2.WooPosPricing.RegularPricing(amount.toBigDecimal()),
    type = productType,
    isDownloadable = isDownloadable,
    parentId = parentId,
    sku = "",
    globalUniqueId = "",
    status = status,
    description = "",
    shortDescription = "",
    lastModified = "",
    images = images,
    variationIds = variationIds,
)

