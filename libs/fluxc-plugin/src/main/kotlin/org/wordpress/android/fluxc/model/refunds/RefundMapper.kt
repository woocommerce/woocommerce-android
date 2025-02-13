package org.wordpress.android.fluxc.model.refunds

import org.wordpress.android.fluxc.model.refunds.WCRefundModel.WCRefundItem
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.DATE_FORMAT_DAY
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundRestClient.RefundResponse
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class RefundMapper @Inject constructor() {
    fun map(response: RefundResponse): WCRefundModel {
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
                            metaData = it.metaData
                    )
                } ?: emptyList(),
                shippingLineItems = response.shippingLineItems ?: emptyList(),
                feeLineItems = response.feeLineItems ?: emptyList()
        )
    }

    private fun fromFormattedDate(date: String): Date? {
        if (date.isEmpty()) {
            return null
        }
        val dateFormat = SimpleDateFormat(DATE_FORMAT_DAY, Locale.ROOT)
        return dateFormat.parse(date)
    }
}
