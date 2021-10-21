package com.woocommerce.android.model

import android.os.Parcelable
import com.woocommerce.android.extensions.*
import com.woocommerce.android.model.Order.*
import com.woocommerce.android.ui.products.ProductHelper
import com.woocommerce.android.util.AddressUtils
import com.woocommerce.android.util.StringUtils
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.util.DateTimeUtils
import java.math.BigDecimal
import java.util.*

@Parcelize
data class Order(
    @Deprecated(replaceWith = ReplaceWith("id"), message = "Use local id to identify order.")
    val identifier: OrderIdentifier,
    private val rawLocalOrderId: Int,
    @Deprecated(replaceWith = ReplaceWith("id"), message = "Use local id to identify order.")
    val remoteId: Long,
    val number: String,
    val dateCreated: Date,
    val dateModified: Date,
    val datePaid: Date?,
    val status: Status,
    val total: BigDecimal,
    val productsTotal: BigDecimal,
    val totalTax: BigDecimal,
    val shippingTotal: BigDecimal,
    val discountTotal: BigDecimal,
    val refundTotal: BigDecimal,
    val feesTotal: BigDecimal,
    val currency: String,
    val orderKey: String,
    val customerNote: String,
    val discountCodes: String,
    val paymentMethod: String,
    val paymentMethodTitle: String,
    val isCashPayment: Boolean,
    val pricesIncludeTax: Boolean,
    val multiShippingLinesAvailable: Boolean,
    val billingAddress: Address,
    val shippingAddress: Address,
    val shippingMethods: List<ShippingMethod>,
    val items: List<Item>,
    val shippingLines: List<ShippingLine>,
    val metaData: List<MetaData<String>>
) : Parcelable {
    val localId
        get() = LocalOrRemoteId.LocalId(this.rawLocalOrderId)

    @IgnoredOnParcel
    val isOrderPaid = datePaid != null

    // Allow refunding only integer quantities
    @IgnoredOnParcel
    val availableRefundQuantity = items.sumByFloat { it.quantity }.toInt()

    @IgnoredOnParcel
    val isRefundAvailable = refundTotal < total && availableRefundQuantity > 0

    @Parcelize
    data class ShippingMethod(
        val id: String,
        val title: String,
        val total: BigDecimal,
        val tax: BigDecimal
    ) : Parcelable

    @Parcelize
    data class OrderStatus(
        val statusKey: String,
        val label: String
    ) : Parcelable

    @Parcelize
    data class Item(
        val itemId: Long,
        val productId: Long,
        val name: String,
        val price: BigDecimal,
        val sku: String,
        val quantity: Float,
        val subtotal: BigDecimal,
        val totalTax: BigDecimal,
        val total: BigDecimal,
        val variationId: Long,
        val attributesList: List<Attribute>
    ) : Parcelable {
        @IgnoredOnParcel
        val uniqueId: Long = ProductHelper.productOrVariationId(productId, variationId)

        @IgnoredOnParcel
        val isVariation: Boolean = variationId != 0L

        @IgnoredOnParcel
        var containsAddons = false

        @IgnoredOnParcel
        val attributesNames = attributesList.map { it.addonName }

        /**
         * @return a comma-separated list of attribute values for display
         */
        val attributesDescription
            get() = attributesList.filter {
                it.value.isNotEmpty() && it.key.isNotEmpty() && it.isNotInternalAttributeData
            }.joinToString {
                it.value.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }

        @Parcelize
        data class Attribute(
            val key: String,
            val value: String
        ) : Parcelable {
            companion object {
                private const val addonAttributeGroupSize = 3
            }

            @IgnoredOnParcel
            private val attributeAddonKeyRegex = "(.*?) \\((.*?)\\)".toRegex()

            @IgnoredOnParcel
            private val keyAsAddonRegexGroup = attributeAddonKeyRegex
                .findAll(key)
                .firstOrNull()?.groupValues
                ?.takeIf { it.size == addonAttributeGroupSize }
                ?.toMutableList()
                ?.apply { removeFirst() }

            @IgnoredOnParcel
            val addonName = keyAsAddonRegexGroup
                ?.first()
                ?: key

            @IgnoredOnParcel
            val asAddonPrice = keyAsAddonRegexGroup
                ?.last()
                .orEmpty()

            // Don't include empty or the "_reduced_stock" key
            // skipping "_reduced_stock" is a temporary workaround until "type" is added to the response.
            val isNotInternalAttributeData
                get() = key.first().toString() != "_"
        }
    }

    @Parcelize
    data class ShippingLine(
        val itemId: Long,
        val methodId: String,
        val methodTitle: String,
        val totalTax: BigDecimal,
        val total: BigDecimal
    ) : Parcelable

    fun getBillingName(defaultValue: String): String {
        return when {
            billingAddress.firstName.isEmpty() && billingAddress.lastName.isEmpty() -> defaultValue
            billingAddress.firstName.isEmpty() -> billingAddress.lastName
            else -> "${billingAddress.firstName} ${billingAddress.lastName}"
        }
    }

    fun formatBillingInformationForDisplay(): String {
        val billingName = getBillingName("")
        val billingAddress = this.billingAddress.getEnvelopeAddress()
        val billingCountry = AddressUtils.getCountryLabelByCountryCode(this.billingAddress.country)
        return this.billingAddress.getFullAddress(
            billingName, billingAddress, billingCountry
        )
    }

    fun formatShippingInformationForDisplay(): String {
        val shippingName = "${shippingAddress.firstName} ${shippingAddress.lastName}"
        val shippingAddress = this.shippingAddress.getEnvelopeAddress()
        val shippingCountry = AddressUtils.getCountryLabelByCountryCode(this.shippingAddress.country)
        return this.shippingAddress.getFullAddress(
            shippingName, shippingAddress, shippingCountry
        )
    }

    fun getProductIds() = items.map { it.productId }

    sealed class Status(val value: String) : Parcelable {
        companion object {
            fun fromValue(value: String): Status {
                return fromDataModel(CoreOrderStatus.fromValue(value)) ?: Custom(value)
            }

            fun fromDataModel(status: CoreOrderStatus?): Status? {
                return when (status) {
                    CoreOrderStatus.PENDING -> Pending
                    CoreOrderStatus.PROCESSING -> Processing
                    CoreOrderStatus.ON_HOLD -> OnHold
                    CoreOrderStatus.COMPLETED -> Completed
                    CoreOrderStatus.CANCELLED -> Cancelled
                    CoreOrderStatus.REFUNDED -> Refunded
                    CoreOrderStatus.FAILED -> Failed
                    else -> null
                }
            }
        }

        override fun toString(): String {
            return value
        }

        @Parcelize
        object Pending : Status(CoreOrderStatus.PENDING.value)

        @Parcelize
        object Processing : Status(CoreOrderStatus.PROCESSING.value)

        @Parcelize
        object OnHold : Status(CoreOrderStatus.ON_HOLD.value)

        @Parcelize
        object Completed : Status(CoreOrderStatus.COMPLETED.value)

        @Parcelize
        object Cancelled : Status(CoreOrderStatus.CANCELLED.value)

        @Parcelize
        object Refunded : Status(CoreOrderStatus.REFUNDED.value)

        @Parcelize
        object Failed : Status(CoreOrderStatus.FAILED.value)

        @Parcelize
        data class Custom(private val customValue: String) : Status(customValue)
    }
}

