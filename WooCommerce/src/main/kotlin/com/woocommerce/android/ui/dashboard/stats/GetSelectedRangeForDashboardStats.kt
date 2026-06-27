package com.woocommerce.android.ui.dashboard.stats

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ui.analytics.ranges.SiteWeekStartCalendarProvider
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.dashboard.data.StatsCustomDateRangeDataStore
import com.woocommerce.android.ui.dashboard.domain.GetSelectedDateRange
import com.woocommerce.android.util.DateUtils
import javax.inject.Inject

class GetSelectedRangeForDashboardStats @Inject constructor(
    private val appPrefs: AppPrefsWrapper,
    customDateRangeDataStore: StatsCustomDateRangeDataStore,
    dateUtils: DateUtils,
    siteWeekStartCalendarProvider: SiteWeekStartCalendarProvider
) : GetSelectedDateRange(appPrefs, customDateRangeDataStore, dateUtils, siteWeekStartCalendarProvider) {
    override fun getSelectedRange(): SelectionType =
        runCatching {
            SelectionType.valueOf(appPrefs.getActiveStoreStatsTab())
        }.getOrDefault(SelectionType.TODAY)
}
