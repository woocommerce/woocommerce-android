package com.woocommerce.android.model

import android.os.Parcelable
import com.woocommerce.android.extensions.formatDateToISO8601Format
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.extensions.roundError
import com.woocommerce.android.model.ProductVariant.Type
import com.woocommerce.android.model.ProductVariant.Type.DOWNLOADABLE
import com.woocommerce.android.model.ProductVariant.Type.PHYSICAL
import com.woocommerce.android.model.ProductVariant.Type.VIRTUAL
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductStockStatus
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.model.WCProductVariationModel.ProductVariantOption
import org.wordpress.android.util.DateTimeUtils
import java.math.BigDecimal
import java.util.Date

@Parcelize
data class ProductVariant(
    val remoteProductId: Long,
    val remoteVariationId: Long,
    val image: Product.Image?,
    val regularPrice: BigDecimal?,
    val salePrice: BigDecimal?,
    val saleEndDateGmt: Date?,
    val saleStartDateGmt: Date?,
    val isSaleScheduled: Boolean,
    val isOnSale: Boolean,
    val stockStatus: ProductStockStatus,
    val stockQuantity: Int,
    val optionName: String,
    var priceWithCurrency: String? = null,
    val isPurchasable: Boolean,
    val type: Type,
    val description: String,
    val status: ProductStatus?
) : Parcelable {
    fun isSameVariant(variant: ProductVariant): Boolean {
        return remoteVariationId == variant.remoteVariationId &&
            remoteProductId == variant.remoteProductId &&
            image == variant.image &&
            regularPrice.isEqualTo(variant.regularPrice) &&
            salePrice.isEqualTo(variant.salePrice) &&
            isOnSale == variant.isOnSale &&
            isSaleScheduled == variant.isSaleScheduled &&
            saleEndDateGmt == variant.saleEndDateGmt &&
            saleStartDateGmt == variant.saleStartDateGmt &&
            stockQuantity == variant.stockQuantity &&
            stockStatus == variant.stockStatus &&
            optionName == variant.optionName &&
            priceWithCurrency == variant.priceWithCurrency &&
            isPurchasable == variant.isPurchasable &&
            type == variant.type &&
            description == variant.description &&
            status == variant.status
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
        this.getImage()?.let {
            Product.Image(
                it.id,
                it.name,
                it.src,
                DateTimeUtils.dateFromIso8601(this.dateCreated) ?: Date()
            )
        },
        this.regularPrice.toBigDecimalOrNull()?.roundError(),
        this.salePrice.toBigDecimalOrNull()?.roundError(),
        this.dateOnSaleToGmt.formatDateToISO8601Format(),
        this.dateOnSaleFromGmt.formatDateToISO8601Format(),
        this.dateOnSaleFromGmt.isNotEmpty() || this.dateOnSaleToGmt.isNotEmpty(),
        this.onSale,
        ProductStockStatus.fromString(this.stockStatus),
        this.stockQuantity,
        getAttributeOptionName(this.getProductVariantOptions()),
        isPurchasable = this.purchasable,
        type = getVariantType(this.virtual, this.downloadable),
        description = this.description,
        status = ProductStatus.fromString(this.status)
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
