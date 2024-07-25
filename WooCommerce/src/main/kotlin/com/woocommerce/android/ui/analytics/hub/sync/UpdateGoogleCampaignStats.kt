package com.woocommerce.android.ui.analytics.hub.sync

import com.woocommerce.android.ui.analytics.hub.GoogleStatsFilterOptions
import com.woocommerce.android.ui.analytics.hub.GoogleStatsFilterOptions.Clicks
import com.woocommerce.android.ui.analytics.hub.GoogleStatsFilterOptions.Conversions
import com.woocommerce.android.ui.analytics.hub.GoogleStatsFilterOptions.Impressions
import com.woocommerce.android.ui.analytics.hub.GoogleStatsFilterOptions.TotalSales
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.store.WCGoogleStore.TotalsType
import org.wordpress.android.fluxc.store.WCGoogleStore.TotalsType.CLICKS
import org.wordpress.android.fluxc.store.WCGoogleStore.TotalsType.CONVERSIONS
import org.wordpress.android.fluxc.store.WCGoogleStore.TotalsType.IMPRESSIONS
import org.wordpress.android.fluxc.store.WCGoogleStore.TotalsType.SALES

class UpdateGoogleCampaignStats @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) {
    suspend operator fun invoke(
        rangeSelection: StatsTimeRangeSelection,
        filterOption: GoogleStatsFilterOptions
    ): Flow<GoogleAdsState> {
        return flow {
            emit(GoogleAdsState.Loading)
            emit(fetchGoogleAdsAsync(rangeSelection, filterOption.toTotalType()))
        }
    }

    private fun GoogleStatsFilterOptions.toTotalType() =
        when (this) {
            TotalSales -> SALES
            Clicks -> CLICKS
            Impressions -> IMPRESSIONS
            Conversions -> CONVERSIONS
        }

    private suspend fun fetchGoogleAdsAsync(
        rangeSelection: StatsTimeRangeSelection,
        selectedStatsTotalType: TotalsType
    ) = analyticsRepository.fetchGoogleAdsStats(rangeSelection, selectedStatsTotalType)
        .run { this as? AnalyticsRepository.GoogleAdsResult.GoogleAdsData }
        ?.let { GoogleAdsState.Available(it.googleAdsStat) }
        ?: GoogleAdsState.Error
}
