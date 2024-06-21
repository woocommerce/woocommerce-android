package com.woocommerce.android.wear.ui.stats.datasource

import com.woocommerce.android.BaseUnitTest
import com.woocommerce.android.wear.analytics.AnalyticsTracker
import com.woocommerce.android.wear.phone.PhoneConnectionRepository
import com.woocommerce.android.wear.system.NetworkStatus
import com.woocommerce.android.wear.ui.stats.datasource.FetchStats.StoreStatsRequest
import com.woocommerce.android.wear.ui.stats.datasource.FetchStats.StoreStatsRequest.Finished
import com.woocommerce.android.wear.ui.stats.datasource.FetchStats.StoreStatsRequest.Waiting
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WooCommerceStore

@ExperimentalCoroutinesApi
class FetchStatsTest : BaseUnitTest() {

    private val statsRepository: StatsRepository = mock()
    private val phoneRepository: PhoneConnectionRepository = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val networkStatus: NetworkStatus = mock()
    private val analyticsTracker: AnalyticsTracker = mock()
    private val selectedSite: SiteModel = mock()

    @Test
    fun `returns Finished when stats are available from store`() = testBlocking {
        val expectedData = generateStatsDataWithExpectedMocks()

        val event = createSut()
            .invoke(selectedSite)
            .first()

        assertThat(event).isEqualTo(Finished(expectedData))
    }

    @Test
    fun `returns Waiting when no stats and not timeout`() = testBlocking {
        whenever(statsRepository.fetchRevenueStats(selectedSite)).thenReturn(Result.failure(Throwable()))
        whenever(statsRepository.fetchVisitorStats(selectedSite)).thenReturn(Result.failure(Throwable()))
        whenever(networkStatus.isConnected()).thenReturn(true)

        val event = createSut()
            .invoke(selectedSite)
            .first()

        assertThat(event).isEqualTo(Waiting)
    }

    @Test
    fun `returns Error when no stats and timeout`() = testBlocking {
        whenever(statsRepository.fetchRevenueStats(selectedSite)).thenReturn(Result.failure(Throwable()))
        whenever(statsRepository.fetchVisitorStats(selectedSite)).thenReturn(Result.failure(Throwable()))
        whenever(networkStatus.isConnected()).thenReturn(true)

        val event = createSut()
            .invoke(selectedSite)
            .filter { it !is Waiting }
            .first()

        assertThat(event).isEqualTo(StoreStatsRequest.Error)
    }

    @Test
    fun `returns stats from phone when no network connection`() = testBlocking {
        val expectedData = StoreStatsData(StoreStatsData.RevenueData("100.0", 5), 1)
        whenever(phoneRepository.isPhoneConnectionAvailable()).thenReturn(true)
        whenever(networkStatus.isConnected()).thenReturn(false)
        whenever(statsRepository.observeStatsDataChanges(selectedSite)).thenReturn(flowOf(expectedData))
        val events = mutableListOf<StoreStatsRequest>()

        createSut()
            .invoke(selectedSite)
            .onEach { events.add(it) }
            .launchIn(this)
        advanceUntilIdle()

        verify(statsRepository, never()).fetchRevenueStats(selectedSite)
        verify(statsRepository, never()).fetchVisitorStats(selectedSite)
        assertThat(events).containsExactly(Finished(expectedData))
    }

    @Test
    fun `returns Error when no network and phone connection`() = testBlocking {
        whenever(phoneRepository.isPhoneConnectionAvailable()).thenReturn(false)
        whenever(networkStatus.isConnected()).thenReturn(false)
        val events = mutableListOf<StoreStatsRequest>()

        createSut()
            .invoke(selectedSite)
            .onEach { events.add(it) }
            .launchIn(this)
        advanceUntilIdle()

        assertThat(events).containsExactly(StoreStatsRequest.Error)
    }

    private fun createSut() =
        FetchStats(statsRepository, phoneRepository, wooCommerceStore, networkStatus, analyticsTracker)

    private suspend fun generateStatsDataWithExpectedMocks(): StoreStatsData {
        val expectedData = StoreStatsData(StoreStatsData.RevenueData("100.0", 5), 1)
        val revenueTotalsResponse = mock<WCRevenueStatsModel.Total> {
            on { totalSales } doReturn 100.0
            on { ordersCount } doReturn 5
        }
        val revenueResponse = mock<WCRevenueStatsModel> {
            on { parseTotal() } doReturn revenueTotalsResponse
        }
        whenever(statsRepository.fetchRevenueStats(selectedSite)).thenReturn(Result.success(revenueResponse))
        whenever(statsRepository.fetchVisitorStats(selectedSite)).thenReturn(Result.success(1))
        whenever(networkStatus.isConnected()).thenReturn(true)
        whenever(wooCommerceStore.formatCurrencyForDisplay(100.0, selectedSite, null, true)).thenReturn("100.0")
        return expectedData
    }
}
