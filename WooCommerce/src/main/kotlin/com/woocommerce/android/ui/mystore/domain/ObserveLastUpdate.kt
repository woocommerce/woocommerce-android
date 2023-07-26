package com.woocommerce.android.ui.mystore.domain

import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsUpdateDataStore
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.store.WCStatsStore
import javax.inject.Inject

class ObserveLastUpdate @Inject constructor(
    private val analyticsUpdateDataStore: AnalyticsUpdateDataStore
) {
    operator fun invoke(
        granularity: WCStatsStore.StatsGranularity,
        analyticData: List<AnalyticsUpdateDataStore.AnalyticData>
    ): Flow<Long?> {
        val selectionType = StatsTimeRangeSelection.SelectionType.from(granularity)
        return analyticsUpdateDataStore.observeLastUpdate(
            selectionType = selectionType,
            analyticData = analyticData
        )
    }

    operator fun invoke(
        granularity: WCStatsStore.StatsGranularity,
        analyticData: AnalyticsUpdateDataStore.AnalyticData
    ): Flow<Long?> {
        val selectionType = StatsTimeRangeSelection.SelectionType.from(granularity)
        return analyticsUpdateDataStore.observeLastUpdate(
            selectionType = selectionType,
            analyticData = analyticData
        )
    }
}
