package com.woocommerce.android.model

import com.woocommerce.android.extensions.CASH_PAYMENTS
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.model.Order.Item
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.StringUtils
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.model.metadata.get
import org.wordpress.android.fluxc.model.order.FeeLineTaxStatus
import org.wordpress.android.fluxc.model.order.OrderAddress
import org.wordpress.android.fluxc.model.order.TaxLine
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderMappingConst.CHARGE_ID_KEY
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderMappingConst.SHIPPING_PHONE_KEY
import java.math.BigDecimal
import java.util.Date
import javax.inject.Inject
import org.wordpress.android.fluxc.model.order.CouponLine as WcCouponLine
import org.wordpress.android.fluxc.model.order.FeeLine as WCFeeLine
import org.wordpress.android.fluxc.model.order.LineItem as WCLineItem
import org.wordpress.android.fluxc.model.order.ShippingLine as WCShippingLine

class OrderMapper @Inject constructor(
    private val getLocations: GetLocations,
    private val dateUtils: DateUtils,
) {
    fun toAppModel(databaseEntity: OrderEntity): Order {
        val metaDataList = databaseEntity.metaData
        return Order(
            id = databaseEntity.orderId,
            number = databaseEntity.number,
            dateCreated = dateUtils.getDateUsingSiteTimeZone(databaseEntity.dateCreated) ?: Date(),
            dateModified = dateUtils.getDateUsingSiteTimeZone(databaseEntity.dateModified) ?: Date(),
            datePaid = dateUtils.getDateUsingSiteTimeZone(databaseEntity.datePaid),
            status = Order.Status.fromValue(databaseEntity.status),
            total = databaseEntity.total.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            productsTotal = databaseEntity.getOrderSubtotal().toBigDecimal(),
            totalTax = databaseEntity.totalTax.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            shippingTotal = databaseEntity.shippingTotal.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            discountTotal = databaseEntity.discountTotal.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            refundTotal = -(databaseEntity.refundTotal), // WCOrderModel.refundTotal is NEGATIVE
            currency = databaseEntity.currency,
            orderKey = databaseEntity.orderKey,
            customerNote = databaseEntity.customerNote,
            discountCodes = databaseEntity.discountCodes,
            paymentMethod = databaseEntity.paymentMethod,
            paymentMethodTitle = databaseEntity.paymentMethodTitle,
            isCashPayment = CASH_PAYMENTS.contains(databaseEntity.paymentMethod),
            pricesIncludeTax = databaseEntity.pricesIncludeTax,
            customer = Order.Customer(
                customerId = null,
                billingAddress = databaseEntity.getBillingAddress().mapAddress(),
                shippingAddress = databaseEntity.getShippingAddress().mapAddress(),
            ),
            shippingMethods = databaseEntity.getShippingLineList().mapShippingMethods(),
            items = databaseEntity.getLineItemList().mapLineItems(),
            shippingLines = databaseEntity.getShippingLineList().mapShippingLines(),
            feesLines = databaseEntity.getFeeLineList().mapFeesLines(),
            taxLines = databaseEntity.getTaxLineList().mapTaxLines(),
            couponLines = databaseEntity.getCouponLineList().mapCouponLines(),
            chargeId = metaDataList.getOrNull(CHARGE_ID_KEY),
            shippingPhone = metaDataList.getOrEmpty(SHIPPING_PHONE_KEY),
            paymentUrl = databaseEntity.paymentUrl,
            isEditable = databaseEntity.isEditable,
            selectedGiftCard = databaseEntity.giftCardCode,
            giftCardDiscountedAmount = databaseEntity.giftCardAmount
                .toBigDecimalOrNull() ?: BigDecimal.ZERO,
            shippingTax = databaseEntity.shippingTax.toBigDecimalOrNull() ?: BigDecimal.ZERO,
        )
    }

    private fun List<WCMetaData>.getOrNull(key: String): String? = get(key)?.valueAsString

    private fun List<WCMetaData>.getOrEmpty(key: String): String = getOrNull(key).orEmpty()

    private fun List<WCFeeLine>.mapFeesLines(): List<Order.FeeLine> = map {
        Order.FeeLine(
            id = it.id!!,
            name = it.name ?: StringUtils.EMPTY,
            totalTax = it.totalTax?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            total = it.total?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            taxStatus = when (it.taxStatus) {
                FeeLineTaxStatus.Taxable -> Order.FeeLine.FeeLineTaxStatus.TAXABLE
                FeeLineTaxStatus.None -> Order.FeeLine.FeeLineTaxStatus.NONE
                else -> Order.FeeLine.FeeLineTaxStatus.UNKNOWN
            }
        )
    }

    private fun List<TaxLine>.mapTaxLines(): List<Order.TaxLine> = map {
        Order.TaxLine(
            id = it.id!!,
            label = it.label!!,
            compound = it.compound ?: false,
            taxTotal = it.taxTotal ?: StringUtils.EMPTY,
            ratePercent = it.ratePercent ?: 0f,
            rateCode = it.rateCode ?: StringUtils.EMPTY,
        )
    }

    private fun List<WCShippingLine>.mapShippingLines(): List<Order.ShippingLine> = map {
        Order.ShippingLine(
            itemId = it.id!!,
            methodId = it.methodId,
            methodTitle = it.methodTitle ?: StringUtils.EMPTY,
            totalTax = it.totalTax?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            total = it.total?.toBigDecimalOrNull() ?: BigDecimal.ZERO
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
                    },
                    it.bundledBy?.toLongOrNull() ?: it.compositeParent?.toLongOrNull(),
                    configurationKey = it.configurationKey
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

    private fun Iterable<WcCouponLine>.mapCouponLines(): List<Order.CouponLine> = map {
        Order.CouponLine(
            code = it.code,
            id = it.id,
            discount = it.discount,
        )
    }
}
