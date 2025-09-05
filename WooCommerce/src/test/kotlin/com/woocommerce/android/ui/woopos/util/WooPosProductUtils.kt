package com.woocommerce.android.ui.woopos.util

import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModelVersion2
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModelVersion2.WooPosPricing
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModelVersion2.WooPosProductImage
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModelVersion2.WooPosProductStatus
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModelVersion2.WooPosProductType

fun generateWooPosProduct(
    productId: Long = 1,
    productName: String = "Product 1",
    status: WooPosProductStatus = WooPosProductStatus.PUBLISH,
    amount: String = "10.0",
    productType: WooPosProductType = WooPosProductType.SIMPLE,
    isDownloadable: Boolean = false,
    images: List<WooPosProductImage> = emptyList()
) = WooPosProductModelVersion2(
    remoteId = productId,
    name = productName,
    pricing = WooPosPricing.RegularPricing(amount.toBigDecimal()),
    type = productType,
    isDownloadable = isDownloadable,
    parentId = null,
    sku = "",
    globalUniqueId = "",
    status = status,
    description = "",
    shortDescription = "",
    lastModified = "",
    images = images,
)
