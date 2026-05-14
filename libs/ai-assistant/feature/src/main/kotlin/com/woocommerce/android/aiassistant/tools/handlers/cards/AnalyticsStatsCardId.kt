package com.woocommerce.android.aiassistant.tools.handlers.cards

import com.woocommerce.android.aiassistant.tools.analytics.AnalyticsInterval
import com.woocommerce.android.aiassistant.tools.analytics.parseAnalyticsDate
import com.woocommerce.android.aiassistant.tools.analytics.validateAnalyticsDateRange

internal data class AnalyticsStatsCardId(
    val after: String,
    val before: String,
    val interval: AnalyticsInterval,
) {
    companion object {
        fun parse(id: String): AnalyticsStatsCardId? {
            val parts = id.takeIf { it.length <= MAX_ANALYTICS_STATS_ID_LENGTH }
                ?.split(":")
                ?.takeIf { it.hasExpectedAnalyticsStatsIdLabels() }
            val interval = parts?.get(INTERVAL_VALUE_INDEX)?.let(AnalyticsInterval::fromValue)

            return parts?.toAnalyticsStatsCardId(interval)?.takeIf { it.hasValidDateRange() }
        }
    }
}

private fun List<String>.toAnalyticsStatsCardId(
    interval: AnalyticsInterval?,
): AnalyticsStatsCardId? {
    return when {
        interval == null -> null
        else -> AnalyticsStatsCardId(
            after = get(AFTER_VALUE_INDEX),
            before = get(BEFORE_VALUE_INDEX),
            interval = interval,
        )
    }
}

private fun List<String>.hasExpectedAnalyticsStatsIdLabels(): Boolean =
    size == ANALYTICS_STATS_ID_PART_COUNT &&
        getOrNull(TOOL_INDEX) == ANALYTICS_ORDERS_ID_PREFIX &&
        getOrNull(AFTER_LABEL_INDEX) == AFTER_LABEL &&
        getOrNull(BEFORE_LABEL_INDEX) == BEFORE_LABEL &&
        getOrNull(INTERVAL_LABEL_INDEX) == INTERVAL_LABEL

private fun AnalyticsStatsCardId.hasValidDateRange(): Boolean {
    val afterDate = parseAnalyticsDate(after) ?: return false
    val beforeDate = parseAnalyticsDate(before) ?: return false
    return validateAnalyticsDateRange(afterDate, beforeDate, interval) == null
}

internal fun AnalyticsStatsCardId.toSyntheticId(): String =
    "$ANALYTICS_ORDERS_ID_PREFIX:$AFTER_LABEL:$after:$BEFORE_LABEL:$before:$INTERVAL_LABEL:${interval.value}"

private const val ANALYTICS_ORDERS_ID_PREFIX = "analytics_orders"
private const val AFTER_LABEL = "after"
private const val BEFORE_LABEL = "before"
private const val INTERVAL_LABEL = "interval"
private const val MAX_ANALYTICS_STATS_ID_LENGTH = 160
private const val ANALYTICS_STATS_ID_PART_COUNT = 7
private const val TOOL_INDEX = 0
private const val AFTER_LABEL_INDEX = 1
private const val AFTER_VALUE_INDEX = 2
private const val BEFORE_LABEL_INDEX = 3
private const val BEFORE_VALUE_INDEX = 4
private const val INTERVAL_LABEL_INDEX = 5
private const val INTERVAL_VALUE_INDEX = 6
