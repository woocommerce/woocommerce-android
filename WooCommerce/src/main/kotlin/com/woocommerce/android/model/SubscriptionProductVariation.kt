package com.woocommerce.android.model

import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.isEquivalentTo
import com.woocommerce.android.extensions.parseFromIso8601DateFormat
import com.woocommerce.android.model.Product.Image
import com.woocommerce.android.ui.products.ProductBackorderStatus
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductStockStatus
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.util.DateTimeUtils
import java.math.BigDecimal
import java.util.Date

@Suppress("LongParameterList")
@Parcelize
class SubscriptionProductVariation(
    val subscriptionDetails: SubscriptionDetails?,
    override val remoteProductId: Long,
    override val remoteVariationId: Long,
    override val sku: String,
    override val image: Product.Image?,
    override val price: BigDecimal?,
    override val regularPrice: BigDecimal?,
    override val salePrice: BigDecimal?,
    override val saleEndDateGmt: Date?,
    override val saleStartDateGmt: Date?,
    override val isSaleScheduled: Boolean,
    override val stockStatus: ProductStockStatus,
    override val backorderStatus: ProductBackorderStatus,
    override val stockQuantity: Double,
    override var priceWithCurrency: String? = null,
    override val isPurchasable: Boolean,
    override val isVirtual: Boolean,
    override val isDownloadable: Boolean,
    override val isStockManaged: Boolean,
    override val description: String,
    override val isVisible: Boolean,
    override val shippingClass: String,
    override val shippingClassId: Long,
    override val menuOrder: Int,
    override val attributes: Array<VariantOption>,
    override val length: Float,
    override val width: Float,
    override val height: Float,
    override val weight: Float,
    override val minAllowedQuantity: Int?,
    override val maxAllowedQuantity: Int?,
    override val groupOfQuantity: Int?,
    override val overrideProductQuantities: Boolean?
) : ProductVariation(
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
) {
    constructor(model: WCProductVariationModel) :
        this(
            subscriptionDetails = model.metadata?.let { SubscriptionDetailsMapper.toAppModel(it) },
            remoteProductId = model.remoteProductId,
            remoteVariationId = model.remoteVariationId,
            sku = model.sku,
            image = model.getImageModel()?.let {
                Product.Image(
                    it.id,
                    it.name,
                    it.src,
                    DateTimeUtils.dateFromIso8601(model.dateCreated) ?: Date()
                )
            },
            price = model.price.toBigDecimalOrNull(),
            regularPrice = model.regularPrice.toBigDecimalOrNull(),
            salePrice = model.salePrice.toBigDecimalOrNull(),
            saleEndDateGmt = model.dateOnSaleToGmt.parseFromIso8601DateFormat(),
            saleStartDateGmt = model.dateOnSaleFromGmt.parseFromIso8601DateFormat(),
            isSaleScheduled = model.dateOnSaleFromGmt.isNotEmpty() || model.dateOnSaleToGmt.isNotEmpty(),
            stockStatus = ProductStockStatus.fromString(model.stockStatus),
            backorderStatus = ProductBackorderStatus.fromString(model.backorders),
            stockQuantity = model.stockQuantity,
            isPurchasable = model.purchasable,
            isVirtual = model.virtual,
            isDownloadable = model.downloadable,
            isStockManaged = model.manageStock,
            description = model.description.fastStripHtml(),
            isVisible = ProductStatus.fromString(model.status) == ProductStatus.PUBLISH,
            shippingClass = model.shippingClass,
            shippingClassId = model.shippingClassId.toLong(),
            menuOrder = model.menuOrder,
            attributes = model.attributeList
                ?.map { VariantOption(it) }
                ?.toTypedArray()
                ?: emptyArray(),
            length = model.length.toFloatOrNull() ?: 0f,
            width = model.width.toFloatOrNull() ?: 0f,
            height = model.height.toFloatOrNull() ?: 0f,
            weight = model.weight.toFloatOrNull() ?: 0f,
            minAllowedQuantity = if (model.minAllowedQuantity >= 0) model.minAllowedQuantity else null,
            maxAllowedQuantity = if (model.minAllowedQuantity >= 0) model.minAllowedQuantity else null,
            groupOfQuantity = if (model.minAllowedQuantity >= 0) model.minAllowedQuantity else null,
            overrideProductQuantities = model.overrideProductQuantities
        )

    @Suppress("ComplexMethod")
    override fun equals(other: Any?): Boolean {
        val variation = other as? SubscriptionProductVariation
        return variation?.let {
            subscriptionDetails == variation.subscriptionDetails &&
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

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (subscriptionDetails?.hashCode() ?: 0)
        result = 31 * result + remoteProductId.hashCode()
        result = 31 * result + remoteVariationId.hashCode()
        result = 31 * result + sku.hashCode()
        result = 31 * result + (image?.hashCode() ?: 0)
        result = 31 * result + (price?.hashCode() ?: 0)
        result = 31 * result + (regularPrice?.hashCode() ?: 0)
        result = 31 * result + (salePrice?.hashCode() ?: 0)
        result = 31 * result + (saleEndDateGmt?.hashCode() ?: 0)
        result = 31 * result + (saleStartDateGmt?.hashCode() ?: 0)
        result = 31 * result + isSaleScheduled.hashCode()
        result = 31 * result + stockStatus.hashCode()
        result = 31 * result + backorderStatus.hashCode()
        result = 31 * result + stockQuantity.hashCode()
        result = 31 * result + (priceWithCurrency?.hashCode() ?: 0)
        result = 31 * result + isPurchasable.hashCode()
        result = 31 * result + isVirtual.hashCode()
        result = 31 * result + isDownloadable.hashCode()
        result = 31 * result + isStockManaged.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + isVisible.hashCode()
        result = 31 * result + shippingClass.hashCode()
        result = 31 * result + shippingClassId.hashCode()
        result = 31 * result + menuOrder
        result = 31 * result + attributes.contentHashCode()
        result = 31 * result + length.hashCode()
        result = 31 * result + width.hashCode()
        result = 31 * result + height.hashCode()
        result = 31 * result + weight.hashCode()
        result = 31 * result + minAllowedQuantity.hashCode()
        result = 31 * result + maxAllowedQuantity.hashCode()
        result = 31 * result + groupOfQuantity.hashCode()
        result = 31 * result + overrideProductQuantities.hashCode()
        return result
    }

    override fun copy(
        remoteProductId: Long,
        remoteVariationId: Long,
        sku: String,
        image: Product.Image?,
        price: BigDecimal?,
        regularPrice: BigDecimal?,
        salePrice: BigDecimal?,
        saleEndDateGmt: Date?,
        saleStartDateGmt: Date?,
        isSaleScheduled: Boolean,
        stockStatus: ProductStockStatus,
        backorderStatus: ProductBackorderStatus,
        stockQuantity: Double,
        priceWithCurrency: String?,
        isPurchasable: Boolean,
        isVirtual: Boolean,
        isDownloadable: Boolean,
        isStockManaged: Boolean,
        description: String,
        isVisible: Boolean,
        shippingClass: String,
        shippingClassId: Long,
        menuOrder: Int,
        attributes: Array<VariantOption>,
        length: Float,
        width: Float,
        height: Float,
        weight: Float,
        minAllowedQuantity: Int?,
        maxAllowedQuantity: Int?,
        groupOfQuantity: Int?,
        overrideProductQuantities: Boolean?,
    ): ProductVariation {
        return SubscriptionProductVariation(
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
            subscriptionDetails = subscriptionDetails,
            minAllowedQuantity = minAllowedQuantity,
            maxAllowedQuantity = maxAllowedQuantity,
            groupOfQuantity = groupOfQuantity,
            overrideProductQuantities = overrideProductQuantities
        )
    }

    fun copy(
        subscriptionDetails: SubscriptionDetails? = this.subscriptionDetails,
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
    ): SubscriptionProductVariation {
        return SubscriptionProductVariation(
            subscriptionDetails = subscriptionDetails,
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
