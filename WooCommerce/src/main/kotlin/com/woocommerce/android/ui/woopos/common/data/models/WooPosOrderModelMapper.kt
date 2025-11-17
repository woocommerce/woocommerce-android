package com.woocommerce.android.ui.woopos.common.data.models

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.orders.WooPosOrderModel
import dagger.Reusable
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderDto
import java.math.BigDecimal
import java.time.Instant
import java.util.Date
import javax.inject.Inject

@Reusable
class WooPosOrderModelMapper @Inject constructor(
    private val logger: WooPosLogWrapper
) {
    private val gson = Gson()

    fun fromDto(dto: OrderDto): WooPosOrderModel {
        val lineItems = parseLineItems(dto.line_items)

        val refundTotal = parseRefundTotal(dto.refunds)
        val discountTotal = dto.discount_total.toBigDecimalOrZero()
        val total = dto.total.toBigDecimalOrZero()
        val totalTax = dto.total_tax.toBigDecimalOrZero()
        val shippingTotal = dto.shipping_total.toBigDecimalOrZero()

        val productsTotal = lineItems.fold(BigDecimal.ZERO) { acc, item ->
            acc + item.total
        }

        val dateCreated = parseDateOrEpoch(dto.date_created_gmt)

        val status = Order.Status.fromValue(dto.status ?: Order.Status.Pending.value)

        return WooPosOrderModel(
            id = dto.id ?: 0L,
            number = dto.number ?: dto.id?.toString().orEmpty(),
            dateCreated = dateCreated,
            status = status,

            total = total,
            productsTotal = productsTotal,
            discountTotal = discountTotal,
            totalTax = totalTax,
            shippingTotal = shippingTotal,
            refundTotal = refundTotal,

            paymentMethodTitle = dto.payment_method_title.orEmpty(),
            customerEmail = dto.billing?.email,
            billingEmail = dto.billing?.email,

            items = lineItems,
            discountCode = dto.coupon_lines?.firstOrNull()?.code
        )
    }

    fun fromDtos(dtos: List<OrderDto>): List<WooPosOrderModel> =
        dtos.map(::fromDto)

    // -------------------------------
    // Helpers
    // -------------------------------

    private fun String?.toBigDecimalOrZero(): BigDecimal =
        this?.toBigDecimalOrNull() ?: BigDecimal.ZERO

    private fun parseDateOrEpoch(raw: String?): Date {
        if (raw.isNullOrBlank()) return Date(0)
        return try { Date.from(Instant.parse(raw)) }
        catch (e: Exception) {
            logger.w("Failed to parse date '$raw' - ${e.message}")
            Date(0)
        }
    }

    private fun parseRefundTotal(refunds: List<OrderDto.Refund>?): BigDecimal {
        if (refunds.isNullOrEmpty()) return BigDecimal.ZERO

        val sum = refunds.fold(BigDecimal.ZERO) { acc, refund ->
            acc + refund.total.toBigDecimalOrZero()
        }

        return sum.negate()  // make positive like in OrderMapper
    }

    private fun parseLineItems(json: JsonElement?): List<WooPosOrderModel.LineItem> {
        if (json == null || json.isJsonNull) return emptyList()

        return try {
            val type = object : TypeToken<List<Map<String, Any?>>>() {}.type
            val maps: List<Map<String, Any?>> = gson.fromJson(json, type)
            maps.mapNotNull(::parseLineItem)
        } catch (e: JsonSyntaxException) {
            logger.w("Failed to parse line_items JSON: $json - ${e.message}")
            emptyList()
        }
    }

    private fun parseLineItem(map: Map<String, Any?>): WooPosOrderModel.LineItem? {
        val itemId = (map["id"] as? Number)?.toLong()
            ?: (map["id"] as? String)?.toLongOrNull()
        val productId = (map["product_id"] as? Number)?.toLong()
            ?: (map["product_id"] as? String)?.toLongOrNull()

        if (itemId == null || productId == null) return null

        return WooPosOrderModel.LineItem(
            itemId = itemId,
            productId = productId,
            name = map["name"] as? String ?: "",
            quantity = when (val q = map["quantity"]) {
                is Number -> q.toFloat()
                is String -> q.toFloatOrNull() ?: 0f
                else -> 0f
            },
            total = (map["total"] as? String).toBigDecimalOrZero()
        )
    }
}
