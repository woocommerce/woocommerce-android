package com.woocommerce.android.ui.mystore.domain

import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsUpdateDataStore
import com.woocommerce.android.util.locale.LocaleProvider
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.store.WCStatsStore
import javax.inject.Inject

class ObserveLastUpdate @Inject constructor(
    private val analyticsUpdateDataStore: AnalyticsUpdateDataStore,
    private val localeProvider: LocaleProvider
) {
    operator fun invoke(
        granularity: WCStatsStore.StatsGranularity,
        analyticData: List<AnalyticsUpdateDataStore.AnalyticData>
    ): Flow<Long?> {
        val rangeSelection = granularity.asRangeSelection(localeProvider.provideLocale())
        return analyticsUpdateDataStore.observeLastUpdate(
            rangeSelection = rangeSelection,
            analyticData = analyticData
        )
    }

    operator fun invoke(
        granularity: WCStatsStore.StatsGranularity,
        analyticData: AnalyticsUpdateDataStore.AnalyticData
    ): Flow<Long?> {
        val rangeSelection = granularity.asRangeSelection(localeProvider.provideLocale())
        return analyticsUpdateDataStore.observeLastUpdate(
            rangeSelection = rangeSelection,
            analyticData = analyticData
        )
    }
}
