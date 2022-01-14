package com.woocommerce.android.model

import com.woocommerce.android.extensions.CASH_PAYMENTS
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.sumByBigDecimal
import com.woocommerce.android.model.Order.Item
import com.woocommerce.android.util.StringUtils
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.order.OrderAddress
import org.wordpress.android.util.DateTimeUtils
import java.math.BigDecimal
import java.util.*
import javax.inject.Inject

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
            billingAddress = mapAddress(databaseEntity.getBillingAddress()),
            shippingAddress = mapAddress(databaseEntity.getShippingAddress()),
            shippingMethods = mapShippingMethods(databaseEntity),
            items = mapLineItems(databaseEntity),
            shippingLines = mapShippingLines(databaseEntity),
            feesLines = mapFeesLines(databaseEntity),
            metaData = databaseEntity.getMetaDataList().mapNotNull { it.toAppModel() }
        )
    }

    private fun mapFeesLines(databaseEntity: WCOrderModel) =
        databaseEntity.getFeeLineList().map {
            Order.FeeLine(
                id = it.id!!,
                name = it.name ?: StringUtils.EMPTY,
                totalTax = it.totalTax?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                total = it.total?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            )
        }

    private fun mapShippingLines(databaseEntity: WCOrderModel) =
        databaseEntity.getShippingLineList().map {
            Order.ShippingLine(
                it.id!!,
                it.methodId ?: StringUtils.EMPTY,
                it.methodTitle ?: StringUtils.EMPTY,
                it.totalTax?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                it.total?.toBigDecimalOrNull() ?: BigDecimal.ZERO
            )
        }

    private fun mapLineItems(databaseEntity: WCOrderModel) =
        databaseEntity.getLineItemList()
            .filter { it.productId != null && it.id != null }
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

    private fun mapShippingMethods(databaseEntity: WCOrderModel) =
        databaseEntity.getShippingLineList().filter { it.methodId != null && it.methodTitle != null }.map {
            Order.ShippingMethod(
                it.methodId!!,
                it.methodTitle!!,
                it.total?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                it.totalTax?.toBigDecimalOrNull() ?: BigDecimal.ZERO
            )
        }

    private fun mapAddress(databaseAddress: OrderAddress): Address {
        val (country, state) = getLocations(databaseAddress.country, stateCode = databaseAddress.state)
        return Address(
            company = databaseAddress.company,
            firstName = databaseAddress.firstName,
            lastName = databaseAddress.lastName,
            country = country,
            state = state,
            address1 = databaseAddress.address1,
            address2 = databaseAddress.address2,
            city = databaseAddress.city,
            postcode = databaseAddress.postcode,
            phone = databaseAddress.phone,
            email = when (databaseAddress) {
                is OrderAddress.Shipping -> ""
                is OrderAddress.Billing -> databaseAddress.email
            }
        )
    }
}
