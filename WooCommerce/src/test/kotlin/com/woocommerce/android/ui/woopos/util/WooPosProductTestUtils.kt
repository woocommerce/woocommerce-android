package com.woocommerce.android.ui.woopos.util

import com.woocommerce.android.ui.woopos.common.data.WooPosVariation
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import java.math.BigDecimal

fun generateWooPosProduct(
    productId: Long = 1,
    parentId: Long? = null,
    productName: String = "Product 1",
    status: WooPosProductModel.WooPosProductStatus = WooPosProductModel.WooPosProductStatus.PUBLISH,
    amount: String = "10.0",
    globalUniqueId: String = "",
    productType: WooPosProductModel.WooPosProductType = WooPosProductModel.WooPosProductType.Simple,
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

fun generateWooPosVariation(
    remoteVariationId: Long = 1,
    remoteProductId: Long = 1,
    globalUniqueId: String = "VAR123",
    type: WooPosVariation.WooPosVariationType = WooPosVariation.WooPosVariationType.VARIATION,
    price: BigDecimal = BigDecimal.TEN,
    image: WooPosVariation.WooPosVariationImage? = null,
    attributes: List<WooPosVariation.WooPosVariationAttribute> = emptyList(),
    isVisible: Boolean = true,
    isDownloadable: Boolean = false
) = WooPosVariation(
    remoteVariationId = remoteVariationId,
    remoteProductId = remoteProductId,
    globalUniqueId = globalUniqueId,
    type = type,
    price = price,
    image = image,
    attributes = attributes,
    isVisible = isVisible,
    isDownloadable = isDownloadable
)
