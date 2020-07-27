package com.woocommerce.android.model

import android.os.Parcelable
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.formatDateToISO8601Format
import com.woocommerce.android.extensions.formatToYYYYmmDDhhmmss
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.extensions.isEquivalentTo
import com.woocommerce.android.extensions.roundError
import com.woocommerce.android.ui.products.ProductBackorderStatus
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductStatus.PRIVATE
import com.woocommerce.android.ui.products.ProductStatus.PUBLISH
import com.woocommerce.android.ui.products.ProductStockStatus
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.WCProductImageModel
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
    val isVisible: Boolean,
    val shippingClass: String,
    val shippingClassId: Long,
    override val length: Float,
    override val width: Float,
    override val height: Float,
    override val weight: Float
) : Parcelable, IProduct {
    override fun equals(other: Any?): Boolean {
        val variation = other as? ProductVariant
        return variation?.let {
            remoteVariationId == variation.remoteVariationId &&
                remoteProductId == variation.remoteProductId &&
                image?.id == variation.image?.id &&
                regularPrice isEquivalentTo variation.regularPrice &&
                salePrice isEquivalentTo variation.salePrice &&
                isOnSale == variation.isOnSale &&
                isSaleScheduled == variation.isSaleScheduled &&
                saleEndDateGmt == variation.saleEndDateGmt &&
                saleStartDateGmt == variation.saleStartDateGmt &&
                stockQuantity == variation.stockQuantity &&
                stockStatus == variation.stockStatus &&
                optionName.fastStripHtml() == variation.optionName.fastStripHtml() &&
                priceWithCurrency == variation.priceWithCurrency &&
                isPurchasable == variation.isPurchasable &&
                isVirtual == variation.isVirtual &&
                isDownloadable == variation.isDownloadable &&
                description.fastStripHtml() == variation.description.fastStripHtml() &&
                isVisible == variation.isVisible &&
                shippingClass == variation.shippingClass &&
                shippingClassId == variation.shippingClassId &&
                weight == variation.weight &&
                length == variation.length &&
                height == variation.height &&
                width == variation.width
        } ?: false
    }

    fun toDataModel(): WCProductVariationModel {
        fun imagesToJson(): String {
            return image?.let { variantImage ->
                JsonObject().also { json ->
                    json.addProperty("id", variantImage.id)
                    json.addProperty("name", variantImage.name)
                    json.addProperty("source", variantImage.source)
                }.toString()
            } ?: ""
        }

        return WCProductVariationModel().also {
            it.remoteProductId = remoteProductId
            it.remoteVariationId = remoteVariationId
            it.image = imagesToJson()
            it.regularPrice = if (regularPrice isEqualTo BigDecimal.ZERO) "" else regularPrice.toString()
            it.salePrice = if (salePrice isEqualTo BigDecimal.ZERO) "" else salePrice.toString()
            if (isSaleScheduled) {
                saleStartDateGmt?.let { dateOnSaleFrom ->
                    it.dateOnSaleFromGmt = dateOnSaleFrom.formatToYYYYmmDDhhmmss()
                }
                it.dateOnSaleToGmt = saleEndDateGmt?.formatToYYYYmmDDhhmmss() ?: ""
            } else {
                it.dateOnSaleFromGmt = ""
                it.dateOnSaleToGmt = ""
            }
            it.onSale = isOnSale
            it.stockStatus = ProductStockStatus.fromStockStatus(stockStatus)
            it.stockQuantity = stockQuantity
            it.purchasable = isPurchasable
            it.virtual = isVirtual
            it.downloadable = isDownloadable
            it.description = description
            it.status = if (isVisible) PUBLISH.value else PRIVATE.value
            it.shippingClass = shippingClass
            it.shippingClassId = shippingClassId.toInt()
            it.length = length.toString()
            it.width = width.toString()
            it.height = height.toString()
            it.weight = weight.toString()
        }
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
        description = this.description.fastStripHtml(),
        isVisible = ProductStatus.fromString(this.status) == PUBLISH,
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
