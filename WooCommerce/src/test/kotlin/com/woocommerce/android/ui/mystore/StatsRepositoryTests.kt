package com.woocommerce.android.ui.mystore

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.mystore.data.StatsRepository
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
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WCLeaderboardsStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class StatsRepositoryTests : BaseUnitTest() {

    private val selectedSite: SelectedSite = mock()
    private val wcStatsStore: WCStatsStore = mock()
    private val wcOrderStore: WCOrderStore = mock()
    private val wcLeaderboardsStore: WCLeaderboardsStore = mock()
    private val wooCommerceStore: WooCommerceStore = mock()

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
            wcOrderStore = wcOrderStore,
            wcLeaderboardsStore = wcLeaderboardsStore,
            wooCommerceStore = wooCommerceStore,
            dispatchers = coroutinesTestRule.testDispatchers
        )
    }

    @Test
    fun `when visitors and revenue requests succeed then a success response is returned containing both value`() = testBlocking {
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
}
