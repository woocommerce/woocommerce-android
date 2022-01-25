package com.woocommerce.android.ui.mystore.domain

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.mystore.data.StatsRepository
import com.woocommerce.android.ui.mystore.data.StatsRepository.StatsException
import com.woocommerce.android.ui.mystore.domain.GetStats.LoadStatsResult.*
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsError
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsErrorType.PLUGIN_NOT_ACTIVE

@ExperimentalCoroutinesApi
class GetStatsTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()
    private val statsRepository: StatsRepository = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()

    private val getStats = GetStats(
        selectedSite,
        statsRepository,
        appPrefsWrapper,
        coroutinesTestRule.testDispatchers
    )

    @Before
    fun setup() = testBlocking {
        givenCheckIfStoreHasNoOrdersFlow(Result.success(true))
        givenIsJetpackConnected(false)
        givenFetchRevenueStats(Result.success(ANY_REVENUE_STATS))
        givenFetchVisitorStats(Result.success(ANY_VISITOR_STATS))
    }

    @Test
    fun `Given it has no orders, when get stats, then emits HasOrders to false`() =
        testBlocking {
            givenCheckIfStoreHasNoOrdersFlow(Result.success(true))

            val result = getStats(refresh = false, granularity = ANY_GRANULARITY)
                .filter { it is HasOrders }
                .first()

            assertThat(result).isEqualTo(HasOrders(false))
        }

    @Test
    fun `Given it has some orders, When get stats, then emits HasOrders to true`() =
        testBlocking {
            givenCheckIfStoreHasNoOrdersFlow(Result.success(false))

            val result = getStats(refresh = false, granularity = ANY_GRANULARITY)
                .filter { it is HasOrders }
                .first()

            assertThat(result).isEqualTo(HasOrders(true))
        }

    @Test
    fun `Given revenue stats success, when get stats, then emits revenue stats`() =
        testBlocking {
            givenFetchRevenueStats(Result.success(ANY_REVENUE_STATS))

            val result = getStats(refresh = false, granularity = ANY_GRANULARITY)
                .filter { it is RevenueStatsSuccess }
                .first()

            assertThat(result).isEqualTo(RevenueStatsSuccess(ANY_REVENUE_STATS))
        }

    @Test
    fun `Given revenue stats error is not plugin not active error, when get stats, then emits revenue stats`() =
        testBlocking {
            givenFetchRevenueStats(Result.failure(StatsException(GENERIC_ORDER_STATS_ERROR)))

            val result = getStats(refresh = false, granularity = ANY_GRANULARITY)
                .filter { it is RevenueStatsError }
                .first()

            assertThat(result).isEqualTo(RevenueStatsError)
        }

    @Test
    fun `Given revenue stats error is plugin not active error, when get stats, then emits revenue stats`() =
        testBlocking {
            givenFetchRevenueStats(Result.failure(StatsException(PLUGIN_NOT_ACTIVE_ORDER_STATS_ERROR)))

            val result = getStats(refresh = false, granularity = ANY_GRANULARITY)
                .filter { it is PluginNotActive }
                .first()

            assertThat(result).isEqualTo(PluginNotActive)
        }

    @Test
    fun `Given revenue stats success, when get stats, save stats supported in prefs`() =
        testBlocking {
            givenFetchRevenueStats(Result.success(ANY_REVENUE_STATS))

            getStats(refresh = false, granularity = ANY_GRANULARITY).collect()

            verify(appPrefsWrapper).setV4StatsSupported(true)
        }

    @Test
    fun `Given revenue stats error is plugin not active error, when get stats, save stats not supported in prefs`() =
        testBlocking {
            givenFetchRevenueStats(Result.failure(StatsException(PLUGIN_NOT_ACTIVE_ORDER_STATS_ERROR)))

            getStats(refresh = false, granularity = ANY_GRANULARITY).collect()

            verify(appPrefsWrapper).setV4StatsSupported(false)
        }

    @Test
    fun `Given visitor stats success, when get stats, then emits visitor stats`() =
        testBlocking {
            givenFetchVisitorStats(Result.success(ANY_VISITOR_STATS))

            val result = getStats(refresh = false, granularity = ANY_GRANULARITY)
                .filter { it is VisitorsStatsSuccess }
                .first()

            assertThat(result).isEqualTo(VisitorsStatsSuccess(ANY_VISITOR_STATS))
        }

    @Test
    fun `Given visitor stats error, when get stats, then emits visitor stats`() =
        testBlocking {
            givenFetchVisitorStats(Result.failure(StatsException(GENERIC_ORDER_STATS_ERROR)))

            val result = getStats(refresh = false, granularity = ANY_GRANULARITY)
                .filter { it is VisitorsStatsError }
                .first()

            assertThat(result).isEqualTo(VisitorsStatsError)
        }

    @Test
    fun `Given jetpack CP is connected, when get stats, then emits is jetpack connected`() =
        testBlocking {
            givenIsJetpackConnected(true)

            val result = getStats(refresh = false, granularity = ANY_GRANULARITY)
                .filter { it is IsJetPackCPEnabled }
                .first()

            assertThat(result).isEqualTo(IsJetPackCPEnabled)
        }

    private suspend fun givenCheckIfStoreHasNoOrdersFlow(result: Result<Boolean>) {
        whenever(
            statsRepository.checkIfStoreHasNoOrders()
        ).thenReturn(flow { emit(result) })
    }

    private suspend fun givenFetchRevenueStats(result: Result<WCRevenueStatsModel?>) {
        whenever(statsRepository.fetchRevenueStats(any(), anyBoolean()))
            .thenReturn(flow { emit(result) })
    }

    private fun givenIsJetpackConnected(isJetPackConnected: Boolean) {
        val siteModel = SiteModel()
        siteModel.setIsJetpackCPConnected(isJetPackConnected)
        whenever(selectedSite.getIfExists()).thenReturn(siteModel)
    }

    private suspend fun givenFetchVisitorStats(result: Result<Map<String, Int>>) {
        whenever(statsRepository.fetchVisitorStats(any(), anyBoolean()))
            .thenReturn(flow { emit(result) })
    }

    private companion object {
        const val ANY_ERROR_MESSAGE = "Error message"
        val GENERIC_ORDER_STATS_ERROR = OrderStatsError(GENERIC_ERROR, ANY_ERROR_MESSAGE)
        val PLUGIN_NOT_ACTIVE_ORDER_STATS_ERROR = OrderStatsError(PLUGIN_NOT_ACTIVE, ANY_ERROR_MESSAGE)
        val ANY_GRANULARITY = WCStatsStore.StatsGranularity.DAYS
        val ANY_REVENUE_STATS = WCRevenueStatsModel()
        val ANY_VISITOR_STATS = mapOf(
            "2020-10-01" to 1,
            "2020-11-01" to 3,
            "2020-12-01" to 4
        )
    }
}
