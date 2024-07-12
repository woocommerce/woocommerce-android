package com.woocommerce.android.ui.dashboard.stats

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsUpdateDataStore
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.dashboard.data.StatsRepository
import com.woocommerce.android.ui.dashboard.data.StatsRepository.StatsException
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsError
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsErrorType.PLUGIN_NOT_ACTIVE
import java.util.Calendar
import java.util.Date
import java.util.Locale

@ExperimentalCoroutinesApi
class GetStatsTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()
    private val statsRepository: StatsRepository = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val analyticsUpdateDataStore: AnalyticsUpdateDataStore = mock()

    private val getStats = GetStats(
        selectedSite = selectedSite,
        statsRepository = statsRepository,
        appPrefsWrapper = appPrefsWrapper,
        coroutineDispatchers = coroutinesTestRule.testDispatchers,
        analyticsUpdateDataStore = analyticsUpdateDataStore,
    )

    @Before
    fun setup() = testBlocking {
        givenIsJetpackConnected(false)
        givenShouldUpdateAnalyticsReturns(true)
        givenFetchRevenueStats(Result.success(ANY_REVENUE_STATS))
        givenFetchVisitorStats(Result.success(ANY_VISITOR_STATS))
        givenFetchTotalVisitorStats(Result.success(ANY_TOTAL_VISITOR_COUNT))
    }

    @Test
    fun `Given revenue stats success, when get stats, then emits revenue stats`() =
        testBlocking {
            givenFetchRevenueStats(Result.success(ANY_REVENUE_STATS))

            val result = getStats(refresh = false, selectedRange = ANY_STATS_RANGE_SELECTION)
                .filter { it is GetStats.LoadStatsResult.RevenueStatsSuccess }
                .first()

            assertThat(result).isEqualTo(GetStats.LoadStatsResult.RevenueStatsSuccess(ANY_REVENUE_STATS))
        }

    @Test
    fun `Given revenue stats error is not plugin not active error, when get stats, then emits revenue stats`() =
        testBlocking {
            givenFetchRevenueStats(Result.failure(StatsException(GENERIC_ORDER_STATS_ERROR)))

            val result = getStats(refresh = false, selectedRange = ANY_STATS_RANGE_SELECTION)
                .filter { it is GetStats.LoadStatsResult.RevenueStatsError }
                .first()

            assertThat(result).isEqualTo(GetStats.LoadStatsResult.RevenueStatsError(GENERIC_ORDER_STATS_ERROR.message))
        }

    @Test
    fun `Given revenue stats error is plugin not active error, when get stats, then emits revenue stats`() =
        testBlocking {
            givenFetchRevenueStats(Result.failure(StatsException(PLUGIN_NOT_ACTIVE_ORDER_STATS_ERROR)))

            val result = getStats(refresh = false, selectedRange = ANY_STATS_RANGE_SELECTION)
                .filter { it is GetStats.LoadStatsResult.PluginNotActive }
                .first()

            assertThat(result).isEqualTo(GetStats.LoadStatsResult.PluginNotActive)
        }

    @Test
    fun `Given revenue stats success, when get stats, save stats supported in prefs`() =
        testBlocking {
            givenFetchRevenueStats(Result.success(ANY_REVENUE_STATS))

            getStats(refresh = false, selectedRange = ANY_STATS_RANGE_SELECTION).collect()

            verify(appPrefsWrapper).setV4StatsSupported(true)
        }

    @Test
    fun `Given revenue stats error is plugin not active error, when get stats, save stats not supported in prefs`() =
        testBlocking {
            givenFetchRevenueStats(Result.failure(StatsException(PLUGIN_NOT_ACTIVE_ORDER_STATS_ERROR)))

            getStats(refresh = false, selectedRange = ANY_STATS_RANGE_SELECTION).collect()

            verify(appPrefsWrapper).setV4StatsSupported(false)
        }

    @Test
    fun `Given visitor stats success, when get stats, then emits visitor stats`() =
        testBlocking {
            givenFetchVisitorStats(Result.success(ANY_VISITOR_STATS))
            givenFetchTotalVisitorStats(Result.success(ANY_TOTAL_VISITOR_COUNT))

            val result = getStats(refresh = false, selectedRange = ANY_STATS_RANGE_SELECTION)
                .filter { it is GetStats.LoadStatsResult.VisitorsStatsSuccess }
                .first()

            assertThat(result).isEqualTo(
                GetStats.LoadStatsResult.VisitorsStatsSuccess(
                    ANY_VISITOR_STATS,
                    ANY_TOTAL_VISITOR_COUNT
                )
            )
        }

    @Test
    fun `Given visitor stats error, when get stats, then emits visitor stats`() =
        testBlocking {
            givenFetchVisitorStats(Result.failure(StatsException(GENERIC_ORDER_STATS_ERROR)))

            val result = getStats(refresh = false, selectedRange = ANY_STATS_RANGE_SELECTION)
                .filter { it is GetStats.LoadStatsResult.VisitorsStatsError }
                .first()

            assertThat(result).isEqualTo(GetStats.LoadStatsResult.VisitorsStatsError)
        }

    @Test
    fun `Given total visitor count error, when get stats, then emits visitor stats error`() =
        testBlocking {
            givenFetchTotalVisitorStats(Result.failure(StatsException(GENERIC_ORDER_STATS_ERROR)))

            val result = getStats(refresh = false, selectedRange = ANY_STATS_RANGE_SELECTION)
                .filter { it is GetStats.LoadStatsResult.VisitorsStatsError }
                .first()

            assertThat(result).isEqualTo(GetStats.LoadStatsResult.VisitorsStatsError)
        }

    @Test
    fun `Given jetpack CP is connected, when get stats, then emits is jetpack connected`() =
        testBlocking {
            givenIsJetpackConnected(true)

            val result = getStats(refresh = false, selectedRange = ANY_STATS_RANGE_SELECTION)
                .filter { it is GetStats.LoadStatsResult.VisitorStatUnavailable }
                .first()

            assertThat(result).isEqualTo(GetStats.LoadStatsResult.VisitorStatUnavailable)
        }

    @Test
    fun `Given refresh is false, then check if analytic should update`() =
        testBlocking {
            givenFetchRevenueStats(Result.success(ANY_REVENUE_STATS))

            getStats(refresh = false, selectedRange = ANY_STATS_RANGE_SELECTION).collect()

            verify(analyticsUpdateDataStore).shouldUpdateAnalytics(
                rangeSelection = any(),
                analyticData = eq(AnalyticsUpdateDataStore.AnalyticData.REVENUE),
                maxOutdatedTime = any()
            )
            verify(analyticsUpdateDataStore).shouldUpdateAnalytics(
                rangeSelection = any(),
                analyticData = eq(AnalyticsUpdateDataStore.AnalyticData.VISITORS),
                maxOutdatedTime = any()
            )
        }

    @Test
    fun `Given refresh is true, then don't check if analytic should update`() =
        testBlocking {
            givenFetchRevenueStats(Result.success(ANY_REVENUE_STATS))

            getStats(refresh = true, selectedRange = ANY_STATS_RANGE_SELECTION).collect()

            verify(analyticsUpdateDataStore, never())
                .shouldUpdateAnalytics(rangeSelection = any(), analyticData = any(), maxOutdatedTime = any())
        }

    @Test
    fun `Given refresh is forced, then update last analytic update`() =
        testBlocking {
            givenFetchRevenueStats(Result.success(ANY_REVENUE_STATS))

            getStats(refresh = true, selectedRange = ANY_STATS_RANGE_SELECTION).collect()

            verify(analyticsUpdateDataStore)
                .storeLastAnalyticsUpdate(
                    any(),
                    eq(AnalyticsUpdateDataStore.AnalyticData.REVENUE)
                )
        }

    @Test
    fun `Given should update is true, then update last analytic update`() =
        testBlocking {
            givenFetchRevenueStats(Result.success(ANY_REVENUE_STATS))
            givenShouldUpdateAnalyticsReturns(true)

            getStats(refresh = false, selectedRange = ANY_STATS_RANGE_SELECTION).collect()

            verify(analyticsUpdateDataStore).storeLastAnalyticsUpdate(
                any(),
                eq(AnalyticsUpdateDataStore.AnalyticData.REVENUE)
            )
        }

    @Test
    fun `Given should update is false, then don't update last analytic update`() =
        testBlocking {
            getRevenueStatsById(Result.success(ANY_REVENUE_STATS))
            givenShouldUpdateAnalyticsReturns(false)

            getStats(refresh = false, selectedRange = ANY_STATS_RANGE_SELECTION).collect()

            verify(analyticsUpdateDataStore, never())
                .storeLastAnalyticsUpdate(
                    any(),
                    eq(AnalyticsUpdateDataStore.AnalyticData.REVENUE)
                )
        }

    @Test
    fun `Given should update is false and the revenue is on local cache then don't call fetch`() =
        testBlocking {
            getRevenueStatsById(Result.success(ANY_REVENUE_STATS))
            givenShouldUpdateAnalyticsReturns(false)

            getStats(refresh = false, selectedRange = ANY_STATS_RANGE_SELECTION).collect()

            verify(statsRepository, never()).fetchRevenueStats(any(), any(), any(), any())
        }

    @Test
    fun `Given should update is false and the revenue is NOT on local cache then call fetch with refresh false`() =
        testBlocking {
            getRevenueStatsById(Result.success(null))
            givenShouldUpdateAnalyticsReturns(false)

            getStats(refresh = false, selectedRange = ANY_STATS_RANGE_SELECTION).collect()

            verify(statsRepository).fetchRevenueStats(any(), any(), eq(false), any())
        }

    @Test
    fun `Given should update is true and result fail, then don't update last analytic update`() =
        testBlocking {
            givenFetchRevenueStats(Result.failure(StatsException(GENERIC_ORDER_STATS_ERROR)))
            givenShouldUpdateAnalyticsReturns(true)

            getStats(refresh = false, selectedRange = ANY_STATS_RANGE_SELECTION).collect()

            verify(analyticsUpdateDataStore, never())
                .storeLastAnalyticsUpdate(
                    any(),
                    eq(AnalyticsUpdateDataStore.AnalyticData.REVENUE)
                )
        }

    private suspend fun givenFetchRevenueStats(result: Result<WCRevenueStatsModel?>) {
        whenever(statsRepository.fetchRevenueStats(any(), any(), any(), any()))
            .thenReturn(result)
    }

    private suspend fun getRevenueStatsById(result: Result<WCRevenueStatsModel?>) {
        whenever(statsRepository.getRevenueStatsById(any())).thenReturn(result)
    }

    private fun givenIsJetpackConnected(isJetPackConnected: Boolean) {
        whenever(selectedSite.connectionType).thenReturn(
            if (isJetPackConnected) {
                SiteConnectionType.JetpackConnectionPackage
            } else {
                SiteConnectionType.Jetpack
            }
        )
    }

    private suspend fun givenFetchVisitorStats(result: Result<Map<String, Int>>) {
        whenever(statsRepository.fetchVisitorStats(any(), any(), any()))
            .thenReturn(result)
    }

    private suspend fun givenFetchTotalVisitorStats(result: Result<Int>) {
        whenever(statsRepository.fetchTotalVisitorStats(any(), any(), any()))
            .thenReturn(result)
    }

    private fun givenShouldUpdateAnalyticsReturns(shouldUpdateAnalytics: Boolean) {
        whenever(
            analyticsUpdateDataStore.shouldUpdateAnalytics(
                rangeSelection = any(),
                analyticData = any(),
                maxOutdatedTime = any()
            )
        )
            .thenReturn(flowOf(shouldUpdateAnalytics))
    }

    private companion object {
        const val ANY_ERROR_MESSAGE = "Error message"
        val GENERIC_ORDER_STATS_ERROR = OrderStatsError(GENERIC_ERROR, ANY_ERROR_MESSAGE)
        val PLUGIN_NOT_ACTIVE_ORDER_STATS_ERROR = OrderStatsError(PLUGIN_NOT_ACTIVE, ANY_ERROR_MESSAGE)
        val ANY_SELECTION_TYPE = StatsTimeRangeSelection.SelectionType.WEEK_TO_DATE
        val ANY_STATS_RANGE_SELECTION = StatsTimeRangeSelection.build(
            selectionType = ANY_SELECTION_TYPE,
            referenceDate = Date(),
            calendar = Calendar.getInstance(),
            locale = Locale.getDefault()
        )
        val ANY_REVENUE_STATS = WCRevenueStatsModel()
        val ANY_VISITOR_STATS = mapOf(
            "2020-10-01" to 1,
            "2020-11-01" to 3,
            "2020-12-01" to 4
        )
        const val ANY_TOTAL_VISITOR_COUNT = 4
    }
}
