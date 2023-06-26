package com.woocommerce.android.ui.analytics.hub.sync

import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import javax.inject.Inject

class ShouldFetchNewStatsData @Inject constructor() {
    operator fun invoke(rangeSelection: StatsTimeRangeSelection): Boolean {
        return true
    }
}
