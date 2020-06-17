package com.woocommerce.android.model

import android.os.Parcelable
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.formatDateToISO8601Format
import com.woocommerce.android.extensions.isEquivalentTo
import com.woocommerce.android.extensions.roundError
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
    val isVirtual: Boolean,
    val isDownloadable: Boolean,
    val description: String,
    val status: ProductStatus?,
    val shippingClass: String,
    val shippingClassId: Long,
    override val length: Float,
    override val width: Float,
    override val height: Float,
    override val weight: Float
) : Parcelable, IProduct {
    fun isSameVariant(variant: ProductVariant): Boolean {
        return remoteVariationId == variant.remoteVariationId &&
            remoteProductId == variant.remoteProductId &&
            image?.id == variant.image?.id &&
            regularPrice isEquivalentTo variant.regularPrice &&
            salePrice isEquivalentTo variant.salePrice &&
            isOnSale == variant.isOnSale &&
            isSaleScheduled == variant.isSaleScheduled &&
            saleEndDateGmt == variant.saleEndDateGmt &&
            saleStartDateGmt == variant.saleStartDateGmt &&
            stockQuantity == variant.stockQuantity &&
            stockStatus == variant.stockStatus &&
            optionName.fastStripHtml() == variant.optionName.fastStripHtml() &&
            priceWithCurrency == variant.priceWithCurrency &&
            isPurchasable == variant.isPurchasable &&
            isVirtual == variant.isVirtual &&
            isDownloadable == variant.isDownloadable &&
            description == variant.description &&
            status == variant.status &&
            shippingClass == variant.shippingClass &&
            shippingClassId == variant.shippingClassId &&
            weight == variant.weight &&
            length == variant.length &&
            height == variant.height &&
            width == variant.width
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
        isDownloadable = this.downloadable,
        isVirtual = this.virtual,
        description = this.description,
        status = ProductStatus.fromString(this.status),
        shippingClass = this.shippingClass,
        shippingClassId = this.shippingClassId.toLong(),
        length = this.length.toFloatOrNull() ?: 0f,
        width = this.width.toFloatOrNull() ?: 0f,
        height = this.height.toFloatOrNull() ?: 0f,
        weight = this.weight.toFloatOrNull() ?: 0f
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
