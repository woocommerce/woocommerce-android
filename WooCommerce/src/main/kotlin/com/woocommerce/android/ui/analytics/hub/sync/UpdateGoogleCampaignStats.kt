package com.woocommerce.android.ui.analytics.hub.sync

import com.woocommerce.android.model.GoogleAdsStat
import com.woocommerce.android.model.StatType
import com.woocommerce.android.ui.analytics.hub.GoogleStatsFilterOptions
import com.woocommerce.android.ui.analytics.hub.GoogleStatsFilterOptions.Clicks
import com.woocommerce.android.ui.analytics.hub.GoogleStatsFilterOptions.Conversions
import com.woocommerce.android.ui.analytics.hub.GoogleStatsFilterOptions.Impressions
import com.woocommerce.android.ui.analytics.hub.GoogleStatsFilterOptions.TotalSales
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class UpdateGoogleCampaignStats @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) {
    private val _googleAdsState = MutableStateFlow(GoogleAdsState.Available(GoogleAdsStat.EMPTY) as GoogleAdsState)
    val googleAdsState: Flow<GoogleAdsState> = _googleAdsState

    suspend operator fun invoke(
        rangeSelection: StatsTimeRangeSelection,
        filterOption: GoogleStatsFilterOptions
    ): Flow<GoogleAdsState> {
        fetchGoogleAdsAsync(rangeSelection, filterOption.toStatType())
        return googleAdsState
    }

    private fun GoogleStatsFilterOptions.toStatType() =
        when (this) {
            TotalSales -> StatType.TOTAL_SALES
            Clicks -> StatType.CLICKS
            Impressions -> StatType.IMPRESSIONS
            Conversions -> StatType.CONVERSIONS
        }

    private suspend fun fetchGoogleAdsAsync(
        rangeSelection: StatsTimeRangeSelection,
        selectedStatsTotalType: StatType
    ) = analyticsRepository.fetchGoogleAdsStats(rangeSelection, selectedStatsTotalType)
        .run { this as? AnalyticsRepository.GoogleAdsResult.GoogleAdsData }
        ?.let { _googleAdsState.value = GoogleAdsState.Available(it.googleAdsStat) }
        ?: _googleAdsState.update { GoogleAdsState.Error }
}
