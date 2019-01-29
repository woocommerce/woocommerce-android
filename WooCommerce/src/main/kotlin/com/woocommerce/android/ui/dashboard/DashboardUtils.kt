package com.woocommerce.android.ui.dashboard

import com.woocommerce.android.util.CurrencyUtils
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

object DashboardUtils {
    val DEFAULT_STATS_GRANULARITY = StatsGranularity.DAYS

    // TODO For certain currencies/locales, replace the thousands mark with k
    fun formatAmountForDisplay(
        amount: Double,
        allowZero: Boolean = true
    ): String {
        return amount.takeIf { allowZero || it > 0 }?.let {
            CurrencyUtils.currencyStringRounded(amount)
        } ?: ""
    }
}
