package com.woocommerce.android.model

import com.woocommerce.android.network.subscription.SubscriptionRestClient
import org.wordpress.android.fluxc.utils.DateUtils
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException
import javax.inject.Inject

class SubscriptionMapper @Inject constructor() {
    fun toAppModel(dto: SubscriptionRestClient.SubscriptionDto): Subscription {
        return Subscription(
            id = dto.id ?: 0L,
            status = Subscription.Status.fromValue(dto.status ?: ""),
            billingPeriod = SubscriptionPeriod.fromValue(dto.billing_period ?: ""),
            billingInterval = dto.billing_interval?.toIntOrNull() ?: 0,
            total = dto.total?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            startDate = formatGmtAsUtcLocalDate(dto.start_date_gmt) ?: LocalDate.now(),
            endDate = formatGmtAsUtcLocalDate(dto.end_date_gmt),
            currency = dto.currency ?: ""
        )
    }

    private fun formatGmtAsUtcLocalDate(date: String?): LocalDate? {
        if (date.isNullOrEmpty()) return null
        return try {
            val formattedDate = DateUtils.formatGmtAsUtcDateString(date)
            val instant = Instant.parse(formattedDate)
            val zoneId = ZoneId.of("UTC")
            val zdt = ZonedDateTime.ofInstant(instant, zoneId)
            LocalDate.from(zdt)
        } catch (e: DateTimeParseException) {
            null
        }
    }
}
