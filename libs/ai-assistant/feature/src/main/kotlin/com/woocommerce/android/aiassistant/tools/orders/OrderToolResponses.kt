package com.woocommerce.android.aiassistant.tools.orders

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.wordpress.android.fluxc.model.order.CouponLine
import org.wordpress.android.fluxc.model.order.FeeLine
import org.wordpress.android.fluxc.model.order.LineItem
import org.wordpress.android.fluxc.model.order.TaxLine
import org.wordpress.android.fluxc.persistence.entity.OrderEntity

@Serializable
internal data class OrderDetailResponse(
    val id: Long,
    val number: String,
    val status: String,
    val total: String,
    val currency: String,
    @SerialName("date_created") val dateCreated: String,
    @SerialName("date_modified") val dateModified: String,
    @SerialName("payment_method_title") val paymentMethodTitle: String,
    @SerialName("customer_name") val customerName: String,
    @SerialName("customer_email") val customerEmail: String,
    @SerialName("customer_id") val customerId: Long,
    @SerialName("date_paid") val datePaid: String,
    @SerialName("customer_note") val customerNote: String,
    @SerialName("total_tax") val totalTax: String,
    @SerialName("shipping_total") val shippingTotal: String,
    @SerialName("discount_total") val discountTotal: String,
    @SerialName("line_items_count") val lineItemsCount: Int,
    @SerialName("line_items_truncated") val lineItemsTruncated: Boolean,
    @SerialName("line_items") val lineItems: List<CompactOrderLineItem>,
    val billing: CompactOrderAddress? = null,
    val shipping: CompactOrderAddress? = null,
    @SerialName("coupon_lines") val couponLines: List<CompactCouponLine>? = null,
    @SerialName("fee_lines") val feeLines: List<CompactFeeLine>? = null,
    @SerialName("tax_lines") val taxLines: List<CompactTaxLine>? = null,
)

@Serializable
internal data class CompactOrderLineItem(
    val id: Long? = null,
    val name: String? = null,
    val quantity: Float? = null,
    val sku: String? = null,
    val total: String? = null,
    @SerialName("product_id") val productId: Long? = null,
    @SerialName("variation_id") val variationId: Long? = null,
)

@Serializable
internal data class CompactOrderAddress(
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postcode: String? = null,
    val country: String? = null,
)

@Serializable
internal data class CompactCouponLine(
    val id: Long? = null,
    val code: String,
    val discount: String? = null,
    @SerialName("discount_tax") val discountTax: String? = null,
)

@Serializable
internal data class CompactFeeLine(
    val id: Long? = null,
    val name: String? = null,
    val total: String? = null,
    @SerialName("total_tax") val totalTax: String? = null,
    @SerialName("tax_status") val taxStatus: String? = null,
)

@Serializable
internal data class CompactTaxLine(
    val id: Long? = null,
    @SerialName("rate_id") val rateId: Long? = null,
    @SerialName("rate_code") val rateCode: String? = null,
    val label: String? = null,
    @SerialName("tax_total") val taxTotal: String? = null,
    @SerialName("shipping_tax_total") val shippingTaxTotal: String? = null,
)

@Serializable
internal data class OrderListRowResponse(
    val id: Long,
    val number: String,
    val status: String,
    val total: String,
    val currency: String,
    @SerialName("date_created") val dateCreated: String,
    @SerialName("customer_id") val customerId: Long,
    @SerialName("customer_name") val customerName: String,
    @SerialName("payment_method_title") val paymentMethodTitle: String? = null,
    @SerialName("customer_email") val customerEmail: String? = null,
    @SerialName("customer_note") val customerNote: String? = null,
    @SerialName("date_paid") val datePaid: String? = null,
    @SerialName("shipping_total") val shippingTotal: String? = null,
    @SerialName("discount_total") val discountTotal: String? = null,
    @SerialName("line_items_count") val lineItemsCount: Int? = null,
    @SerialName("line_items_truncated") val lineItemsTruncated: Boolean? = null,
    @SerialName("line_items") val lineItems: List<CompactOrderLineItem>? = null,
    val billing: CompactOrderAddress? = null,
    val shipping: CompactOrderAddress? = null,
)

