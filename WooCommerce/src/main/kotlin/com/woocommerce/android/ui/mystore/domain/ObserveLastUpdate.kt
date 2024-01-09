package com.woocommerce.android.ui.mystore.domain

import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsUpdateDataStore
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.locale.LocaleProvider
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.store.WCStatsStore
import javax.inject.Inject

class ObserveLastUpdate @Inject constructor(
    private val analyticsUpdateDataStore: AnalyticsUpdateDataStore,
    private val localeProvider: LocaleProvider,
    private val dateUtils: DateUtils
) {
    operator fun invoke(
        granularity: WCStatsStore.StatsGranularity,
        analyticData: List<AnalyticsUpdateDataStore.AnalyticData>
    ): Flow<Long?> {
        val rangeSelection = granularity.asRangeSelection(
            dateUtils = dateUtils,
            locale = localeProvider.provideLocale()
        )
        return analyticsUpdateDataStore.observeLastUpdate(
            rangeSelection = rangeSelection,
            analyticData = analyticData
        )
    }

    operator fun invoke(
        granularity: WCStatsStore.StatsGranularity,
        analyticData: AnalyticsUpdateDataStore.AnalyticData
    ): Flow<Long?> {
        val rangeSelection = granularity.asRangeSelection(
            dateUtils = dateUtils,
            locale = localeProvider.provideLocale()
        )
        return analyticsUpdateDataStore.observeLastUpdate(
            rangeSelection = rangeSelection,
            analyticData = analyticData
        )
    }

    operator fun invoke(
        timeRangeSelection: StatsTimeRangeSelection
    ): Flow<Long?> {
        return analyticsUpdateDataStore.observeLastUpdate(
            rangeSelection = timeRangeSelection,
            analyticData = AnalyticsUpdateDataStore.AnalyticData.ALL
        )
    }
}
