package com.woocommerce.android.aiassistant.tools.handlers.cards

import com.woocommerce.android.aiassistant.tools.analytics.AnalyticsInterval
import com.woocommerce.android.aiassistant.tools.analytics.normaliseCurrency
import com.woocommerce.android.aiassistant.tools.analytics.validateAnalyticsDate
import com.woocommerce.android.aiassistant.tools.analytics.validateAnalyticsDateRange

internal data class AnalyticsStatsCardId(
    val after: String,
    val before: String,
    val interval: AnalyticsInterval,
    val currency: String?,
) {
    companion object {
        fun parse(id: String): AnalyticsStatsCardId? {
            val parts = id.takeIf { it.length <= MAX_ANALYTICS_STATS_ID_LENGTH }
                ?.split(":")
                ?.takeIf { it.size == ANALYTICS_STATS_ID_PART_COUNT }
                ?.takeIf { it.hasExpectedAnalyticsStatsIdLabels() }
            val interval = parts?.get(INTERVAL_VALUE_INDEX)?.let(AnalyticsInterval::fromValue)
            val parsedCurrency = parts?.get(CURRENCY_VALUE_INDEX)?.toParsedCurrency()

            return if (parts != null && interval != null && parsedCurrency != null) {
                AnalyticsStatsCardId(
                    after = parts[AFTER_VALUE_INDEX],
                    before = parts[BEFORE_VALUE_INDEX],
                    interval = interval,
                    currency = parsedCurrency.value,
                ).takeIf { it.hasValidDateRange() }
            } else {
                null
            }
        }
    }
}

private data class ParsedCurrency(val value: String?)

private fun List<String>.hasExpectedAnalyticsStatsIdLabels(): Boolean =
    get(TOOL_INDEX) == ANALYTICS_STATS_TOOL &&
        get(AFTER_LABEL_INDEX) == AFTER_LABEL &&
        get(BEFORE_LABEL_INDEX) == BEFORE_LABEL &&
        get(INTERVAL_LABEL_INDEX) == INTERVAL_LABEL &&
        get(CURRENCY_LABEL_INDEX) == CURRENCY_LABEL

private fun String.toParsedCurrency(): ParsedCurrency? =
    when (this) {
        NO_CURRENCY_VALUE -> ParsedCurrency(null)
        else -> normaliseCurrency(this)
            ?.takeIf(VALID_CURRENCY_CODE::matches)
            ?.let(::ParsedCurrency)
    }

private fun AnalyticsStatsCardId.hasValidDateRange(): Boolean =
    validateAnalyticsDate(after) &&
        validateAnalyticsDate(before) &&
        validateAnalyticsDateRange(after, before, interval) == null

internal fun AnalyticsStatsCardId.toSyntheticId(): String {
    val currencyValue = currency ?: NO_CURRENCY_VALUE
    return "$ANALYTICS_STATS_TOOL:$AFTER_LABEL:$after:$BEFORE_LABEL:$before:" +
        "$INTERVAL_LABEL:${interval.value}:$CURRENCY_LABEL:$currencyValue"
}

private const val ANALYTICS_STATS_TOOL = "analytics_revenue"
private const val AFTER_LABEL = "after"
private const val BEFORE_LABEL = "before"
private const val INTERVAL_LABEL = "interval"
private const val CURRENCY_LABEL = "currency"
private const val NO_CURRENCY_VALUE = "none"
private const val MAX_ANALYTICS_STATS_ID_LENGTH = 160
private const val ANALYTICS_STATS_ID_PART_COUNT = 9
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
