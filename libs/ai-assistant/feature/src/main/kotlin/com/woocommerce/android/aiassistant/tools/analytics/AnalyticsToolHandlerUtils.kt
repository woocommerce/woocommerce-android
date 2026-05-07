package com.woocommerce.android.aiassistant.tools.analytics

import com.woocommerce.android.aiassistant.core.chat.ToolResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

internal fun validateAnalyticsDate(value: String): Boolean = try {
    LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
    true
} catch (_: DateTimeParseException) {
    false
}

internal fun analyticsDateAfterBound(value: String) = "${value}T00:00:00"

internal fun analyticsDateBeforeBound(value: String) = "${value}T23:59:59"

internal fun normaliseCurrency(value: String?) = value?.trim()?.takeIf { it.isNotEmpty() }

internal fun validateAnalyticsDateRange(
    after: String,
    before: String,
    interval: AnalyticsInterval,
): String? {
    val afterDate = LocalDate.parse(after, DateTimeFormatter.ISO_LOCAL_DATE)
    val beforeDate = LocalDate.parse(before, DateTimeFormatter.ISO_LOCAL_DATE)
    if (afterDate.isAfter(beforeDate)) {
        return "after must be on or before before"
    }

    val bucketCount = analyticsBucketCount(afterDate, beforeDate, interval)
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
    currency: String? = null,
    previousPeriodTotals: JsonObject? = null,
): JsonObject = buildJsonObject {
    put("after", after)
    put("before", before)
    put("interval", interval.value)
    currency?.let { put("currency", it) }
    stats.totals?.let { put("totals", it) }
    previousPeriodTotals?.let { put("previous_period_totals", it) }
    stats.intervals?.let { intervals ->
        put("interval_count", intervals.size)
        putJsonArray("interval_subtotals") {
            intervals.mapNotNull(::intervalSubtotal).forEach(::add)
        }
    }
}

internal fun previousPeriodFor(after: String, before: String): Pair<String, String> {
    val afterDate = LocalDate.parse(after, DateTimeFormatter.ISO_LOCAL_DATE)
    val beforeDate = LocalDate.parse(before, DateTimeFormatter.ISO_LOCAL_DATE)
    val inclusiveDays = ChronoUnit.DAYS.between(afterDate, beforeDate) + 1
    val previousBefore = afterDate.minusDays(1)
    val previousAfter = previousBefore.minusDays(inclusiveDays - 1)
    return previousAfter.format(DateTimeFormatter.ISO_LOCAL_DATE) to
        previousBefore.format(DateTimeFormatter.ISO_LOCAL_DATE)
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