internal suspend fun OrderEntity.toOrderDetailResponse(
    extraFields: Set<String> = emptySet(),
    lineItemsLimit: Int = ORDER_DETAIL_LINE_ITEMS_LIMIT,
): OrderDetailResponse {
    val allLineItems = getLineItemList()
    return OrderDetailResponse(
        id = orderId,
        number = number,
        status = status,
        total = total,
        currency = currency,
        dateCreated = dateCreated,
        dateModified = dateModified,
        paymentMethodTitle = paymentMethodTitle,
        customerName = customerName(),
        customerEmail = billingEmail,
        customerId = customerId,
        datePaid = datePaid,
        customerNote = customerNote,
        totalTax = totalTax,
        shippingTotal = shippingTotal,
        discountTotal = discountTotal,
        lineItemsCount = allLineItems.size,
        lineItemsTruncated = allLineItems.size > lineItemsLimit,
        lineItems = allLineItems.take(lineItemsLimit).map { it.toCompactLineItem() },
        billing = if ("billing" in extraFields) toCompactBillingAddress() else null,
        shipping = if ("shipping" in extraFields) toCompactShippingAddress() else null,
        couponLines = if ("coupon_lines" in extraFields) {
            getCouponLineList().take(ORDER_ADJUSTMENT_LINES_LIMIT).map { it.toCompactCouponLine() }
        } else {
            null
        },
        feeLines = if ("fee_lines" in extraFields) {
            getFeeLineList().take(ORDER_ADJUSTMENT_LINES_LIMIT).map { it.toCompactFeeLine() }
        } else {
            null
        },
        taxLines = if ("tax_lines" in extraFields) {
            getTaxLineList().take(ORDER_ADJUSTMENT_LINES_LIMIT).map { it.toCompactTaxLine() }
        } else {
            null
        },
    )
}

internal suspend fun OrderEntity.toOrderListRowResponse(
    extraFields: Set<String> = emptySet(),
    lineItemsLimit: Int = ORDER_LIST_LINE_ITEMS_LIMIT,
): OrderListRowResponse {
    val includeLineItems = "line_items" in extraFields
    val allLineItems = if (includeLineItems) getLineItemList() else emptyList()
    return OrderListRowResponse(
        id = orderId,
        number = number,
        status = status,
        total = total,
        currency = currency,
        dateCreated = dateCreated,
        customerId = customerId,
        customerName = customerName(),
        paymentMethodTitle = paymentMethodTitle.takeIf { "payment_method_title" in extraFields },
        customerEmail = billingEmail.takeIf { "customer_email" in extraFields },
        customerNote = customerNote.takeIf { "customer_note" in extraFields },
        datePaid = datePaid.takeIf { "date_paid" in extraFields },
        shippingTotal = shippingTotal.takeIf { "shipping_total" in extraFields },
        discountTotal = discountTotal.takeIf { "discount_total" in extraFields },
        lineItemsCount = if (includeLineItems) allLineItems.size else null,
        lineItemsTruncated = if (includeLineItems) allLineItems.size > lineItemsLimit else null,
        lineItems = if (includeLineItems) allLineItems.take(lineItemsLimit).map { it.toCompactLineItem() } else null,
        billing = if ("billing" in extraFields) toCompactBillingAddress() else null,
        shipping = if ("shipping" in extraFields) toCompactShippingAddress() else null,
    )
}

private fun OrderEntity.customerName(): String =
    listOf(billingFirstName, billingLastName).filter { it.isNotBlank() }.joinToString(" ")

private fun LineItem.toCompactLineItem() = CompactOrderLineItem(
    id = id,
    name = name,
    quantity = quantity,
    sku = sku,
    total = total,
    productId = productId,
    variationId = variationId,
)

private fun OrderEntity.toCompactBillingAddress() = CompactOrderAddress(
    firstName = billingFirstName.takeIf { it.isNotBlank() },
    lastName = billingLastName.takeIf { it.isNotBlank() },
    email = billingEmail.takeIf { it.isNotBlank() },
    phone = billingPhone.takeIf { it.isNotBlank() },
    city = billingCity.takeIf { it.isNotBlank() },
    state = billingState.takeIf { it.isNotBlank() },
    postcode = billingPostcode.takeIf { it.isNotBlank() },
    country = billingCountry.takeIf { it.isNotBlank() },
)

private fun OrderEntity.toCompactShippingAddress() = CompactOrderAddress(
    firstName = shippingFirstName.takeIf { it.isNotBlank() },
    lastName = shippingLastName.takeIf { it.isNotBlank() },
    phone = shippingPhone.takeIf { it.isNotBlank() },
    city = shippingCity.takeIf { it.isNotBlank() },
    state = shippingState.takeIf { it.isNotBlank() },
    postcode = shippingPostcode.takeIf { it.isNotBlank() },
    country = shippingCountry.takeIf { it.isNotBlank() },
)

private fun CouponLine.toCompactCouponLine() = CompactCouponLine(
    id = id,
    code = code,
    discount = discount,
    discountTax = discountTax,
)

private fun FeeLine.toCompactFeeLine() = CompactFeeLine(
    id = id,
    name = name,
    total = total,
    totalTax = totalTax,
    taxStatus = taxStatus?.value,
)

private fun TaxLine.toCompactTaxLine() = CompactTaxLine(
    id = id,
    rateId = rateId,
    rateCode = rateCode,
    label = label,
    taxTotal = taxTotal,
    shippingTaxTotal = shippingTaxTotal,
)

internal const val ORDER_DETAIL_LINE_ITEMS_LIMIT = 10
internal const val ORDER_LIST_LINE_ITEMS_LIMIT = 5
internal const val ORDER_ADJUSTMENT_LINES_LIMIT = 10
