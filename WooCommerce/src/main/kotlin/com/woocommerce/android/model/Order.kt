package com.woocommerce.android.model

import android.os.Parcelable
import com.woocommerce.android.extensions.*
import com.woocommerce.android.model.Order.*
import com.woocommerce.android.ui.products.ProductHelper
import com.woocommerce.android.util.AddressUtils
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.order.TaxLine
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import java.math.BigDecimal
import java.util.*

@Parcelize
data class Order(
    val id: Long,
    @Deprecated(replaceWith = ReplaceWith("id"), message = "Use id to identify order.")
    val rawLocalOrderId: Int,
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
    val feesLines: List<FeeLine>,
    val taxLines: List<TaxLine>,
    val metaData: List<MetaData<String>>
) : Parcelable {
    @Deprecated(replaceWith = ReplaceWith("id"), message = "Use id to identify order.")
    val localId
        get() = LocalOrRemoteId.LocalId(this.rawLocalOrderId)

    @IgnoredOnParcel
    val isOrderPaid = datePaid != null

    // Allow refunding only integer quantities
    @IgnoredOnParcel
    val availableRefundQuantity = items.sumByFloat { it.quantity }.toInt() + feesLines.count()

    @IgnoredOnParcel
    val isRefundAvailable = refundTotal < total && availableRefundQuantity > 0

    @IgnoredOnParcel
    val chargeId
        get() = metaData.firstOrNull { it.key == "_charge_id" }?.value

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
        val pricePreDiscount = if (quantity == 0f) BigDecimal.ZERO else subtotal / quantity.toBigDecimal()

        @IgnoredOnParcel
        val discount = subtotal - total

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

        companion object {
            val EMPTY by lazy {
                Item(
                    itemId = 0,
                    productId = 0,
                    name = "",
                    price = BigDecimal(0),
                    sku = "",
                    quantity = 0f,
                    subtotal = BigDecimal(0),
                    totalTax = BigDecimal(0),
                    total = BigDecimal(0),
                    variationId = 0,
                    attributesList = emptyList()
                )
            }
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

    @Parcelize
    data class FeeLine(
        val id: Long,
        val name: String,
        val total: BigDecimal,
        val totalTax: BigDecimal,
    ) : Parcelable

    @Parcelize
    data class TaxLine(
        val id: Long,
        val compound: Boolean,
        val taxTotal: String,
        val ratePercent: Float
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
        val billingCountry = AddressUtils.getCountryLabelByCountryCode(this.billingAddress.country.code)
        return this.billingAddress.getFullAddress(
            billingName, billingAddress, billingCountry
        )
    }

    fun formatShippingInformationForDisplay(): String {
        val shippingName = "${shippingAddress.firstName} ${shippingAddress.lastName}"
        val shippingAddress = this.shippingAddress.getEnvelopeAddress()
        val shippingCountry = AddressUtils.getCountryLabelByCountryCode(this.shippingAddress.country.code)
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

    companion object {
        val EMPTY by lazy {
            Order(
                id = 0,
                rawLocalOrderId = 0,
                number = "",
                dateCreated = Date(),
                dateModified = Date(),
                datePaid = null,
                status = Status.Pending,
                total = BigDecimal(0),
                productsTotal = BigDecimal(0),
                totalTax = BigDecimal(0),
                shippingTotal = BigDecimal(0),
                discountTotal = BigDecimal(0),
                refundTotal = BigDecimal(0),
                feesTotal = BigDecimal(0),
                currency = "",
                orderKey = "",
                customerNote = "",
                discountCodes = "",
                paymentMethod = "",
                paymentMethodTitle = "",
                isCashPayment = false,
                pricesIncludeTax = false,
                multiShippingLinesAvailable = false,
                billingAddress = Address.EMPTY,
                shippingAddress = Address.EMPTY,
                shippingMethods = emptyList(),
                items = emptyList(),
                shippingLines = emptyList(),
                metaData = emptyList(),
                feesLines = emptyList(),
                taxLines = emptyList()
            )
        }
    }
}

fun WCOrderStatusModel.toOrderStatus(): OrderStatus {
    return OrderStatus(
        this.statusKey,
        this.label
    )
}
