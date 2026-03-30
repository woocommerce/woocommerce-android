package com.woocommerce.android.ui.dashboard.topcategories

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.dashboard.data.TopCategoriesCustomDateRangeDataStore
import com.woocommerce.android.ui.dashboard.domain.GetSelectedDateRange
import com.woocommerce.android.util.DateUtils
import javax.inject.Inject

class GetSelectedRangeForTopCategories @Inject constructor(
    private val appPrefs: AppPrefsWrapper,
    customDateRangeDataStore: TopCategoriesCustomDateRangeDataStore,
    dateUtils: DateUtils
) : GetSelectedDateRange(appPrefs, customDateRangeDataStore, dateUtils) {
    override fun getSelectedRange(): SelectionType =
        runCatching {
            SelectionType.valueOf(appPrefs.getActiveTopCategoriesTab())
        }.getOrDefault(SelectionType.TODAY)
}
