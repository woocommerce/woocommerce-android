package com.woocommerce.android.ui.woopos.common.data.models

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import dagger.Reusable
import org.wordpress.android.fluxc.model.WCProductModel
import java.math.BigDecimal
import javax.inject.Inject

@Reusable
class WCProductToWooPosProductModelMapper @Inject constructor(
    private val wooPosProductModelMapper: WooPosProductModelVersion2Mapper,
    private val logger: WooPosLogWrapper
) {
    fun map(wcProduct: WCProductModel): WooPosProductModelVersion2 {
        return WooPosProductModelVersion2(
            remoteId = wcProduct.remoteProductId,
            parentId = wcProduct.parentId.takeIf { it != 0L },
            name = wcProduct.name,
            sku = wcProduct.sku,
            globalUniqueId = wcProduct.globalUniqueId,
            type = wooPosProductModelMapper.mapProductType(wcProduct.type),
            status = wooPosProductModelMapper.mapProductStatus(wcProduct.status),
            pricing = wooPosProductModelMapper.mapPricing(
                price = wcProduct.price.toBigDecimalOrNull(),
                regularPrice = wcProduct.regularPrice.toBigDecimalOrNull(),
                salePrice = wcProduct.salePrice.toBigDecimalOrNull(),
                isOnSale = wcProduct.onSale
            ),
            description = wcProduct.description,
            shortDescription = wcProduct.shortDescription,
            isDownloadable = wcProduct.downloadable,
            lastModified = wcProduct.dateModified,
            images = wooPosProductModelMapper.parseImages(wcProduct.images),
            attributes = wooPosProductModelMapper.parseAttributes(wcProduct.attributes),
            categories = wooPosProductModelMapper.parseCategories(wcProduct.categories),
            tags = wooPosProductModelMapper.parseTags(wcProduct.tags)
        )
    }

    private fun String.toBigDecimalOrNull(): BigDecimal? {
        return try {
            if (this.isNotBlank()) BigDecimal(this) else null
        } catch (e: NumberFormatException) {
            logger.e("Failed to parse price: '$this'", e)
            null
        }
    }
}
