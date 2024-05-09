package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.network.giftcard.GiftCardRestClient
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.action.WCStatsAction
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCBundleStats
import org.wordpress.android.fluxc.model.WCGiftCardStats
import org.wordpress.android.fluxc.model.WCProductBundleItemReport
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCLeaderboardsStore
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class StatsRepositoryTests : BaseUnitTest() {

    private val selectedSite: SelectedSite = mock()
    private val wcStatsStore: WCStatsStore = mock()
    private val wcLeaderboardsStore: WCLeaderboardsStore = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val getWooVersion: GetWooCorePluginCachedVersion = mock()
    private val giftCardRestClient: GiftCardRestClient = mock()

    private lateinit var sut: StatsRepository

    private val defaultSiteModel = SiteModel()
    private val defaultRange = SelectionType.TODAY.generateSelectionData(
        referenceStartDate = Date(),
        referenceEndDate = Date(),
        calendar = Calendar.getInstance(),
        locale = Locale.ROOT
    ).currentRange

    @Before
    fun setup() {
        sut = StatsRepository(
            selectedSite = selectedSite,
            wcStatsStore = wcStatsStore,
            wcLeaderboardsStore = wcLeaderboardsStore,
            wooCommerceStore = wooCommerceStore,
            giftCardRestClient = giftCardRestClient,
            getWooVersion = getWooVersion,
            dispatchers = coroutinesTestRule.testDispatchers
        )
    }

    @Test
    fun `when visitors and revenue requests succeed then a success response is returned containing both value`() =
        testBlocking {
            val granularity = WCStatsStore.StatsGranularity.DAYS
            val startDate = "2024-01-25 00:00:00"
            val endDate = "2024-01-25 23:59:59"
            val visitorStatsResponse = WCStatsStore.OnWCStatsChanged(
                rowsAffected = 2,
                granularity = granularity,
                quantity = "5",
                date = startDate
            )

            val revenueStatsResponse = WCStatsStore.OnWCRevenueStatsChanged(
                rowsAffected = 2,
                granularity = granularity,
                startDate = startDate,
                endDate = endDate
            )

            whenever(selectedSite.get()).thenReturn(defaultSiteModel)
            whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(null)
            whenever(wcStatsStore.fetchNewVisitorStats(any())).thenReturn(visitorStatsResponse)
            whenever(wcStatsStore.fetchRevenueStats(any())).thenReturn(revenueStatsResponse)
            whenever(wcStatsStore.getRawRevenueStats(eq(defaultSiteModel), eq(granularity), eq(startDate), eq(endDate)))
                .thenReturn(WCRevenueStatsModel())

            val result = sut.fetchStats(
                range = defaultRange,
                revenueStatsGranularity = WCStatsStore.StatsGranularity.DAYS,
                visitorStatsGranularity = WCStatsStore.StatsGranularity.DAYS,
                forced = true,
                includeVisitorStats = true
            )

            val model = result.getOrNull()
            assertThat(result.isFailure).isEqualTo(false)
            assertThat(model).isNotNull
            assertThat(model!!.revenue).isNotNull
            assertThat(model.visitors).isNotNull
        }

    @Test
    fun `when visitors requests fails then a success response is returned with visitors null`() = testBlocking {
        val granularity = WCStatsStore.StatsGranularity.DAYS
        val startDate = "2024-01-25 00:00:00"
        val endDate = "2024-01-25 23:59:59"
        val visitorStatsResponse = WCStatsStore.OnWCStatsChanged(0, granularity).also {
            it.error = WCStatsStore.OrderStatsError()
            it.causeOfChange = WCStatsAction.FETCH_NEW_VISITOR_STATS
        }

        val revenueStatsResponse = WCStatsStore.OnWCRevenueStatsChanged(
            rowsAffected = 2,
            granularity = granularity,
            startDate = startDate,
            endDate = endDate
        )

        whenever(selectedSite.get()).thenReturn(defaultSiteModel)
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(null)
        whenever(wcStatsStore.fetchNewVisitorStats(any())).thenReturn(visitorStatsResponse)
        whenever(wcStatsStore.fetchRevenueStats(any())).thenReturn(revenueStatsResponse)
        whenever(wcStatsStore.getRawRevenueStats(eq(defaultSiteModel), eq(granularity), eq(startDate), eq(endDate)))
            .thenReturn(WCRevenueStatsModel())

        val result = sut.fetchStats(
            range = defaultRange,
            revenueStatsGranularity = WCStatsStore.StatsGranularity.DAYS,
            visitorStatsGranularity = WCStatsStore.StatsGranularity.DAYS,
            forced = true,
            includeVisitorStats = true
        )

        val model = result.getOrNull()
        assertThat(result.isFailure).isEqualTo(false)
        assertThat(model).isNotNull
        assertThat(model!!.revenue).isNotNull
        assertThat(model.visitors).isNull()
    }

    @Test
    fun `when revenue requests fails then an error is returned`() = testBlocking {
        val granularity = WCStatsStore.StatsGranularity.DAYS
        val startDate = "2024-01-25 00:00:00"
        val visitorStatsResponse = WCStatsStore.OnWCStatsChanged(
            rowsAffected = 2,
            granularity = granularity,
            quantity = "5",
            date = startDate
        )

        val revenueStatsResponse = WCStatsStore.OnWCRevenueStatsChanged(0, granularity)
            .also { it.error = WCStatsStore.OrderStatsError() }

        whenever(selectedSite.get()).thenReturn(defaultSiteModel)
        whenever(wooCommerceStore.getSiteSettings(any())).thenReturn(null)
        whenever(wcStatsStore.fetchNewVisitorStats(any())).thenReturn(visitorStatsResponse)
        whenever(wcStatsStore.fetchRevenueStats(any())).thenReturn(revenueStatsResponse)

        val result = sut.fetchStats(
            range = defaultRange,
            revenueStatsGranularity = WCStatsStore.StatsGranularity.DAYS,
            visitorStatsGranularity = WCStatsStore.StatsGranularity.DAYS,
            forced = true,
            includeVisitorStats = true
        )

        assertThat(result.isFailure).isEqualTo(true)
    }

    @Test
    fun `when bundle requests fails then an error is returned`() = testBlocking {
        val startDate = "2024-03-25 00:00:00"
        val endDate = "2024-04-1 00:00:00"
        val error = WooError(
            type = WooErrorType.INVALID_RESPONSE,
            original = BaseRequest.GenericErrorType.INVALID_RESPONSE,
            message = "something fails"
        )

        val bundleStatsResponse = WooResult<WCBundleStats>(error)

        whenever(selectedSite.get()).thenReturn(defaultSiteModel)
        whenever(wcStatsStore.fetchProductBundlesStats(any(), any(), any(), any())).thenReturn(bundleStatsResponse)

        val result = sut.fetchProductBundlesStats(
            startDate = startDate,
            endDate = endDate
        )

        assertThat(result.isError).isEqualTo(true)
        assertThat(result.model).isNull()
        assertThat(result.error).isEqualTo(error)
    }

    @Test
    fun `when bundle requests succeed then a valid response is returned`() = testBlocking {
        val startDate = "2024-03-25 00:00:00"
        val endDate = "2024-04-1 00:00:00"
        val stats = WCBundleStats(
            itemsSold = 50,
            netRevenue = 1300.00
        )

        val bundleStatsResponse = WooResult(stats)

        whenever(selectedSite.get()).thenReturn(defaultSiteModel)
        whenever(wcStatsStore.fetchProductBundlesStats(any(), any(), any(), any())).thenReturn(bundleStatsResponse)

        val result = sut.fetchProductBundlesStats(
            startDate = startDate,
            endDate = endDate
        )

        assertThat(result.isError).isEqualTo(false)
        assertThat(result.model).isNotNull
        assertThat(result.error).isNull()
        assertThat(result.model).isEqualTo(stats)
    }

    @Test
    fun `when bundle report requests fails then an error is returned`() = testBlocking {
        val startDate = "2024-03-25 00:00:00"
        val endDate = "2024-04-1 00:00:00"
        val error = WooError(
            type = WooErrorType.INVALID_RESPONSE,
            original = BaseRequest.GenericErrorType.INVALID_RESPONSE,
            message = "something fails"
        )

        val bundleReportResponse = WooResult<List<WCProductBundleItemReport>>(error)

        whenever(selectedSite.get()).thenReturn(defaultSiteModel)
        whenever(wcStatsStore.fetchProductBundlesReport(any(), any(), any(), any())).thenReturn(bundleReportResponse)

        val result = sut.fetchBundleReport(
            startDate = startDate,
            endDate = endDate
        )

        assertThat(result.isError).isEqualTo(true)
        assertThat(result.model).isNull()
        assertThat(result.error).isEqualTo(error)
    }

    @Test
    fun `when bundle report requests succeed then a valid response is returned`() = testBlocking {
        val startDate = "2024-03-25 00:00:00"
        val endDate = "2024-04-1 00:00:00"
        val report = listOf(
            WCProductBundleItemReport(
                name = "item 1",
                image = null,
                itemsSold = 35,
                netRevenue = 1000.00
            ),
            WCProductBundleItemReport(
                name = "item 2",
                image = null,
                itemsSold = 15,
                netRevenue = 300.00
            )
        )

        val bundleReportResponse = WooResult(report)

        whenever(selectedSite.get()).thenReturn(defaultSiteModel)
        whenever(wcStatsStore.fetchProductBundlesReport(any(), any(), any(), any())).thenReturn(bundleReportResponse)

        val result = sut.fetchBundleReport(
            startDate = startDate,
            endDate = endDate
        )

        assertThat(result.isError).isEqualTo(false)
        assertThat(result.model).isNotNull
        assertThat(result.error).isNull()
        assertThat(result.model).isEqualTo(report)
    }

    @Test
    fun `when gift cards requests fails then an error is returned`() = testBlocking {
        val startDate = "2024-03-25 00:00:00"
        val endDate = "2024-04-1 00:00:00"
        val error = WooError(
            type = WooErrorType.INVALID_RESPONSE,
            original = BaseRequest.GenericErrorType.INVALID_RESPONSE,
            message = "something fails"
        )

        val giftCardStatsResponse = WooPayload<GiftCardRestClient.GiftCardsStatsApiResponse>(error)

        whenever(selectedSite.get()).thenReturn(defaultSiteModel)
        whenever(giftCardRestClient.fetchGiftCardStats(any(), any(), any(), any())).thenReturn(giftCardStatsResponse)

        val result = sut.fetchGiftCardStats(
            startDate = startDate,
            endDate = endDate
        )

        assertThat(result.isError).isEqualTo(true)
        assertThat(result.model).isNull()
        assertThat(result.error).isEqualTo(error)
    }

    @Test
    fun `when gift cards requests succeed then a valid response is returned`() = testBlocking {
        val startDate = "2024-03-25 00:00:00"
        val endDate = "2024-04-1 00:00:00"
        val giftCardsUsed = 26L
        val giftCardsNetAmount = 345.87
        val stats = GiftCardRestClient.GiftCardsStatsApiResponse(
            totals = GiftCardRestClient.GiftCardsStatsTotals(
                count = giftCardsUsed,
                netAmount = giftCardsNetAmount
            ),
            intervals = emptyList()
        )
        val expectedResult = WCGiftCardStats(
            usedValue = giftCardsUsed,
            netValue = giftCardsNetAmount,
            intervals = emptyList()
        )

        val giftCardsStatsResponse = WooPayload(stats)

        whenever(selectedSite.get()).thenReturn(defaultSiteModel)
        whenever(giftCardRestClient.fetchGiftCardStats(any(), any(), any(), any())).thenReturn(giftCardsStatsResponse)

        val result = sut.fetchGiftCardStats(
            startDate = startDate,
            endDate = endDate
        )

        assertThat(result.isError).isEqualTo(false)
        assertThat(result.model).isNotNull
        assertThat(result.error).isNull()
        assertThat(result.model).isEqualTo(expectedResult)
    }
}
