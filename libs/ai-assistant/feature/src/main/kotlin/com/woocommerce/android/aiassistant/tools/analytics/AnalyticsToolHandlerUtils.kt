package com.woocommerce.android.aiassistant.tools.analytics

import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.tools.RestDateBounds
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

internal fun parseAnalyticsDate(value: String): LocalDate? = RestDateBounds.parseDate(value)

internal fun analyticsDateAfterBound(value: String) = requireNotNull(RestDateBounds.lowerBound(value))

internal fun analyticsDateBeforeBound(value: String) = requireNotNull(RestDateBounds.upperBound(value))

internal fun validateAnalyticsDateRange(
    after: LocalDate,
    before: LocalDate,
    interval: AnalyticsInterval,
): String? {
    if (after.isAfter(before)) {
        return "after must be on or before before"
    }

    val bucketCount = analyticsBucketCount(after, before, interval)
    return if (bucketCount > MAX_ANALYTICS_INTERVALS) {
        "Requested range contains $bucketCount ${interval.value} buckets; use a coarser interval or shorter range."
    } else {
        null
    }
}

internal fun analyticsValidationError(toolCallId: String, reason: String) =
    ToolResult.ValidationError(toolCallId = toolCallId, reason = reason)

internal fun analyticsStatsSummary(
    after: String,
    before: String,
    interval: AnalyticsInterval,
    stats: AnalyticsStats,
    cardId: String,
    currency: String? = null,
    previousPeriodTotals: JsonObject? = null,
    previousPeriodPartial: Boolean = false,
    previousPeriodWarning: String? = null,
): JsonObject = buildJsonObject {
    put("after", after)
    put("before", before)
    put("interval", interval.value)
    put("card_id", cardId)
    currency?.let { put("currency", it) }
    stats.totals?.let { put("totals", it) }
    previousPeriodTotals?.let { put("previous_period_totals", it) }
    if (previousPeriodPartial) {
        put("previous_period_partial", true)
    }
    previousPeriodWarning?.let { put("previous_period_warning", it) }
    stats.intervals?.let { intervals ->
        put("interval_count", intervals.size)
        putJsonArray("interval_subtotals") {
            intervals.mapNotNull(::intervalSubtotal).forEach(::add)
        }
    }
}

internal fun previousPeriodFor(after: LocalDate, before: LocalDate): Pair<String, String> {
    val inclusiveDays = ChronoUnit.DAYS.between(after, before) + 1
    val previousBefore = after.minusDays(1)
    val previousAfter = previousBefore.minusDays(inclusiveDays - 1)
    return previousAfter.toString() to previousBefore.toString()
}

private fun intervalSubtotal(interval: JsonObject): JsonObject? {
    val subtotal = buildJsonObject {
        interval["interval"]?.let { put("interval", it) }
        interval["date_start"]?.let { put("date_start", it) }
        interval["subtotals"]?.let { put("subtotals", it) }
    }
    return subtotal.takeIf { it.isNotEmpty() }
}

private fun analyticsBucketCount(
    after: LocalDate,
    before: LocalDate,
    interval: AnalyticsInterval,
): Long {
    val inclusiveDays = ChronoUnit.DAYS.between(after, before) + 1
    return when (interval) {
        AnalyticsInterval.HOUR -> inclusiveDays * HOURS_PER_DAY
        AnalyticsInterval.DAY -> inclusiveDays
        AnalyticsInterval.WEEK -> {
            val afterWeek = after.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val beforeWeek = before.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            ChronoUnit.WEEKS.between(afterWeek, beforeWeek) + 1
        }
        AnalyticsInterval.MONTH -> {
            val afterMonth = YearMonth.from(after)
            val beforeMonth = YearMonth.from(before)
            ChronoUnit.MONTHS.between(afterMonth, beforeMonth) + 1
        }
        AnalyticsInterval.YEAR -> before.year - after.year + 1L
    }
}

private const val MAX_ANALYTICS_INTERVALS = 100
private const val HOURS_PER_DAY = 24
