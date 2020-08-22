package com.woocommerce.android.model

import android.os.Parcelable
import com.google.gson.JsonObject
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.formatDateToISO8601Format
import com.woocommerce.android.extensions.formatToString
import com.woocommerce.android.extensions.formatToYYYYmmDDhhmmss
import com.woocommerce.android.extensions.isEquivalentTo
import com.woocommerce.android.extensions.isNotSet
import com.woocommerce.android.extensions.roundError
import com.woocommerce.android.ui.products.ProductBackorderStatus
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductStatus.PRIVATE
import com.woocommerce.android.ui.products.ProductStatus.PUBLISH
import com.woocommerce.android.ui.products.ProductStockStatus
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.model.WCProductVariationModel.ProductVariantOption
import org.wordpress.android.util.DateTimeUtils
import java.math.BigDecimal
import java.util.Date

@Parcelize
data class ProductVariation(
    val remoteProductId: Long,
    val remoteVariationId: Long,
    val sku: String,
    val image: Product.Image?,
    val regularPrice: BigDecimal?,
    val salePrice: BigDecimal?,
    val saleEndDateGmt: Date?,
    val saleStartDateGmt: Date?,
    val isSaleScheduled: Boolean,
    val stockStatus: ProductStockStatus,
    val backorderStatus: ProductBackorderStatus,
    val stockQuantity: Int,
    val optionName: String,
    var priceWithCurrency: String? = null,
    val isPurchasable: Boolean,
    val isVirtual: Boolean,
    val isDownloadable: Boolean,
    val isStockManaged: Boolean,
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
        val variation = other as? ProductVariation
        return variation?.let {
            remoteVariationId == variation.remoteVariationId &&
                remoteProductId == variation.remoteProductId &&
                sku == variation.sku &&
                image?.id == variation.image?.id &&
                regularPrice isEquivalentTo variation.regularPrice &&
                salePrice isEquivalentTo variation.salePrice &&
                isSaleScheduled == variation.isSaleScheduled &&
                saleEndDateGmt == variation.saleEndDateGmt &&
                saleStartDateGmt == variation.saleStartDateGmt &&
                stockQuantity == variation.stockQuantity &&
                stockStatus == variation.stockStatus &&
                backorderStatus == variation.backorderStatus &&
                optionName.fastStripHtml() == variation.optionName.fastStripHtml() &&
                priceWithCurrency == variation.priceWithCurrency &&
                isPurchasable == variation.isPurchasable &&
                isVirtual == variation.isVirtual &&
                isDownloadable == variation.isDownloadable &&
                isStockManaged == variation.isStockManaged &&
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

    override fun hashCode(): Int {
        return super.hashCode()
    }

    fun toDataModel(cachedVariation: WCProductVariationModel? = null): WCProductVariationModel {
        fun imageToJson(): String {
            return image?.let { variantImage ->
                JsonObject().also { json ->
                    json.addProperty("id", variantImage.id)
                    json.addProperty("name", variantImage.name)
                    json.addProperty("src", variantImage.source)
                    json.addProperty("date_created_gmt", variantImage.dateCreated.formatToYYYYmmDDhhmmss())
                }.toString()
            } ?: ""
        }

        return (cachedVariation ?: WCProductVariationModel()).also {
            it.remoteProductId = remoteProductId
            it.remoteVariationId = remoteVariationId
            it.sku = sku
            it.image = imageToJson()
            it.regularPrice = if (regularPrice.isNotSet()) "" else regularPrice.toString()
            it.salePrice = if (salePrice.isNotSet()) "" else salePrice.toString()
            if (isSaleScheduled) {
                saleStartDateGmt?.let { dateOnSaleFrom ->
                    it.dateOnSaleFromGmt = dateOnSaleFrom.formatToYYYYmmDDhhmmss()
                }
                it.dateOnSaleToGmt = saleEndDateGmt?.formatToYYYYmmDDhhmmss() ?: ""
            } else {
                it.dateOnSaleFromGmt = ""
                it.dateOnSaleToGmt = ""
            }
            it.stockStatus = ProductStockStatus.fromStockStatus(stockStatus)
            it.backorders = ProductBackorderStatus.fromBackorderStatus(backorderStatus)
            it.stockQuantity = stockQuantity
            it.purchasable = isPurchasable
            it.virtual = isVirtual
            it.downloadable = isDownloadable
            it.manageStock = isStockManaged
            it.description = description
            it.status = if (isVisible) PUBLISH.value else PRIVATE.value
            it.shippingClass = shippingClass
            it.shippingClassId = shippingClassId.toInt()
            it.length = if (length == 0f) "" else length.formatToString()
            it.width = if (width == 0f) "" else width.formatToString()
            it.weight = if (weight == 0f) "" else weight.formatToString()
            it.height = if (height == 0f) "" else height.formatToString()
        }
    }
}

fun WCProductVariationModel.toAppModel(): ProductVariation {
    return ProductVariation(
        this.remoteProductId,
        this.remoteVariationId,
        this.sku,
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
        ProductStockStatus.fromString(this.stockStatus),
        ProductBackorderStatus.fromString(this.backorders),
        this.stockQuantity,
        getAttributeOptionName(this.getProductVariantOptions()),
        isPurchasable = this.purchasable,
        isDownloadable = this.downloadable,
        isVirtual = this.virtual,
        isStockManaged = this.manageStock,
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
 * returns the product variation combination name in the format {option1} - {option2}
 */
private fun getAttributeOptionName(variationOptions: List<ProductVariantOption>): String {
    var optionName = ""
    for (variationOption in variationOptions) {
        if (!variationOption.option.isNullOrEmpty()) {
            if (optionName.isNotEmpty()) {
                optionName += " - "
            }
            optionName += "${variationOption.option}"
        }
    }
    return optionName
}
