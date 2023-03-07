package com.woocommerce.android.model

import com.woocommerce.android.network.subscription.SubscriptionRestClient
import org.wordpress.android.fluxc.utils.DateUtils
import org.wordpress.android.util.DateTimeUtils
import java.math.BigDecimal
import java.util.Date
import javax.inject.Inject

class SubscriptionMapper @Inject constructor() {
    fun toAppModel(dto: SubscriptionRestClient.SubscriptionDto): Subscription {
        return Subscription(
            id = dto.id ?: 0L,
            status = Subscription.Status.fromValue(dto.status ?: ""),
            billingPeriod = Subscription.Period.fromValue(dto.billing_period ?: ""),
            billingInterval = dto.billing_interval?.toIntOrNull() ?: 0,
            total = dto.total?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            startDate = formatGmtAsUtcDate(dto.start_date_gmt) ?: Date() ,
            endDate = formatGmtAsUtcDate(dto.end_date_gmt),
            currency = dto.currency ?: ""
        )
    }

    private fun formatGmtAsUtcDate(date: String?): Date? {
        return date?.let {
            val formattedDate = DateUtils.formatGmtAsUtcDateString(it)
            DateTimeUtils.dateUTCFromIso8601(formattedDate)
        }
    }
}
