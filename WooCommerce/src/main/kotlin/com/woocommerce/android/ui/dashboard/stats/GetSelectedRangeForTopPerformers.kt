package com.woocommerce.android.ui.dashboard.stats

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.dashboard.data.TopPerformersCustomDateRangeDataStore
import com.woocommerce.android.util.DateUtils
import javax.inject.Inject

class GetSelectedRangeForTopPerformers @Inject constructor(
    private val appPrefs: AppPrefsWrapper,
    customDateRangeDataStore: TopPerformersCustomDateRangeDataStore,
    dateUtils: DateUtils
) : GetSelectedDateRange(appPrefs, customDateRangeDataStore, dateUtils) {
    override fun getSelectedRange(): SelectionType =
        runCatching {
            SelectionType.valueOf(appPrefs.getActiveTopPerformersTab())
        }.getOrDefault(SelectionType.TODAY)
}
