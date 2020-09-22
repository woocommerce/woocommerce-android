package com.woocommerce.android.model

import android.os.Parcelable
import com.woocommerce.android.extensions.CASH_PAYMENTS
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.roundError
import com.woocommerce.android.model.Order.Item
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.ui.products.ProductHelper
import com.woocommerce.android.util.AddressUtils
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.util.DateTimeUtils
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP
import java.util.Date

@Parcelize
data class Order(
    val identifier: OrderIdentifier,
    val remoteId: Long,
    val number: String,
    val localSiteId: Int,
    val dateCreated: Date,
    val dateModified: Date,
    val datePaid: Date?,
    val status: CoreOrderStatus,
    val total: BigDecimal,
    val productsTotal: BigDecimal,
    val totalTax: BigDecimal,
    val shippingTotal: BigDecimal,
    val discountTotal: BigDecimal,
    val refundTotal: BigDecimal,
    val currency: String,
    val customerNote: String,
    val discountCodes: String,
    val paymentMethod: String,
    val paymentMethodTitle: String,
    val isCashPayment: Boolean,
    val pricesIncludeTax: Boolean,
    val multiShippingLinesAvailable: Boolean,
    val billingAddress: Address,
    val shippingAddress: Address,
    val shippingMethodList: List<String?>,
    val items: List<Item>
) : Parcelable {
    @IgnoredOnParcel
    val isOrderPaid = paymentMethodTitle.isEmpty() && datePaid == null

    @IgnoredOnParcel
    val isAwaitingPayment = status == CoreOrderStatus.PENDING ||
        status == CoreOrderStatus.ON_HOLD || datePaid == null

    @IgnoredOnParcel
    val isRefundAvailable = refundTotal < total

    @IgnoredOnParcel
    val availableRefundQuantity = items.sumBy { it.quantity }

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
        val quantity: Int,
        val subtotal: BigDecimal,
        val totalTax: BigDecimal,
        val total: BigDecimal,
        val variationId: Long
    ) : Parcelable {
        @IgnoredOnParcel
        val uniqueId: Long = ProductHelper.productOrVariationId(productId, variationId)
    }

    /*
     * Calculates the max quantity for each item by subtracting the number of already-refunded items
     */
    fun getMaxRefundQuantities(
        refunds: List<Refund>,
        unpackagedOrderItems: List<Item> = this.items
    ): Map<Long, Int> {
        val map = mutableMapOf<Long, Int>()
        val groupedRefunds = refunds.flatMap { it.items }.groupBy { it.uniqueId }
        unpackagedOrderItems.map { item ->
            map[item.uniqueId] = item.quantity - (groupedRefunds[item.uniqueId]?.sumBy { it.quantity } ?: 0)
        }
        return map
    }

    fun hasNonRefundedItems(refunds: List<Refund>): Boolean = getMaxRefundQuantities(refunds).values.any { it > 0 }

    fun hasUnpackagedProducts(shippingLabels: List<ShippingLabel>): Boolean {
        val productNames = mutableSetOf<String>()
        shippingLabels.map { productNames.addAll(it.productNames) }
        return this.items.size != productNames.size
    }

    /**
     * Returns products from an order that is not associated with any shipping labels
     * AND is also not refunded
     */
    fun getUnpackagedAndNonRefundedProducts(
        refunds: List<Refund>,
        shippingLabels: List<ShippingLabel>
    ): List<Item> {
        val productNames = mutableSetOf<String>()
        shippingLabels.map { productNames.addAll(it.productNames) }

        val unpackagedProducts = this.items.filter { !productNames.contains(it.name) }
        return getNonRefundedProducts(refunds, unpackagedProducts)
    }

    /**
     * Returns products that are not refunded in an order
     * @param [refunds] List of refunds for the order
     * @param [unpackagedProducts] list of products not associated with any shipping labels.
     * This is left null, in cases where we only want to fetch non refunded products from an order.
     */
    fun getNonRefundedProducts(
        refunds: List<Refund>,
        unpackagedProducts: List<Item> = this.items
    ): List<Item> {
        val leftoverProducts = getMaxRefundQuantities(refunds, unpackagedProducts).filter { it.value > 0 }
        val filteredItems = unpackagedProducts.filter { leftoverProducts.contains(it.uniqueId) }
            .map {
                val newQuantity = leftoverProducts[it.uniqueId]
                val quantity = it.quantity.toBigDecimal()
                val totalTax = if (quantity > BigDecimal.ZERO) {
                    it.totalTax.divide(quantity, 2, HALF_UP)
                } else BigDecimal.ZERO

                it.copy(
                    quantity = newQuantity ?: error("Missing product"),
                    total = it.price.times(newQuantity.toBigDecimal()),
                    totalTax = totalTax
                )
            }
        return filteredItems
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
}

fun WCOrderModel.toAppModel(): Order {
    return Order(
            OrderIdentifier(this),
            this.remoteOrderId,
            this.number,
            this.localSiteId,
            DateTimeUtils.dateUTCFromIso8601(this.dateCreated) ?: Date(),
            DateTimeUtils.dateUTCFromIso8601(this.dateModified) ?: Date(),
            DateTimeUtils.dateUTCFromIso8601(this.datePaid),
        CoreOrderStatus.fromValue(this.status) ?: CoreOrderStatus.PENDING,
            this.total.toBigDecimalOrNull()?.roundError() ?: BigDecimal.ZERO,
            this.getOrderSubtotal().toBigDecimal().roundError(),
            this.totalTax.toBigDecimalOrNull()?.roundError() ?: BigDecimal.ZERO,
            this.shippingTotal.toBigDecimalOrNull()?.roundError() ?: BigDecimal.ZERO,
            this.discountTotal.toBigDecimalOrNull()?.roundError() ?: BigDecimal.ZERO,
            -this.refundTotal.toBigDecimal().roundError(), // WCOrderModel.refundTotal is NEGATIVE
            this.currency,
            this.customerNote,
            this.discountCodes,
            this.paymentMethod,
            this.paymentMethodTitle,
            CASH_PAYMENTS.contains(this.paymentMethod),
            this.pricesIncludeTax,
            this.isMultiShippingLinesAvailable(),
            this.getBillingAddress().let {
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
            this.getShippingAddress().let {
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
            getShippingLineList().map { it.methodTitle },
            getLineItemList()
                    .filter { it.productId != null && it.id != null }
                    .map {
                        Item(
                                it.id!!,
                                it.productId!!,
                                it.name?.fastStripHtml() ?: "",
                                it.price?.toBigDecimalOrNull()?.roundError() ?: BigDecimal.ZERO,
                                it.sku ?: "",
                                it.quantity?.toInt() ?: 0,
                                it.subtotal?.toBigDecimalOrNull()?.roundError() ?: BigDecimal.ZERO,
                                it.totalTax?.toBigDecimalOrNull()?.roundError() ?: BigDecimal.ZERO,
                                it.total?.toBigDecimalOrNull()?.roundError() ?: BigDecimal.ZERO,
                                it.variationId ?: 0
                        )
                    }
    )
}

fun WCOrderStatusModel.toOrderStatus(): OrderStatus {
    return OrderStatus(
        this.statusKey,
        this.label
    )
}
