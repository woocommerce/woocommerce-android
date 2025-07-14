package com.woocommerce.android.ui.dashboard.coupons

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.dashboard.data.CouponsCustomDateRangeDataStore
import com.woocommerce.android.ui.dashboard.domain.GetSelectedDateRange
import com.woocommerce.android.util.DateUtils
import javax.inject.Inject

class GetSelectedRangeForCoupons @Inject constructor(
    private val appPrefs: AppPrefsWrapper,
    customDateRangeDataStore: CouponsCustomDateRangeDataStore,
    dateUtils: DateUtils
) : GetSelectedDateRange(appPrefs, customDateRangeDataStore, dateUtils) {
    override fun getSelectedRange(): SelectionType =
        runCatching {
            SelectionType.valueOf(appPrefs.getActiveCouponsTab())
        }.getOrDefault(SelectionType.TODAY)
}
