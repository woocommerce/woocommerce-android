package com.woocommerce.android.model

import android.os.Parcelable
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.formatToString
import com.woocommerce.android.extensions.formatToYYYYmmDDhhmmss
import com.woocommerce.android.extensions.isEquivalentTo
import com.woocommerce.android.extensions.isNotSet
import com.woocommerce.android.extensions.isSet
import com.woocommerce.android.extensions.parseFromIso8601DateFormat
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
import java.util.Date

@Parcelize
@Suppress("LongParameterList")
open class ProductVariation(
    open val remoteProductId: Long,
    open val remoteVariationId: Long,
    open val sku: String,
    open val image: Image?,
    open val price: BigDecimal?,
    open val regularPrice: BigDecimal?,
    open val salePrice: BigDecimal?,
    open val saleEndDateGmt: Date?,
    open val saleStartDateGmt: Date?,
    open val isSaleScheduled: Boolean,
    open val stockStatus: ProductStockStatus,
    open val backorderStatus: ProductBackorderStatus,
    open val stockQuantity: Double,
    open var priceWithCurrency: String? = null,
    open val isPurchasable: Boolean,
    open val isVirtual: Boolean,
    open val isDownloadable: Boolean,
    open val isStockManaged: Boolean,
    open val description: String,
    open val isVisible: Boolean,
    open val shippingClass: String,
    open val shippingClassId: Long,
    open val menuOrder: Int,
    open val attributes: Array<VariantOption>,
    override val length: Float,
    override val width: Float,
    override val height: Float,
    override val weight: Float,
    open val minAllowedQuantity: Int?,
    open val maxAllowedQuantity: Int?,
    open val groupOfQuantity: Int?,
    open val overrideProductQuantities: Boolean?
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
                width == variation.width &&
                minAllowedQuantity == variation.minAllowedQuantity &&
                maxAllowedQuantity == variation.maxAllowedQuantity &&
                groupOfQuantity == variation.groupOfQuantity &&
                overrideProductQuantities == variation.overrideProductQuantities
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
                    json.addProperty(
                        /* property = */
                        "date_created_gmt",
                        /* value = */
                        variantImage.dateCreated?.formatToYYYYmmDDhhmmss() ?: ""
                    )
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
            it.minAllowedQuantity = minAllowedQuantity ?: -1
            it.maxAllowedQuantity = maxAllowedQuantity ?: -1
            it.groupOfQuantity = groupOfQuantity ?: -1
            it.overrideProductQuantities = overrideProductQuantities ?: false
            if (this is SubscriptionProductVariation) {
                // Subscription details are currently the only editable metadata fields from the app.
                it.metadata = subscriptionDetails?.toMetadataJson().toString()
            }
        }
    }

    fun getName(parentProduct: Product? = null): String {
        return parentProduct?.variationEnabledAttributes?.joinToString(" - ") { attribute ->
            val option = attributes.firstOrNull { it.name == attribute.name }
            option?.option ?: "Any ${attribute.name}"
        } ?: attributes.filter { it.option != null }.joinToString(" - ") { o -> o.option!! }
    }

    open fun copy(
        remoteProductId: Long = this.remoteProductId,
        remoteVariationId: Long = this.remoteVariationId,
        sku: String = this.sku,
        image: Image? = this.image,
        price: BigDecimal? = this.price,
        regularPrice: BigDecimal? = this.regularPrice,
        salePrice: BigDecimal? = this.salePrice,
        saleEndDateGmt: Date? = this.saleEndDateGmt,
        saleStartDateGmt: Date? = this.saleStartDateGmt,
        isSaleScheduled: Boolean = this.isSaleScheduled,
        stockStatus: ProductStockStatus = this.stockStatus,
        backorderStatus: ProductBackorderStatus = this.backorderStatus,
        stockQuantity: Double = this.stockQuantity,
        priceWithCurrency: String? = this.priceWithCurrency,
        isPurchasable: Boolean = this.isPurchasable,
        isVirtual: Boolean = this.isVirtual,
        isDownloadable: Boolean = this.isDownloadable,
        isStockManaged: Boolean = this.isStockManaged,
        description: String = this.description,
        isVisible: Boolean = this.isVisible,
        shippingClass: String = this.shippingClass,
        shippingClassId: Long = this.shippingClassId,
        menuOrder: Int = this.menuOrder,
        attributes: Array<VariantOption> = this.attributes,
        length: Float = this.length,
        width: Float = this.width,
        height: Float = this.height,
        weight: Float = this.weight,
        minAllowedQuantity: Int? = this.minAllowedQuantity,
        maxAllowedQuantity: Int? = this.maxAllowedQuantity,
        groupOfQuantity: Int? = this.groupOfQuantity,
        overrideProductQuantities: Boolean? = this.overrideProductQuantities
    ): ProductVariation {
        return ProductVariation(
            remoteProductId = remoteProductId,
            remoteVariationId = remoteVariationId,
            sku = sku,
            image = image,
            price = price,
            regularPrice = regularPrice,
            salePrice = salePrice,
            saleEndDateGmt = saleEndDateGmt,
            saleStartDateGmt = saleStartDateGmt,
            isSaleScheduled = isSaleScheduled,
            stockStatus = stockStatus,
            backorderStatus = backorderStatus,
            stockQuantity = stockQuantity,
            priceWithCurrency = priceWithCurrency,
            isPurchasable = isPurchasable,
            isVirtual = isVirtual,
            isDownloadable = isDownloadable,
            isStockManaged = isStockManaged,
            description = description,
            isVisible = isVisible,
            shippingClass = shippingClass,
            shippingClassId = shippingClassId,
            menuOrder = menuOrder,
            attributes = attributes,
            length = length,
            width = width,
            height = height,
            weight = weight,
            minAllowedQuantity = minAllowedQuantity,
            maxAllowedQuantity = maxAllowedQuantity,
            groupOfQuantity = groupOfQuantity,
            overrideProductQuantities = overrideProductQuantities

        )
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
        saleEndDateGmt = this.dateOnSaleToGmt.parseFromIso8601DateFormat(),
        saleStartDateGmt = this.dateOnSaleFromGmt.parseFromIso8601DateFormat(),
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
        weight = this.weight.toFloatOrNull() ?: 0f,
        minAllowedQuantity = if (this.minAllowedQuantity >= 0) this.minAllowedQuantity else null,
        maxAllowedQuantity = if (this.maxAllowedQuantity >= 0) this.maxAllowedQuantity else null,
        groupOfQuantity = if (this.groupOfQuantity >= 0) this.groupOfQuantity else null,
        overrideProductQuantities = this.overrideProductQuantities
    )
}
