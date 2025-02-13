package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.model.order.OrderAddress
import org.wordpress.android.fluxc.utils.DateUtils
import java.math.BigDecimal
import javax.inject.Inject

class OrderDtoMapper @Inject internal constructor(
    private val stripOrder: StripOrder,
    private val stripOrderMetaData: StripOrderMetaData
) {
    @Suppress("LongMethod", "ComplexMethod")
    fun toDatabaseEntity(orderDto: OrderDto, localSiteId: LocalId): Pair<OrderEntity, List<WCMetaData>> {
        fun convertDateToUTCString(date: String?): String =
                date?.let { DateUtils.formatGmtAsUtcDateString(it) } ?: "" // Store the date in UTC format

        val rawRemoteDataEntity = with(orderDto) {
            OrderEntity(
                    orderId = this.id ?: 0,
                    localSiteId = localSiteId,
                    number = this.number ?: (this.id ?: 0).toString(),
                    status = this.status ?: "",
                    currency = this.currency ?: "",
                    orderKey = this.order_key ?: "",
                    dateCreated = convertDateToUTCString(this.date_created_gmt),
                    dateModified = convertDateToUTCString(this.date_modified_gmt),
                    total = this.total ?: "",
                    totalTax = this.total_tax ?: "",
                    shippingTotal = this.shipping_total ?: "",
                    paymentMethod = this.payment_method ?: "",
                    paymentMethodTitle = this.payment_method_title ?: "",
                    datePaid = this.date_paid_gmt?.let { "${it}Z" } ?: "",
                    pricesIncludeTax = this.prices_include_tax,
                    customerNote = this.customer_note ?: "",
                    discountTotal = this.discount_total ?: "",
                    discountCodes = this.coupon_lines?.let { couponLines ->
                        // Extract the discount codes from the coupon lines list and
                        // store them as a comma-delimited 'String'.
                        couponLines
                                .filter { !it.code.isNullOrEmpty() }
                                .joinToString { it.code!! }
                    }.orEmpty(),
                    refundTotal = this.refunds?.let { refunds ->
                        // Extract the individual refund totals from the refunds list and
                        // store their sum as a 'Double'.
                        refunds.sumOf { it.total?.toBigDecimalOrNull() ?: BigDecimal.ZERO }
                    } ?: BigDecimal.ZERO,
                    customerId = this.customer_id ?: 0,
                    billingFirstName = this.billing?.first_name ?: "",
                    billingLastName = this.billing?.last_name ?: "",
                    billingCompany = this.billing?.company ?: "",
                    billingAddress1 = this.billing?.address_1 ?: "",
                    billingAddress2 = this.billing?.address_2 ?: "",
                    billingCity = this.billing?.city ?: "",
                    billingState = this.billing?.state ?: "",
                    billingPostcode = this.billing?.postcode ?: "",
                    billingCountry = this.billing?.country ?: "",
                    billingEmail = this.billing?.email ?: "",
                    billingPhone = this.billing?.phone ?: "",
                    shippingFirstName = this.shipping?.first_name ?: "",
                    shippingLastName = this.shipping?.last_name ?: "",
                    shippingCompany = this.shipping?.company ?: "",
                    shippingAddress1 = this.shipping?.address_1 ?: "",
                    shippingAddress2 = this.shipping?.address_2 ?: "",
                    shippingCity = this.shipping?.city ?: "",
                    shippingState = this.shipping?.state ?: "",
                    shippingPostcode = this.shipping?.postcode ?: "",
                    shippingCountry = this.shipping?.country ?: "",
                    shippingPhone = this.shipping?.phone ?: "",
                    lineItems = this.line_items.toString(),
                    shippingLines = this.shipping_lines.toString(),
                    feeLines = this.fee_lines.toString(),
                    taxLines = this.tax_lines.toString(),
                    couponLines = Gson().toJson(this.coupon_lines),
                    metaData = (this.meta_data as? JsonArray)?.mapNotNull { element ->
                        (element as? JsonObject)?.let { WCMetaData.fromJson(it) }
                    } ?: emptyList(),
                    paymentUrl = this.payment_url ?: "",
                    isEditable = this.is_editable ?: (this.status in EDITABLE_STATUSES),
                    needsPayment = this.needs_payment,
                    needsProcessing = this.needs_processing,
                    shippingTax = this.shipping_tax ?: ""
            )
        }

        val strippedMetaData = stripOrderMetaData.invoke(rawRemoteDataEntity.metaData)

        return stripOrder(rawRemoteDataEntity) to strippedMetaData
    }

    companion object {
        val EDITABLE_STATUSES = listOf("pending", "on-hold", "auto-draft")
        fun OrderAddress.Billing.toDto() = OrderDto.Billing(
                first_name = this.firstName,
                last_name = this.lastName,
                company = this.company,
                address_1 = this.address1,
                address_2 = this.address2,
                city = this.city,
                state = this.state,
                postcode = this.postcode,
                country = this.country,
                // the backend will fail to create the order if the billing email is an empty string,
                // so we use null to avoid this situation
                email = this.email.ifEmpty { null },
                phone = this.phone
        )

        fun OrderAddress.Shipping.toDto() = OrderDto.Shipping(
                first_name = this.firstName,
                last_name = this.lastName,
                company = this.company,
                address_1 = this.address1,
                address_2 = this.address2,
                city = this.city,
                state = this.state,
                postcode = this.postcode,
                country = this.country,
                phone = this.phone
        )
    }
}
