package com.woocommerce.android.ui.analytics.hub.sync

import com.woocommerce.android.model.GoogleAdsStat
import com.woocommerce.android.ui.analytics.hub.GoogleStatsFilterOptions
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.WCGoogleStore.TotalsType

@ExperimentalCoroutinesApi
class UpdateGoogleCampaignStatsTest : BaseUnitTest() {

    private lateinit var sut: UpdateGoogleCampaignStats
    private val analyticsRepository: AnalyticsRepository = mock()

    private val rangeSelection: StatsTimeRangeSelection = mock()
    private val googleAdsStat: GoogleAdsStat = mock()

    @Test
    fun `returns Available state when Google Ads data is fetched successfully`() = testBlocking {
        whenever(analyticsRepository.fetchGoogleAdsStats(rangeSelection, TotalsType.SALES))
            .thenReturn(AnalyticsRepository.GoogleAdsResult.GoogleAdsData(googleAdsStat))

        sut = UpdateGoogleCampaignStats(analyticsRepository)

        val result = sut.invoke(rangeSelection, GoogleStatsFilterOptions.TotalSales).first()
        assertThat(result).isInstanceOf(GoogleAdsState.Available::class.java)
    }

    @Test
    fun `returns Error state when Google Ads data fetch fails`() = testBlocking {
        whenever(analyticsRepository.fetchGoogleAdsStats(rangeSelection, TotalsType.SALES))
            .thenReturn(AnalyticsRepository.GoogleAdsResult.GoogleAdsError)

        sut = UpdateGoogleCampaignStats(analyticsRepository)

        val result = sut.invoke(rangeSelection, GoogleStatsFilterOptions.TotalSales).first()
        assertThat(result).isEqualTo(GoogleAdsState.Error)
    }

    @Test
    fun `returns Available state when filter option is Clicks`() = testBlocking {
        whenever(analyticsRepository.fetchGoogleAdsStats(rangeSelection, TotalsType.CLICKS))
            .thenReturn(AnalyticsRepository.GoogleAdsResult.GoogleAdsData(googleAdsStat))

        sut = UpdateGoogleCampaignStats(analyticsRepository)

        val result = sut.invoke(rangeSelection, GoogleStatsFilterOptions.Clicks).first()
        assertThat(result).isInstanceOf(GoogleAdsState.Available::class.java)
    }

    @Test
    fun `returns Available state when filter option is Impressions`() = testBlocking {
        whenever(analyticsRepository.fetchGoogleAdsStats(rangeSelection, TotalsType.IMPRESSIONS))
            .thenReturn(AnalyticsRepository.GoogleAdsResult.GoogleAdsData(googleAdsStat))

        sut = UpdateGoogleCampaignStats(analyticsRepository)

        val result = sut.invoke(rangeSelection, GoogleStatsFilterOptions.Impressions).first()
        assertThat(result).isInstanceOf(GoogleAdsState.Available::class.java)
    }

    @Test
    fun `returns Available state when filter option is Conversions`() = testBlocking {
        whenever(analyticsRepository.fetchGoogleAdsStats(rangeSelection, TotalsType.CONVERSIONS))
            .thenReturn(AnalyticsRepository.GoogleAdsResult.GoogleAdsData(googleAdsStat))

        sut = UpdateGoogleCampaignStats(analyticsRepository)

        val result = sut.invoke(rangeSelection, GoogleStatsFilterOptions.Conversions).first()
        assertThat(result).isInstanceOf(GoogleAdsState.Available::class.java)
    }
}
