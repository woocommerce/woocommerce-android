package com.woocommerce.android.aiassistant.tools.handlers.cards

import com.woocommerce.android.aiassistant.tools.analytics.AnalyticsInterval
import com.woocommerce.android.aiassistant.tools.analytics.normaliseCurrency
import com.woocommerce.android.aiassistant.tools.analytics.parseAnalyticsDate
import com.woocommerce.android.aiassistant.tools.analytics.validateAnalyticsDateRange

internal data class AnalyticsStatsCardId(
    val kind: AnalyticsStatsKind,
    val after: String,
    val before: String,
    val interval: AnalyticsInterval,
    val currency: String?,
) {
    companion object {
        fun parse(id: String): AnalyticsStatsCardId? {
            val parts = id.takeIf { it.length <= MAX_ANALYTICS_STATS_ID_LENGTH }
                ?.split(":")
                ?.takeIf { it.hasExpectedAnalyticsStatsIdLabels() }
            val kind = parts?.get(TOOL_INDEX)?.let(AnalyticsStatsKind::fromPrefix)
            val interval = parts?.get(INTERVAL_VALUE_INDEX)?.let(AnalyticsInterval::fromValue)
            val parsedCurrency = parts?.parsedCurrency(kind)

            return parts?.toAnalyticsStatsCardId(kind, interval, parsedCurrency)?.takeIf { it.hasValidDateRange() }
        }
    }
}

internal enum class AnalyticsStatsKind(
    val idPrefix: String,
    val serializedName: String,
) {
    Revenue("analytics_revenue", "revenue"),
    Orders("analytics_orders", "orders");

    companion object {
        fun fromPrefix(value: String): AnalyticsStatsKind? =
            entries.firstOrNull { it.idPrefix == value }
    }
}

private data class ParsedCurrency(val value: String?)

private fun List<String>.toAnalyticsStatsCardId(
    kind: AnalyticsStatsKind?,
    interval: AnalyticsInterval?,
    parsedCurrency: ParsedCurrency?,
): AnalyticsStatsCardId? {
    return when {
        kind == null -> null
        interval == null -> null
        parsedCurrency == null -> null
        else -> AnalyticsStatsCardId(
            kind = kind,
            after = get(AFTER_VALUE_INDEX),
            before = get(BEFORE_VALUE_INDEX),
            interval = interval,
            currency = parsedCurrency.value,
        )
    }
}

private fun List<String>.hasExpectedAnalyticsStatsIdLabels(): Boolean {
    val kind = getOrNull(TOOL_INDEX)?.let(AnalyticsStatsKind::fromPrefix) ?: return false
    val hasBaseLabels = getOrNull(AFTER_LABEL_INDEX) == AFTER_LABEL &&
        getOrNull(BEFORE_LABEL_INDEX) == BEFORE_LABEL &&
        getOrNull(INTERVAL_LABEL_INDEX) == INTERVAL_LABEL

    return hasBaseLabels && when (size) {
        ANALYTICS_ORDERS_STATS_ID_PART_COUNT -> kind == AnalyticsStatsKind.Orders
        ANALYTICS_STATS_ID_PART_COUNT_WITH_CURRENCY -> get(CURRENCY_LABEL_INDEX) == CURRENCY_LABEL
        else -> false
    }
}

private fun List<String>.parsedCurrency(kind: AnalyticsStatsKind?): ParsedCurrency? =
    when (size) {
        ANALYTICS_ORDERS_STATS_ID_PART_COUNT -> ParsedCurrency(null)
        ANALYTICS_STATS_ID_PART_COUNT_WITH_CURRENCY -> get(CURRENCY_VALUE_INDEX)
            .toParsedCurrency()
            ?.takeIf { kind?.acceptsCurrency(get(CURRENCY_VALUE_INDEX)) == true }
        else -> null
    }

private fun String.toParsedCurrency(): ParsedCurrency? =
    when (this) {
        NO_CURRENCY_VALUE -> ParsedCurrency(null)
        else -> normaliseCurrency(this)
            ?.takeIf(VALID_CURRENCY_CODE::matches)
            ?.let(::ParsedCurrency)
    }

private fun AnalyticsStatsCardId.hasValidDateRange(): Boolean {
    val afterDate = parseAnalyticsDate(after) ?: return false
    val beforeDate = parseAnalyticsDate(before) ?: return false
    return validateAnalyticsDateRange(afterDate, beforeDate, interval) == null
}

private fun AnalyticsStatsKind.acceptsCurrency(currency: String): Boolean =
    this != AnalyticsStatsKind.Orders || currency == NO_CURRENCY_VALUE

internal fun AnalyticsStatsCardId.toSyntheticId(): String {
    if (kind == AnalyticsStatsKind.Orders) {
        return "${kind.idPrefix}:$AFTER_LABEL:$after:$BEFORE_LABEL:$before:$INTERVAL_LABEL:${interval.value}"
    }
    val currencyValue = currency ?: NO_CURRENCY_VALUE
    return "${kind.idPrefix}:$AFTER_LABEL:$after:$BEFORE_LABEL:$before:" +
        "$INTERVAL_LABEL:${interval.value}:$CURRENCY_LABEL:$currencyValue"
}

private const val AFTER_LABEL = "after"
private const val BEFORE_LABEL = "before"
private const val INTERVAL_LABEL = "interval"
private const val CURRENCY_LABEL = "currency"
private const val NO_CURRENCY_VALUE = "none"
private const val MAX_ANALYTICS_STATS_ID_LENGTH = 160
private const val ANALYTICS_ORDERS_STATS_ID_PART_COUNT = 7
private const val ANALYTICS_STATS_ID_PART_COUNT_WITH_CURRENCY = 9
private const val TOOL_INDEX = 0
private const val AFTER_LABEL_INDEX = 1
private const val AFTER_VALUE_INDEX = 2
private const val BEFORE_LABEL_INDEX = 3
private const val BEFORE_VALUE_INDEX = 4
private const val INTERVAL_LABEL_INDEX = 5
private const val INTERVAL_VALUE_INDEX = 6
private const val CURRENCY_LABEL_INDEX = 7
private const val CURRENCY_VALUE_INDEX = 8
private val VALID_CURRENCY_CODE = Regex("[A-Z]{3}")
