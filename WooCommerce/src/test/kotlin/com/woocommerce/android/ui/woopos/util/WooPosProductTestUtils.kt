package com.woocommerce.android.ui.woopos.util

import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel

fun generateWooPosProduct(
    productId: Long = 1,
    parentId: Long? = null,
    productName: String = "Product 1",
    status: WooPosProductModel.WooPosProductStatus = WooPosProductModel.WooPosProductStatus.PUBLISH,
    amount: String = "10.0",
    globalUniqueId: String = "",
    productType: WooPosProductModel.WooPosProductType = WooPosProductModel.WooPosProductType.SIMPLE,
    isDownloadable: Boolean = false,
    images: List<WooPosProductModel.WooPosProductImage> = emptyList(),
    variationIds: List<Long> = emptyList(),
    attributes: List<WooPosProductModel.WooPosProductAttribute> = emptyList(),
) = WooPosProductModel(
    remoteId = productId,
    name = productName,
    pricing = WooPosProductModel.WooPosPricing.RegularPricing(amount.toBigDecimal()),
    type = productType,
    isDownloadable = isDownloadable,
    parentId = parentId,
    sku = "",
    globalUniqueId = globalUniqueId,
    status = status,
    description = "",
    shortDescription = "",
    lastModified = "",
    images = images,
    variationIds = variationIds,
    attributes = attributes,
)
