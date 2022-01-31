package com.woocommerce.android.model

import com.woocommerce.android.extensions.CASH_PAYMENTS
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.sumByBigDecimal
import com.woocommerce.android.model.Order.Item
import com.woocommerce.android.util.StringUtils
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.OrderAddress
import org.wordpress.android.fluxc.model.order.TaxLine
import org.wordpress.android.util.DateTimeUtils
import java.math.BigDecimal
import java.util.*
import javax.inject.Inject
import org.wordpress.android.fluxc.model.order.FeeLine as WCFeeLine
import org.wordpress.android.fluxc.model.order.LineItem as WCLineItem
import org.wordpress.android.fluxc.model.order.ShippingLine as WCShippingLine

class OrderMapper @Inject constructor(private val getLocations: GetLocations) {
    fun toAppModel(databaseEntity: WCOrderModel): Order {
        @Suppress("DEPRECATION_ERROR")
        return Order(
            rawLocalOrderId = databaseEntity.id,
            id = databaseEntity.remoteOrderId.value,
            number = databaseEntity.number,
            dateCreated = DateTimeUtils.dateUTCFromIso8601(databaseEntity.dateCreated) ?: Date(),
            dateModified = DateTimeUtils.dateUTCFromIso8601(databaseEntity.dateModified) ?: Date(),
            datePaid = DateTimeUtils.dateUTCFromIso8601(databaseEntity.datePaid),
            status = Order.Status.fromValue(databaseEntity.status),
            total = databaseEntity.total.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            productsTotal = databaseEntity.getOrderSubtotal().toBigDecimal(),
            totalTax = databaseEntity.totalTax.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            shippingTotal = databaseEntity.shippingTotal.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            discountTotal = databaseEntity.discountTotal.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            refundTotal = -(databaseEntity.refundTotal), // WCOrderModel.refundTotal is NEGATIVE
            feesTotal = databaseEntity.getFeeLineList()
                .sumByBigDecimal { it.total?.toBigDecimalOrNull() ?: BigDecimal.ZERO },
            currency = databaseEntity.currency,
            orderKey = databaseEntity.orderKey,
            customerNote = databaseEntity.customerNote,
            discountCodes = databaseEntity.discountCodes,
            paymentMethod = databaseEntity.paymentMethod,
            paymentMethodTitle = databaseEntity.paymentMethodTitle,
            isCashPayment = CASH_PAYMENTS.contains(databaseEntity.paymentMethod),
            pricesIncludeTax = databaseEntity.pricesIncludeTax,
            multiShippingLinesAvailable = databaseEntity.isMultiShippingLinesAvailable(),
            billingAddress = databaseEntity.getBillingAddress().mapAddress(),
            shippingAddress = databaseEntity.getShippingAddress().mapAddress(),
            shippingMethods = databaseEntity.getShippingLineList().mapShippingMethods(),
            items = databaseEntity.getLineItemList().mapLineItems(),
            shippingLines = databaseEntity.getShippingLineList().mapShippingLines(),
            feesLines = databaseEntity.getFeeLineList().mapFeesLines(),
            taxLines = databaseEntity.getTaxLineList().mapTaxLines(),
            metaData = databaseEntity.getMetaDataList().mapNotNull { it.toAppModel() }
        )
    }

    private fun List<WCFeeLine>.mapFeesLines(): List<Order.FeeLine> = map {
        Order.FeeLine(
            id = it.id!!,
            name = it.name ?: StringUtils.EMPTY,
            totalTax = it.totalTax?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            total = it.total?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
        )
    }

    private fun List<TaxLine>.mapTaxLines(): List<Order.TaxLine> = map {
        Order.TaxLine(
            id = it.id!!,
            compound = it.compound ?: false,
            taxTotal = it.taxTotal ?: StringUtils.EMPTY,
            ratePercent = it.ratePercent ?: 0f
        )
    }

    private fun List<WCShippingLine>.mapShippingLines(): List<Order.ShippingLine> = map {
        Order.ShippingLine(
            it.id!!,
            it.methodId ?: StringUtils.EMPTY,
            it.methodTitle ?: StringUtils.EMPTY,
            it.totalTax?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            it.total?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        )
    }

    private fun List<WCLineItem>.mapLineItems(): List<Item> =
        this.filter { it.productId != null && it.id != null }
            .map {
                Item(
                    it.id!!,
                    it.productId!!,
                    it.parentName?.fastStripHtml() ?: it.name?.fastStripHtml() ?: StringUtils.EMPTY,
                    it.price?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    it.sku ?: "",
                    it.quantity ?: 0f,
                    it.subtotal?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    it.totalTax?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    it.total?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    it.variationId ?: 0,
                    it.getAttributeList().map { attribute ->
                        Item.Attribute(attribute.key.orEmpty(), attribute.value.orEmpty())
                    }
                )
            }

    private fun List<WCShippingLine>.mapShippingMethods(): List<Order.ShippingMethod> =
        this.filter { it.methodId != null && it.methodTitle != null }
            .map {
                Order.ShippingMethod(
                    it.methodId!!,
                    it.methodTitle!!,
                    it.total?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    it.totalTax?.toBigDecimalOrNull() ?: BigDecimal.ZERO
                )
            }

    private fun OrderAddress.mapAddress(): Address {
        val (country, state) = getLocations(this.country, stateCode = this.state)
        return Address(
            company = this.company,
            firstName = this.firstName,
            lastName = this.lastName,
            country = country,
            state = state,
            address1 = this.address1,
            address2 = this.address2,
            city = this.city,
            postcode = this.postcode,
            phone = this.phone,
            email = when (this) {
                is OrderAddress.Shipping -> ""
                is OrderAddress.Billing -> this.email
            }
        )
    }
}
