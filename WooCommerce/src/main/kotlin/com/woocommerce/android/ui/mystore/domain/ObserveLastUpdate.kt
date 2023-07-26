package com.woocommerce.android.ui.mystore.domain

import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsUpdateDataStore
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveLastUpdate @Inject constructor(
    private val analyticsUpdateDataStore: AnalyticsUpdateDataStore
) {
    operator fun invoke(
        rangeSelection: StatsTimeRangeSelection,
        analyticData: List<AnalyticsUpdateDataStore.AnalyticData>
    ): Flow<Long?> {
        return analyticsUpdateDataStore.observeLastUpdate(
            rangeSelection = rangeSelection,
            analyticData = analyticData
        )
    }

    operator fun invoke(
        rangeSelection: StatsTimeRangeSelection,
        analyticData: AnalyticsUpdateDataStore.AnalyticData
    ): Flow<Long?> {
        return analyticsUpdateDataStore.observeLastUpdate(
            rangeSelection = rangeSelection,
            analyticData = analyticData
        )
    }
}
