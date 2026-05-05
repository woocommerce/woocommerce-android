package com.woocommerce.android.aiassistant.tools.analytics

import com.woocommerce.android.aiassistant.core.chat.ToolResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
    stats: AnalyticsStats,
    currency: String? = null,
    includeStatsCharts: Boolean = false,
): JsonObject = buildJsonObject {
    put("after", after)
    put("before", before)
    currency?.let { put("currency", it) }
    stats.totals?.let { put("totals", it) }
    stats.intervals?.let { intervals ->
        put("interval_count", intervals.size)
        putJsonArray("interval_subtotals") {
            intervals.mapNotNull(::intervalSubtotal).forEach(::add)
        }
        if (includeStatsCharts) {
            putJsonArray("revenue_chart") {
                intervals.mapNotNull(::revenueChartPoint).forEach(::add)
            }
            putJsonArray("order_chart") {
                intervals.mapNotNull(::orderChartPoint).forEach(::add)
            }
        }
    }
    if (stats.intervals == null && includeStatsCharts) {
        putJsonArray("revenue_chart") {
        }
        putJsonArray("order_chart") {
        }
    }
}

private fun intervalSubtotal(interval: JsonObject): JsonObject? {
    val subtotal = buildJsonObject {
        interval["interval"]?.let { put("interval", it) }
        interval["date_start"]?.let { put("date_start", it) }
        interval["subtotals"]?.let { put("subtotals", it) }
    }
    return subtotal.takeIf { it.isNotEmpty() }
}

private fun revenueChartPoint(interval: JsonObject): JsonObject? {
    val date = interval.chartDate() ?: return null
    val value = interval.chartRevenueValue() ?: return null
    return buildJsonObject {
        put("date", date)
        put("value", value)
    }
}

private fun orderChartPoint(interval: JsonObject): JsonObject? {
    val date = interval.chartDate() ?: return null
    val value = interval.chartOrderCountValue() ?: return null
    return buildJsonObject {
        put("date", date)
        put("value", value)
    }
}

private fun JsonObject.chartDate(): String? {
    val intervalDate = stringValue("interval")
        ?.takeIf { it.isIsoLocalDate() }
    if (intervalDate != null) return intervalDate

    return stringValue("date_start")
        ?.take(ISO_LOCAL_DATE_LENGTH)
        ?.takeIf { it.isIsoLocalDate() }
}

private fun JsonObject.chartRevenueValue(): Double? {
    val subtotals = this["subtotals"]?.jsonObject ?: return null
    return REVENUE_CHART_VALUE_KEYS.firstNotNullOfOrNull { key ->
        subtotals.stringValue(key)?.toDoubleOrNull() ?: subtotals[key]?.jsonPrimitive?.doubleOrNull
    }
}

private fun JsonObject.chartOrderCountValue(): Double? {
    val subtotals = this["subtotals"]?.jsonObject ?: return null
    return ORDER_CHART_VALUE_KEYS.firstNotNullOfOrNull { key ->
        subtotals.stringValue(key)?.toDoubleOrNull() ?: subtotals[key]?.jsonPrimitive?.doubleOrNull
    }
}

private fun JsonObject.stringValue(name: String): String? =
    this[name]?.jsonPrimitive?.content

private fun String.isIsoLocalDate(): Boolean =
    ISO_LOCAL_DATE_SHAPE.matches(this) &&
        runCatching { LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE) }.isSuccess

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
private const val ISO_LOCAL_DATE_LENGTH = 10
private val ISO_LOCAL_DATE_SHAPE = Regex("\\d{4}-\\d{2}-\\d{2}")
private val REVENUE_CHART_VALUE_KEYS = listOf("net_revenue", "total_sales", "total_revenue", "gross_revenue")
private val ORDER_CHART_VALUE_KEYS = listOf("orders_count", "order_count", "orders")
