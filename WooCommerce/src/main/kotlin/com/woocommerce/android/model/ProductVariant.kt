package com.woocommerce.android.model

import com.woocommerce.android.ui.products.ProductStockStatus
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.model.WCProductVariationModel.ProductVariantOption
import java.math.BigDecimal

data class ProductVariant(
    val remoteProductId: Long,
    val remoteVariationId: Long,
    val imageUrl: String?,
    val price: BigDecimal?,
    val stockStatus: ProductStockStatus,
    val stockQuantity: Int,
    val attributes: String?
)

fun WCProductVariationModel.toAppModel(): ProductVariant {
    return ProductVariant(
        this.remoteProductId,
        this.remoteVariationId,
        this.imageUrl,
        this.price.toBigDecimalOrNull(),
        ProductStockStatus.fromString(this.stockStatus),
        this.stockQuantity,
        getAttributeOptionName(this.getProductVariantOptions())
    )
}

/**
 * Given a list of [ProductVariantOption]
 * returns the product variant combination name in the format {option1} - {option2}
 */
private fun getAttributeOptionName(variantOptions: List<ProductVariantOption>): String? {
    var optionName = ""
    for (variantOption in variantOptions) {
        if (!variantOption.option.isNullOrEmpty()) {
            if (optionName.isNotEmpty()) {
                optionName += "-"
            }
            optionName += " ${variantOption.option} "
        }
    }
    return optionName
}
