package com.woocommerce.android.ui.dashboard.stats

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.mystore.data.StatsCustomDateRangeDataStore
import com.woocommerce.android.util.DateUtils
import javax.inject.Inject

class GetSelectedRangeForDashboardStats @Inject constructor(
    private val appPrefs: AppPrefsWrapper,
    customDateRangeDataStore: StatsCustomDateRangeDataStore,
    dateUtils: DateUtils
) : GetSelectedDateRange(appPrefs, customDateRangeDataStore, dateUtils) {
    override fun getSelectedRange(): SelectionType =
        runCatching {
            SelectionType.valueOf(appPrefs.getActiveStoreStatsTab())
        }.getOrDefault(SelectionType.TODAY)
}
