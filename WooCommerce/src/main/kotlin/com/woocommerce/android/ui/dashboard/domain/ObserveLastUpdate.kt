package com.woocommerce.android.ui.dashboard.domain

import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsUpdateDataStore
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveLastUpdate @Inject constructor(
    private val analyticsUpdateDataStore: AnalyticsUpdateDataStore
) {
    operator fun invoke(
        selectedRange: StatsTimeRangeSelection,
        analyticDataList: List<AnalyticsUpdateDataStore.AnalyticData>,
        shouldAllCardsBePresent: Boolean = true
    ): Flow<Long?> {
        return analyticsUpdateDataStore.observeLastUpdate(
            rangeSelection = selectedRange,
            analyticData = analyticDataList,
            shouldAllCardsBePresent = shouldAllCardsBePresent
        )
    }

    operator fun invoke(
        selectedRange: StatsTimeRangeSelection,
        analyticData: AnalyticsUpdateDataStore.AnalyticData
    ): Flow<Long?> {
        return analyticsUpdateDataStore.observeLastUpdate(
            rangeSelection = selectedRange,
            analyticData = analyticData
        )
    }
}
