package com.woocommerce.android.model

import android.os.Parcelable
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.sumByBigDecimal
import com.woocommerce.android.extensions.sumByFloat
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.ui.orders.creation.configuration.ProductConfiguration
import com.woocommerce.android.ui.products.ProductHelper
import com.woocommerce.android.util.AddressUtils
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import java.math.BigDecimal
import java.util.Date
import java.util.Locale

@Parcelize
data class Order(
    val id: Long,
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
    val currency: String,
    val orderKey: String,
    val customerNote: String,
    val discountCodes: String,
    val paymentMethod: String,
    val paymentMethodTitle: String,
    val isCashPayment: Boolean,
    val pricesIncludeTax: Boolean,
    val customer: Customer?,
    val shippingMethods: List<ShippingMethod>,
    val items: List<Item>,
    val shippingLines: List<ShippingLine>,
    val feesLines: List<FeeLine>,
    val couponLines: List<CouponLine>,
    val taxLines: List<TaxLine>,
    val chargeId: String?,
    val shippingPhone: String,
    val paymentUrl: String,
    val isEditable: Boolean,
    val selectedGiftCard: String?,
    val giftCardDiscountedAmount: BigDecimal?,
    val shippingTax: BigDecimal,
) : Parcelable {
    @IgnoredOnParcel
    val isOrderPaid = datePaid != null

    @IgnoredOnParcel
    val isOrderFullyRefunded = refundTotal >= total

    // Allow refunding only integer quantities
    @IgnoredOnParcel
    val quantityOfItemsWhichPossibleToRefund = items.sumByFloat { it.quantity }.toInt() + feesLines.count()

    @IgnoredOnParcel
    val isRefundAvailable = !isOrderFullyRefunded && quantityOfItemsWhichPossibleToRefund > 0 && isOrderPaid

    @IgnoredOnParcel
    val billingName
        get() = getBillingName("")

    val hasMultipleShippingLines: Boolean
        get() = shippingLines.size > 1

    val hasMultipleFeeLines: Boolean
        get() = feesLines.size > 1

    @IgnoredOnParcel
    val feesTotal = feesLines.sumByBigDecimal(FeeLine::total)

    @IgnoredOnParcel
    val billingAddress = customer?.billingAddress ?: Address.EMPTY

    @IgnoredOnParcel
    val shippingAddress = customer?.shippingAddress ?: Address.EMPTY

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
        val attributesList: List<Attribute>,
        val parent: Long? = null,
        val configuration: ProductConfiguration? = null,
        val configurationKey: Long? = null
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
                it.value.isNotEmpty() && it.key.isNotEmpty()
            }.joinToString { attribute ->
                attribute.value
                    .fastStripHtml()
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
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
        }
    }

    @Parcelize
    data class ShippingLine(
        val itemId: Long,
        val methodId: String?,
        val methodTitle: String,
        val totalTax: BigDecimal,
        val total: BigDecimal
    ) : Parcelable {
        constructor(methodId: String, methodTitle: String, total: BigDecimal) :
            this(0L, methodId, methodTitle, BigDecimal.ZERO, total)
    }

    @Parcelize
    data class TaxLine(
        val id: Long,
        val label: String,
        val compound: Boolean,
        val taxTotal: String,
        val ratePercent: Float,
        val rateCode: String
    ) : Parcelable

    @Parcelize
    data class FeeLine(
        val id: Long,
        val name: String?,
        val total: BigDecimal,
        val totalTax: BigDecimal,
        var taxStatus: FeeLineTaxStatus,
    ) : Parcelable {
        fun getTotalValue(): BigDecimal = total + totalTax

        companion object {
            val EMPTY = FeeLine(
                id = 0,
                name = "",
                total = BigDecimal.ZERO,
                totalTax = BigDecimal.ZERO,
                taxStatus = FeeLineTaxStatus.UNKNOWN,
            )
        }

        enum class FeeLineTaxStatus {
            TAXABLE, NONE, UNKNOWN,
        }
    }

    @Parcelize
    data class CouponLine(
        val code: String,
        val id: Long? = null,
        val discount: String? = null,
    ) : Parcelable

    @Parcelize
    data class Customer(
        val customerId: Long? = null,
        val firstName: String? = null,
        val lastName: String? = null,
        val email: String? = null,
        val billingAddress: Address,
        val shippingAddress: Address,
        val username: String? = null,
    ) : Parcelable {
        companion object {
            val EMPTY = Customer(
                customerId = null,
                firstName = null,
                lastName = null,
                email = null,
                billingAddress = Address.EMPTY,
                shippingAddress = Address.EMPTY,
            )
        }
    }

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
            billingName,
            billingAddress,
            billingCountry
        )
    }

    fun formatShippingInformationForDisplay(): String {
        val shippingName = "${shippingAddress.firstName} ${shippingAddress.lastName}"
        val shippingAddress = this.shippingAddress.getEnvelopeAddress()
        val shippingCountry = AddressUtils.getCountryLabelByCountryCode(this.shippingAddress.country.code)
        return this.shippingAddress.getFullAddress(
            shippingName,
            shippingAddress,
            shippingCountry
        )
    }

    fun getProductIds() = items.map { it.productId }

    fun isEmpty() = this.copy(
        currency = "",
        dateCreated = DEFAULT_EMPTY_ORDER.dateCreated,
        dateModified = DEFAULT_EMPTY_ORDER.dateModified
    ) == DEFAULT_EMPTY_ORDER

    sealed class Status(val value: String) : Parcelable {
        companion object {
            const val AUTO_DRAFT = "auto-draft"

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
        private val DEFAULT_EMPTY_ORDER by lazy {
            Order(
                id = 0,
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
                currency = "",
                orderKey = "",
                customerNote = "",
                discountCodes = "",
                paymentMethod = "",
                paymentMethodTitle = "",
                isCashPayment = false,
                pricesIncludeTax = false,
                customer = null,
                shippingMethods = emptyList(),
                items = emptyList(),
                shippingLines = emptyList(),
                chargeId = "",
                feesLines = emptyList(),
                taxLines = emptyList(),
                couponLines = emptyList(),
                shippingPhone = "",
                paymentUrl = "",
                isEditable = true,
                selectedGiftCard = "",
                giftCardDiscountedAmount = null,
                shippingTax = BigDecimal(0)
            )
        }

        fun getEmptyOrder(dateCreated: Date, dateModified: Date) =
            DEFAULT_EMPTY_ORDER.copy(dateCreated = dateCreated, dateModified = dateModified)
    }
}

fun WCOrderStatusModel.toOrderStatus(): OrderStatus {
    return OrderStatus(
        this.statusKey,
        this.label
    )
}