fun WCOrderModel.toAppModel(): Order {
    return Order(
        identifier = OrderIdentifier(this),
        rawLocalOrderId = this.id,
        remoteId = this.remoteOrderId,
        number = this.number,
        dateCreated = DateTimeUtils.dateUTCFromIso8601(this.dateCreated) ?: Date(),
        dateModified = DateTimeUtils.dateUTCFromIso8601(this.dateModified) ?: Date(),
        datePaid = DateTimeUtils.dateUTCFromIso8601(this.datePaid),
        status = Status.fromValue(status),
        total = this.total.toBigDecimalOrNull()?.roundError() ?: BigDecimal.ZERO,
        productsTotal = this.getOrderSubtotal().toBigDecimal().roundError(),
        totalTax = this.totalTax.toBigDecimalOrNull()?.roundError() ?: BigDecimal.ZERO,
        shippingTotal = this.shippingTotal.toBigDecimalOrNull()?.roundError() ?: BigDecimal.ZERO,
        discountTotal = this.discountTotal.toBigDecimalOrNull()?.roundError() ?: BigDecimal.ZERO,
        refundTotal = -this.refundTotal.toBigDecimal().roundError(), // WCOrderModel.refundTotal is NEGATIVE
        feesTotal = this.getFeeLineList()
            .sumByBigDecimal { it.total?.toBigDecimalOrNull()?.roundError() ?: BigDecimal.ZERO },
        currency = this.currency,
        orderKey = this.orderKey,
        customerNote = this.customerNote,
        discountCodes = this.discountCodes,
        paymentMethod = this.paymentMethod,
        paymentMethodTitle = this.paymentMethodTitle,
        isCashPayment = CASH_PAYMENTS.contains(this.paymentMethod),
        pricesIncludeTax = this.pricesIncludeTax,
        multiShippingLinesAvailable = this.isMultiShippingLinesAvailable(),
        billingAddress = this.getBillingAddress().let {
            Address(
                it.company,
                it.firstName,
                it.lastName,
                this.billingPhone,
                it.country,
                it.state,
                it.address1,
                it.address2,
                it.city,
                it.postcode,
                this.billingEmail
            )
        },
        shippingAddress = this.getShippingAddress().let {
            Address(
                it.company,
                it.firstName,
                it.lastName,
                "",
                it.country,
                it.state,
                it.address1,
                it.address2,
                it.city,
                it.postcode,
                ""
            )
        },
        shippingMethods = getShippingLineList().filter { it.methodId != null && it.methodTitle != null }.map {
            ShippingMethod(
                it.methodId!!,
                it.methodTitle!!,
                it.total?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                it.totalTax?.toBigDecimalOrNull() ?: BigDecimal.ZERO
            )
        },
        items = getLineItemList()
            .filter { it.productId != null && it.id != null }
            .map {
                Item(
                    it.id!!,
                    it.productId!!,
                    it.parentName?.fastStripHtml() ?: it.name?.fastStripHtml() ?: StringUtils.EMPTY,
                    it.price?.toBigDecimalOrNull()?.roundError() ?: BigDecimal.ZERO,
                    it.sku ?: "",
                    it.quantity ?: 0f,
                    it.subtotal?.toBigDecimalOrNull()?.roundError() ?: BigDecimal.ZERO,
                    it.totalTax?.toBigDecimalOrNull()?.roundError() ?: BigDecimal.ZERO,
                    it.total?.toBigDecimalOrNull()?.roundError() ?: BigDecimal.ZERO,
                    it.variationId ?: 0,
                    it.getAttributeList().map { attribute ->
                        Item.Attribute(attribute.key.orEmpty(), attribute.value.orEmpty())
                    }
                )
            },
        shippingLines = getShippingLineList().map {
            ShippingLine(
                it.id!!,
                it.methodId ?: StringUtils.EMPTY,
                it.methodTitle ?: StringUtils.EMPTY,
                it.totalTax?.toBigDecimalOrNull()?.roundError() ?: BigDecimal.ZERO,
                it.total?.toBigDecimalOrNull()?.roundError() ?: BigDecimal.ZERO
            )
        },
        metaData = getMetaDataList().mapNotNull { it.toAppModel() }
    )
}

fun WCOrderStatusModel.toOrderStatus(): OrderStatus {
    return OrderStatus(
        this.statusKey,
        this.label
    )
}
