package org.wordpress.android.fluxc.model.refunds

import com.google.gson.Gson
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.refunds.WCRefundModel.WCRefundItem
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundRestClient.RefundResponse
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
