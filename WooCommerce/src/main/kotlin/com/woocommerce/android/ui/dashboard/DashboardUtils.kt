package com.woocommerce.android.ui.dashboard

import android.support.annotation.StringRes
import com.woocommerce.android.R
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

object DashboardUtils {
    @StringRes
    fun getStringForGranularity(timeframe: StatsGranularity): Int {
        return when (timeframe) {
            StatsGranularity.DAYS -> R.string.dashboard_stats_granularity_days
            StatsGranularity.WEEKS -> R.string.dashboard_stats_granularity_weeks
            StatsGranularity.MONTHS -> R.string.dashboard_stats_granularity_months
            StatsGranularity.YEARS -> R.string.dashboard_stats_granularity_years
        }
    }
}
