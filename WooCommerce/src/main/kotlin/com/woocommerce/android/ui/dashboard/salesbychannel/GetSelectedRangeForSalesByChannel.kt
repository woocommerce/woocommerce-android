package com.woocommerce.android.ui.dashboard.salesbychannel

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.dashboard.data.SalesByChannelCustomDateRangeDataStore
import com.woocommerce.android.ui.dashboard.domain.GetSelectedDateRange
import com.woocommerce.android.util.DateUtils
import javax.inject.Inject

class GetSelectedRangeForSalesByChannel @Inject constructor(
    private val appPrefs: AppPrefsWrapper,
    customDateRangeDataStore: SalesByChannelCustomDateRangeDataStore,
    dateUtils: DateUtils
) : GetSelectedDateRange(appPrefs, customDateRangeDataStore, dateUtils) {
    override fun getSelectedRange(): SelectionType =
        runCatching {
            SelectionType.valueOf(appPrefs.getActiveSalesByChannelTab())
        }.getOrDefault(SelectionType.TODAY)
}
