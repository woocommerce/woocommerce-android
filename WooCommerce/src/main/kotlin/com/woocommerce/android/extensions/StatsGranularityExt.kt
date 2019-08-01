package com.woocommerce.android.extensions

import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.utils.DateUtils

/**
 * TEMP CODE: Method returns the start date for visitor stats based on the [StatsGranularity]
 * This is just a temp method till we remove support for the old version.
 * Then the date ranges for visitor stats can be modified in FluxC and this method can be removed
 */
fun StatsGranularity.getVisitorStatsStartDate(): String {
    return when (this) {
        StatsGranularity.DAYS -> DateUtils.getStartOfCurrentDay()
        StatsGranularity.WEEKS -> DateUtils.getFirstDayOfCurrentWeek()
        StatsGranularity.MONTHS -> DateUtils.getFirstDayOfCurrentMonth()
        StatsGranularity.YEARS -> DateUtils.getFirstDayOfCurrentYear()
    }
}
