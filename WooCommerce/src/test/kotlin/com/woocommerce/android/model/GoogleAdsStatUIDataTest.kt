package com.woocommerce.android.model

import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class GoogleAdsStatUIDataTest : BaseUnitTest() {

    private val currencyFormatter: CurrencyFormatter = mock()
    private val resourceProvider: ResourceProvider = mock()

    @Test
    fun `mainTotalStat is formatted correctly for TOTAL_SALES`() = testBlocking {
        whenever(resourceProvider.getString(any())).thenReturn("Total Sales")
        whenever(currencyFormatter.formatCurrency("1000.0")).thenReturn("$1000.0")

        val sut = GoogleAdsStatUIData(rawStats, StatType.TOTAL_SALES, currencyFormatter, resourceProvider)

        assertThat(sut.mainTotalStat).isEqualTo("$1000.0")
        assertThat(sut.mainTotalStatTitle).isEqualTo("Total Sales")
    }

    @Test
    fun `mainTotalStat is empty when sales is null for TOTAL_SALES`() = testBlocking {
        val rawStatsWithNullSales = rawStats.copy(totals = rawStats.totals.copy(sales = 0.0))
        whenever(resourceProvider.getString(any())).thenReturn("Total Sales")
        whenever(currencyFormatter.formatCurrency("0.0")).thenReturn("0")

        val sut = GoogleAdsStatUIData(rawStatsWithNullSales, StatType.TOTAL_SALES, currencyFormatter, resourceProvider)

        assertThat(sut.mainTotalStat).isEqualTo("0")
    }

    @Test
    fun `statItems are mapped correctly for TOTAL_SALES`() = testBlocking {
        whenever(currencyFormatter.formatCurrency("500.0")).thenReturn("$500.0")
        whenever(currencyFormatter.formatCurrency("100.0")).thenReturn("$100.0")
        whenever(resourceProvider.getString(any(), eq("$100.0"))).thenReturn("$100.0")

        val sut = GoogleAdsStatUIData(rawStats, StatType.TOTAL_SALES, currencyFormatter, resourceProvider)

        assertThat(sut.statItems).hasSize(2)
        assertThat(sut.statItems[0].name).isEqualTo("Campaign 1")
        assertThat(sut.statItems[0].mainStat).isEqualTo("$500.0")
        assertThat(sut.statItems[0].secondaryStat).isEqualTo("$100.0")
    }

    @Test
    fun `deltaPercentage is mapped correctly for TOTAL_SALES`() = testBlocking {
        val sut = GoogleAdsStatUIData(rawStats, StatType.TOTAL_SALES, currencyFormatter, resourceProvider)

        assertThat(sut.deltaPercentage).isEqualTo(10)
    }

    @Test
    fun `mainTotalStat is formatted correctly for SPEND`() = testBlocking {
        whenever(resourceProvider.getString(any())).thenReturn("Spend")
        whenever(currencyFormatter.formatCurrency("200.0")).thenReturn("$200.0")

        val sut = GoogleAdsStatUIData(rawStats, StatType.SPEND, currencyFormatter, resourceProvider)

        assertThat(sut.mainTotalStat).isEqualTo("$200.0")
        assertThat(sut.mainTotalStatTitle).isEqualTo("Spend")
    }

    @Test
    fun `mainTotalStat is empty when spend is null for SPEND`() = testBlocking {
        val rawStatsWithNullSpend = rawStats.copy(totals = rawStats.totals.copy(spend = 0.0))
        whenever(resourceProvider.getString(any())).thenReturn("Spend")
        whenever(currencyFormatter.formatCurrency("0.0")).thenReturn("0")

        val sut = GoogleAdsStatUIData(rawStatsWithNullSpend, StatType.SPEND, currencyFormatter, resourceProvider)

        assertThat(sut.mainTotalStat).isEqualTo("0")
    }

    @Test
    fun `statItems are mapped correctly for SPEND`() = testBlocking {
        whenever(currencyFormatter.formatCurrency("500.0")).thenReturn("$500.0")
        whenever(currencyFormatter.formatCurrency("100.0")).thenReturn("$100.0")
        whenever(resourceProvider.getString(any(), eq("$500.0"))).thenReturn("$500.0")

        val sut = GoogleAdsStatUIData(rawStats, StatType.SPEND, currencyFormatter, resourceProvider)

        assertThat(sut.statItems).hasSize(2)
        assertThat(sut.statItems[0].name).isEqualTo("Campaign 1")
        assertThat(sut.statItems[0].mainStat).isEqualTo("$100.0")
        assertThat(sut.statItems[0].secondaryStat).isEqualTo("$500.0")
    }

    @Test
    fun `deltaPercentage is mapped correctly for SPEND`() = testBlocking {
        val sut = GoogleAdsStatUIData(rawStats, StatType.SPEND, currencyFormatter, resourceProvider)

        assertThat(sut.deltaPercentage).isEqualTo(5)
    }

    @Test
    fun `mainTotalStat is formatted correctly for CLICKS`() = testBlocking {
        whenever(resourceProvider.getString(any())).thenReturn("Clicks")

        val sut = GoogleAdsStatUIData(rawStats, StatType.CLICKS, currencyFormatter, resourceProvider)

        assertThat(sut.mainTotalStat).isEqualTo("300")
        assertThat(sut.mainTotalStatTitle).isEqualTo("Clicks")
    }

    @Test
    fun `mainTotalStat is empty when clicks is null for CLICKS`() = testBlocking {
        val rawStatsWithNullClicks = rawStats.copy(totals = rawStats.totals.copy(clicks = 0))
        whenever(resourceProvider.getString(any())).thenReturn("Clicks")

        val sut = GoogleAdsStatUIData(rawStatsWithNullClicks, StatType.CLICKS, currencyFormatter, resourceProvider)

        assertThat(sut.mainTotalStat).isEqualTo("0")
    }

    @Test
    fun `statItems are mapped correctly for CLICKS`() = testBlocking {
        whenever(currencyFormatter.formatCurrency("100.0")).thenReturn("$100.0")
        whenever(resourceProvider.getString(any(), eq("$100.0"))).thenReturn("$100.0")
        val sut = GoogleAdsStatUIData(rawStats, StatType.CLICKS, currencyFormatter, resourceProvider)

        assertThat(sut.statItems).hasSize(2)
        assertThat(sut.statItems[0].name).isEqualTo("Campaign 1")
        assertThat(sut.statItems[0].mainStat).isEqualTo("150")
        assertThat(sut.statItems[0].secondaryStat).isEqualTo("$100.0")
    }

    @Test
    fun `deltaPercentage is mapped correctly for CLICKS`() = testBlocking {
        val sut = GoogleAdsStatUIData(rawStats, StatType.CLICKS, currencyFormatter, resourceProvider)

        assertThat(sut.deltaPercentage).isEqualTo(15)
    }

    @Test
    fun `mainTotalStat is formatted correctly for IMPRESSIONS`() = testBlocking {
        whenever(resourceProvider.getString(any())).thenReturn("Impressions")

        val sut = GoogleAdsStatUIData(rawStats, StatType.IMPRESSIONS, currencyFormatter, resourceProvider)

        assertThat(sut.mainTotalStat).isEqualTo("400")
        assertThat(sut.mainTotalStatTitle).isEqualTo("Impressions")
    }

    @Test
    fun `mainTotalStat is empty when impressions is null for IMPRESSIONS`() = testBlocking {
        val rawStatsWithNullImpressions = rawStats.copy(totals = rawStats.totals.copy(impressions = 0))
        whenever(resourceProvider.getString(any())).thenReturn("Impressions")

        val sut =
            GoogleAdsStatUIData(rawStatsWithNullImpressions, StatType.IMPRESSIONS, currencyFormatter, resourceProvider)

        assertThat(sut.mainTotalStat).isEqualTo("0")
    }

    @Test
    fun `statItems are mapped correctly for IMPRESSIONS`() = testBlocking {
        whenever(currencyFormatter.formatCurrency("100.0")).thenReturn("$100.0")
        whenever(resourceProvider.getString(any(), eq("$100.0"))).thenReturn("$100.0")
        val sut = GoogleAdsStatUIData(rawStats, StatType.IMPRESSIONS, currencyFormatter, resourceProvider)

        assertThat(sut.statItems).hasSize(2)
        assertThat(sut.statItems[0].name).isEqualTo("Campaign 1")
        assertThat(sut.statItems[0].mainStat).isEqualTo("200")
        assertThat(sut.statItems[0].secondaryStat).isEqualTo("$100.0")
    }

    @Test
    fun `deltaPercentage is mapped correctly for IMPRESSIONS`() = testBlocking {
        val sut = GoogleAdsStatUIData(rawStats, StatType.IMPRESSIONS, currencyFormatter, resourceProvider)

        assertThat(sut.deltaPercentage).isEqualTo(20)
    }

    @Test
    fun `mainTotalStat is formatted correctly for CONVERSIONS`() = testBlocking {
        whenever(resourceProvider.getString(any())).thenReturn("Conversions")

        val sut = GoogleAdsStatUIData(rawStats, StatType.CONVERSIONS, currencyFormatter, resourceProvider)

        assertThat(sut.mainTotalStat).isEqualTo("50")
        assertThat(sut.mainTotalStatTitle).isEqualTo("Conversions")
    }

    @Test
    fun `mainTotalStat is empty when conversions is null for CONVERSIONS`() = testBlocking {
        val rawStatsWithNullConversions = rawStats.copy(totals = rawStats.totals.copy(conversions = 0))
        whenever(resourceProvider.getString(any())).thenReturn("Conversions")

        val sut =
            GoogleAdsStatUIData(rawStatsWithNullConversions, StatType.CONVERSIONS, currencyFormatter, resourceProvider)

        assertThat(sut.mainTotalStat).isEqualTo("0")
    }

    @Test
    fun `statItems are mapped correctly for CONVERSIONS`() = testBlocking {
        whenever(currencyFormatter.formatCurrency("100.0")).thenReturn("$100.0")
        whenever(resourceProvider.getString(any(), eq("$100.0"))).thenReturn("$100.0")
        val sut = GoogleAdsStatUIData(rawStats, StatType.CONVERSIONS, currencyFormatter, resourceProvider)

        assertThat(sut.statItems).hasSize(2)
        assertThat(sut.statItems[0].name).isEqualTo("Campaign 1")
        assertThat(sut.statItems[0].mainStat).isEqualTo("25")
        assertThat(sut.statItems[0].secondaryStat).isEqualTo("$100.0")
    }

    @Test
    fun `deltaPercentage is mapped correctly for CONVERSIONS`() = testBlocking {
        val sut = GoogleAdsStatUIData(rawStats, StatType.CONVERSIONS, currencyFormatter, resourceProvider)

        assertThat(sut.deltaPercentage).isEqualTo(30)
    }

    private val rawStats = GoogleAdsStat(
        totals = GoogleAdsTotals(
            sales = 1000.0,
            spend = 200.0,
            clicks = 300,
            impressions = 400,
            conversions = 50
        ),
        googleAdsCampaigns = listOf(
            GoogleAdsCampaign(
                id = 1,
                name = "Campaign 1",
                subtotal = GoogleAdsTotals(
                    sales = 500.0,
                    spend = 100.0,
                    clicks = 150,
                    impressions = 200,
                    conversions = 25
                )
            ),
            GoogleAdsCampaign(
                id = 2,
                name = "Campaign 2",
                subtotal = GoogleAdsTotals(
                    sales = 500.0,
                    spend = 100.0,
                    clicks = 150,
                    impressions = 200,
                    conversions = 25
                )
            )
        ),
        totalsDeltaPercentage = GoogleAdsTotalsDeltaPercentage(
            salesDelta = DeltaPercentage.Value(10),
            spendDelta = DeltaPercentage.Value(5),
            clicksDelta = DeltaPercentage.Value(15),
            impressionsDelta = DeltaPercentage.Value(20),
            conversionsDelta = DeltaPercentage.Value(30)
        )
    )
}
