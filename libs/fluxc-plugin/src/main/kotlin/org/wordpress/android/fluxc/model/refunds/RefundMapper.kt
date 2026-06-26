package org.wordpress.android.fluxc.model.refunds

import com.google.gson.Gson
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.refunds.WCRefundModel.WCRefundItem
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundPreviewRestClient.RefundPreviewResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundRestClient.RefundResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundRestClient.SimplifiedRefundResponse
import org.wordpress.android.fluxc.persistence.entity.RefundEntity
import org.wordpress.android.fluxc.utils.DateUtils
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.util.Date
import javax.inject.Inject

class RefundMapper @Inject constructor(private val gson: Gson) {

    fun toModel(entity: RefundEntity): WCRefundModel {
        val refundResponse = gson.fromJson(entity.data, RefundResponse::class.java)
        return toModel(refundResponse)
    }

    fun toEntity(
        siteId: LocalId,
        orderId: RemoteId,
        refundResponse: RefundResponse
    ): RefundEntity {
        val json = gson.toJson(refundResponse)
        val entity = RefundEntity(
            siteId = siteId,
            orderId = orderId,
            refundId = RemoteId(refundResponse.refundId),
            data = json
        )
        return entity
    }

    fun toModel(response: RefundResponse): WCRefundModel {
        return WCRefundModel(
                id = response.refundId,
                dateCreated = response.dateCreated?.let { fromFormattedDate(it) } ?: Date(),
                amount = response.amount?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                reason = response.reason,
                automaticGatewayRefund = response.refundedPayment ?: false,
                items = response.items?.map {
                    WCRefundItem(
                            itemId = it.id ?: -1,
                            quantity = it.quantity?.toInt() ?: 0,
                            subtotal = it.subtotal?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            totalTax = it.totalTax?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            name = it.name,
                            productId = it.productId,
                            variationId = it.variationId,
                            total = it.total?.toBigDecimalOrNull(),
                            sku = it.sku,
                            price = it.price?.toBigDecimalOrNull(),
                            metaData = it.metaData,
                            taxes = it.taxes
                    )
                } ?: emptyList(),
                shippingLineItems = response.shippingLineItems ?: emptyList(),
                feeLineItems = response.feeLineItems ?: emptyList()
        )
    }

    fun toModel(response: SimplifiedRefundResponse): WCRefundModel {
        return WCRefundModel(
            id = response.refundId,
            dateCreated = response.dateCreated?.let { fromFormattedDate(it) } ?: Date(),
            amount = response.amount?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            reason = response.reason,
            automaticGatewayRefund = response.refundedPayment ?: false,
            items = response.lineItems?.map { it.toRefundItem() } ?: emptyList(),
            // v4 returns products, fees and shipping combined in line_items with no type marker, so
            // they cannot be split into the dedicated shipping/fee lists; all are mapped into items.
            shippingLineItems = emptyList(),
            feeLineItems = emptyList()
        )
    }

    private fun SimplifiedRefundResponse.SimplifiedLineItem.toRefundItem(): WCRefundItem {
        val subtotal = refundTotal?.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val totalTax = refundTax
            ?.sumOf { it.refundTotal?.toBigDecimalOrNull() ?: BigDecimal.ZERO }
            ?: BigDecimal.ZERO
        // v4 returns positive magnitudes, but WCRefundItem values are stored negative by contract
        // (the v3 API returns them negative) and negated back to positive in Refund.toAppModel().
        // Negate here so v4-created refunds honour the same contract as v3.
        return WCRefundItem(
            itemId = lineItemId ?: -1,
            quantity = -(quantity ?: 0),
            subtotal = -subtotal,
            totalTax = -totalTax,
            total = -(subtotal + totalTax)
        )
    }

    fun toPreviewModel(response: RefundPreviewResponse): WCRefundPreview {
        return WCRefundPreview(
            subtotal = response.subtotal.toBigDecimalOrZero(),
            tax = response.tax.toBigDecimalOrZero(),
            total = response.total.toBigDecimalOrZero(),
            maxRefundable = response.maxRefundable.toBigDecimalOrZero(),
            breakdown = WCRefundPreview.Breakdown(
                products = response.breakdown?.products.toSection(),
                shipping = response.breakdown?.shipping.toSection(),
                fees = response.breakdown?.fees.toSection(),
            ),
        )
    }

    private fun RefundPreviewResponse.Section?.toSection(): WCRefundPreview.Section {
        return WCRefundPreview.Section(
            items = this?.items?.map { it.toItem() } ?: emptyList(),
            subtotal = this?.subtotal.toBigDecimalOrZero(),
            tax = this?.tax.toBigDecimalOrZero(),
            total = this?.total.toBigDecimalOrZero(),
        )
    }

    private fun RefundPreviewResponse.Item.toItem(): WCRefundPreview.Item {
        return WCRefundPreview.Item(
            id = id,
            name = name.orEmpty(),
            quantity = quantity,
            subtotal = subtotal.toBigDecimalOrZero(),
            tax = tax.toBigDecimalOrZero(),
            total = total.toBigDecimalOrZero(),
            productId = productId,
        )
    }

    private fun String?.toBigDecimalOrZero(): BigDecimal = this?.toBigDecimalOrNull() ?: BigDecimal.ZERO

    private fun fromFormattedDate(date: String): Date? {
        if (date.isEmpty()) return null
        return try {
            val localDateTime = LocalDateTime.parse(date, DateUtils.DATE_TIME_FORMATTER)
            Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant())
        } catch (_: DateTimeParseException) {
            val localDate = LocalDate.parse(date, DateUtils.DATE_FORMATTER)
            Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        }
    }
}
