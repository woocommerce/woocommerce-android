package com.woocommerce.android.model

import android.os.Parcelable
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.extensions.roundError
import com.woocommerce.android.model.ProductVariant.Type
import com.woocommerce.android.model.ProductVariant.Type.DOWNLOADABLE
import com.woocommerce.android.model.ProductVariant.Type.PHYSICAL
import com.woocommerce.android.model.ProductVariant.Type.VIRTUAL
import com.woocommerce.android.ui.products.ProductStockStatus
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.model.WCProductVariationModel.ProductVariantOption
import java.math.BigDecimal

@Parcelize
data class ProductVariant(
    val remoteProductId: Long,
    val remoteVariationId: Long,
    val imageUrl: String?,
    val price: BigDecimal?,
    val stockStatus: ProductStockStatus,
    val stockQuantity: Int,
    val optionName: String,
    var priceWithCurrency: String? = null,
    val isPurchasable: Boolean,
    val type: Type,
    val description: String
) : Parcelable {
    fun isSameVariant(variant: ProductVariant): Boolean {
        return remoteVariationId == variant.remoteVariationId &&
            remoteProductId == variant.remoteProductId &&
            imageUrl == variant.imageUrl &&
            price.isEqualTo(variant.price) &&
            stockQuantity == variant.stockQuantity &&
            stockStatus == variant.stockStatus &&
            optionName == variant.optionName &&
            priceWithCurrency == variant.priceWithCurrency &&
            isPurchasable == variant.isPurchasable &&
            type == variant.type &&
            description == variant.description
    }

    enum class Type {
        VIRTUAL,
        DOWNLOADABLE,
        PHYSICAL
    }
}

fun WCProductVariationModel.toAppModel(): ProductVariant {
    return ProductVariant(
        this.remoteProductId,
        this.remoteVariationId,
        this.imageUrl,
        this.price.toBigDecimalOrNull()?.roundError(),
        ProductStockStatus.fromString(this.stockStatus),
        this.stockQuantity,
        getAttributeOptionName(this.getProductVariantOptions()),
        isPurchasable = this.purchasable,
        type = getVariantType(this.virtual, this.downloadable),
        description = this.description
    )
}

/**
 * Given a list of [ProductVariantOption]
 * returns the product variant combination name in the format {option1} - {option2}
 */
private fun getAttributeOptionName(variantOptions: List<ProductVariantOption>): String {
    var optionName = ""
    for (variantOption in variantOptions) {
        if (!variantOption.option.isNullOrEmpty()) {
            if (optionName.isNotEmpty()) {
                optionName += " - "
            }
            optionName += "${variantOption.option}"
        }
    }
    return optionName
}

private fun getVariantType(isVirtual: Boolean, isDownloadable: Boolean): Type {
    return when {
        isVirtual -> VIRTUAL
        isDownloadable -> DOWNLOADABLE
        else -> PHYSICAL
    }
}
