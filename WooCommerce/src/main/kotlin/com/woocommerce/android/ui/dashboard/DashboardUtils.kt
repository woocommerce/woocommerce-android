package com.woocommerce.android.ui.dashboard

import android.content.Context
import android.support.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.util.CurrencyUtils
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

    // TODO For certain currencies/locales, replace the thousands mark with k
    fun formatAmountForDisplay(
        context: Context,
        amount: Double,
        currencyCode: String?,
        allowZero: Boolean = true
    ): String {
        return amount.takeIf { allowZero || it > 0 }?.let {
            CurrencyUtils.currencyStringRounded(context, amount, currencyCode.orEmpty())
        } ?: ""
    }
}
