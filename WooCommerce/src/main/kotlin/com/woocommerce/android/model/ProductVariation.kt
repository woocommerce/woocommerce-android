package com.woocommerce.android.model

import android.os.Parcelable
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.woocommerce.android.extensions.*
import com.woocommerce.android.model.Product.Image
import com.woocommerce.android.ui.products.ProductBackorderStatus
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductStatus.PRIVATE
import com.woocommerce.android.ui.products.ProductStatus.PUBLISH
import com.woocommerce.android.ui.products.ProductStockStatus
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.util.DateTimeUtils
import java.math.BigDecimal
import java.util.*

@Parcelize
data class ProductVariation(
    val remoteProductId: Long,
    val remoteVariationId: Long,
    val sku: String,
    val image: Image?,
    val price: BigDecimal?,
    val regularPrice: BigDecimal?,
    val salePrice: BigDecimal?,
    val saleEndDateGmt: Date?,
    val saleStartDateGmt: Date?,
    val isSaleScheduled: Boolean,
    val stockStatus: ProductStockStatus,
    val backorderStatus: ProductBackorderStatus,
    val stockQuantity: Double,
    var priceWithCurrency: String? = null,
    val isPurchasable: Boolean,
    val isVirtual: Boolean,
    val isDownloadable: Boolean,
    val isStockManaged: Boolean,
    val description: String,
    val isVisible: Boolean,
    val shippingClass: String,
    val shippingClassId: Long,
    val menuOrder: Int,
    val attributes: Array<VariantOption>,
    override val length: Float,
    override val width: Float,
    override val height: Float,
    override val weight: Float
) : Parcelable, IProduct, Comparable<ProductVariation> {
    val isSaleInEffect: Boolean
        get() {
            val now = Date()
            return salePrice.isSet() &&
                (
                    !isSaleScheduled || (
                        (saleStartDateGmt == null || now.after(saleStartDateGmt)) &&
                            (saleEndDateGmt == null || now.before(saleEndDateGmt))
                        )
                    )
        }

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
                isPurchasable == variation.isPurchasable &&
                isVirtual == variation.isVirtual &&
                isDownloadable == variation.isDownloadable &&
                isStockManaged == variation.isStockManaged &&
                description.fastStripHtml() == variation.description.fastStripHtml() &&
                isVisible == variation.isVisible &&
                shippingClass == variation.shippingClass &&
                shippingClassId == variation.shippingClassId &&
                attributes.contentEquals(variation.attributes) &&
                weight == variation.weight &&
                length == variation.length &&
                height == variation.height &&
                width == variation.width
        } ?: false
    }

    override fun compareTo(other: ProductVariation): Int {
        val menuOrdering = menuOrder.compareTo(other.menuOrder)
        if (menuOrdering != 0) return menuOrdering
        return getName().compareTo(other.getName())
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
            it.menuOrder = menuOrder
            it.attributes = JsonArray().toString()
            attributes.takeIf { list -> list.isNotEmpty() }
                ?.forEach { variant -> it.addVariant(variant.asSourceModel()) }
            it.length = if (length == 0f) "" else length.formatToString()
            it.width = if (width == 0f) "" else width.formatToString()
            it.weight = if (weight == 0f) "" else weight.formatToString()
            it.height = if (height == 0f) "" else height.formatToString()
        }
    }

    fun getName(parentProduct: Product? = null): String {
        return parentProduct?.variationEnabledAttributes?.joinToString(" - ") { attribute ->
            val option = attributes.firstOrNull { it.name == attribute.name }
            option?.option ?: "Any ${attribute.name}"
        } ?: attributes.filter { it.option != null }.joinToString(" - ") { o -> o.option!! }
    }
}

@Parcelize
data class VariantOption(
    val id: Long?,
    val name: String?,
    val option: String?
) : Parcelable {
    constructor(sourceModel: WCProductVariationModel.ProductVariantOption) : this(
        id = sourceModel.id,
        name = sourceModel.name,
        option = sourceModel.option
    )

    companion object {
        val empty by lazy { VariantOption(null, null, null) }
    }

    fun asSourceModel() = WCProductVariationModel.ProductVariantOption(id, name, option)
}

fun WCProductVariationModel.toAppModel(): ProductVariation {
    return ProductVariation(
        remoteProductId = this.remoteProductId,
        remoteVariationId = this.remoteVariationId,
        sku = this.sku,
        image = this.getImageModel()?.let {
            Product.Image(
                it.id,
                it.name,
                it.src,
                DateTimeUtils.dateFromIso8601(this.dateCreated) ?: Date()
            )
        },
        price = this.price.toBigDecimalOrNull(),
        regularPrice = this.regularPrice.toBigDecimalOrNull(),
        salePrice = this.salePrice.toBigDecimalOrNull(),
        saleEndDateGmt = this.dateOnSaleToGmt.formatDateToISO8601Format(),
        saleStartDateGmt = this.dateOnSaleFromGmt.formatDateToISO8601Format(),
        isSaleScheduled = this.dateOnSaleFromGmt.isNotEmpty() || this.dateOnSaleToGmt.isNotEmpty(),
        stockStatus = ProductStockStatus.fromString(this.stockStatus),
        backorderStatus = ProductBackorderStatus.fromString(this.backorders),
        stockQuantity = this.stockQuantity,
        isPurchasable = this.purchasable,
        isVirtual = this.virtual,
        isDownloadable = this.downloadable,
        isStockManaged = this.manageStock,
        description = this.description.fastStripHtml(),
        isVisible = ProductStatus.fromString(this.status) == PUBLISH,
        shippingClass = this.shippingClass,
        shippingClassId = this.shippingClassId.toLong(),
        menuOrder = this.menuOrder,
        attributes = this.attributeList
            ?.map { VariantOption(it) }
            ?.toTypedArray()
            ?: emptyArray(),
        length = this.length.toFloatOrNull() ?: 0f,
        width = this.width.toFloatOrNull() ?: 0f,
        height = this.height.toFloatOrNull() ?: 0f,
        weight = this.weight.toFloatOrNull() ?: 0f
    )
}
