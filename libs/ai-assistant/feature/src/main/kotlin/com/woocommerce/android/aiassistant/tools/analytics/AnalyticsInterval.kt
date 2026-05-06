package com.woocommerce.android.aiassistant.tools.analytics

import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

internal enum class AnalyticsInterval(
    val value: String,
    val statsGranularity: StatsGranularity,
) {
    HOUR("hour", StatsGranularity.HOURS),
    DAY("day", StatsGranularity.DAYS),
    WEEK("week", StatsGranularity.WEEKS),
    MONTH("month", StatsGranularity.MONTHS),
    YEAR("year", StatsGranularity.YEARS);

    companion object {
        val values = entries.map { it.value }

        fun fromValue(value: String): AnalyticsInterval? = entries.find { it.value == value }
    }
}
