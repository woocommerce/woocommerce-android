package com.woocommerce.android.ui.mystore

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.mystore.domain.GetStats
import com.woocommerce.android.ui.mystore.domain.GetTopPerformers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.store.WooCommerceStore
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class MyStoreViewModelTest : BaseUnitTest() {
    private val savedState: SavedStateHandle = mock()
    private val networkStatus: NetworkStatus = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val getStats: GetStats = mock()
    private val getTopPerformers: GetTopPerformers = mock()
    private val currencyFormatter: CurrencyFormatter = mock()
    private val selectedSite: SelectedSite = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()

    private lateinit var sut: MyStoreViewModel

    @Before
    fun setup() = testBlocking {
        givenStatsLoadingResult(GetStats.LoadStatsResult.VisitorsStatsError)
        givenToPerformersResult(GetTopPerformers.TopPerformersResult.TopPerformersError)
    }

    @Test
    fun `Given there is network connectivity, When view model is created, stats are fetched`() =
        testBlocking {
            givenNetworkConnectivity(connected = true)

            whenViewModelIsCreated()

            verify(getStats).invoke(refresh = true, DEFAULT_STATS_GRANULARITY)
            verify(getTopPerformers).invoke(forceRefresh = true, DEFAULT_STATS_GRANULARITY, ANY_TOP_PERFORMERS_COUNT)
        }

    @Test
    fun `Given there is no network, When view model is created, stats are marked as refresh pending`() =
        testBlocking {
            givenNetworkConnectivity(connected = false)

            whenViewModelIsCreated()

            assertTrue(sut.refreshStoreStats[DEFAULT_STATS_GRANULARITY.ordinal])
            assertTrue(sut.refreshTopPerformerStats[DEFAULT_STATS_GRANULARITY.ordinal])
        }

    @Test
    fun `Given there is no network, When view model is created, stats are not fetched from API`() =
        testBlocking {
            givenNetworkConnectivity(connected = false)

            whenViewModelIsCreated()

            verify(getStats, never()).invoke(any(), any())
            verify(getTopPerformers, never()).invoke(any(), any(), any())
        }

    @Test
    fun `Given there is no network, When granularity changed, stats are marked as refresh pending`() =
        testBlocking {
            whenViewModelIsCreated()
            givenNetworkConnectivity(connected = false)

            sut.onStatsGranularityChanged(ANY_SELECTED_STATS_GRANULARITY)

            assertTrue(sut.refreshStoreStats[ANY_SELECTED_STATS_GRANULARITY.ordinal])
            assertTrue(sut.refreshTopPerformerStats[ANY_SELECTED_STATS_GRANULARITY.ordinal])
        }

    @Test
    fun `Given there is no network, When granularity changed, stats are not fetched from API`() =
        testBlocking {
            whenViewModelIsCreated()
            givenNetworkConnectivity(connected = false)

            sut.onStatsGranularityChanged(ANY_SELECTED_STATS_GRANULARITY)

            verify(getStats, never()).invoke(any(), any())
            verify(getTopPerformers, never()).invoke(any(), any(), any())
        }

    @Test
    fun `Given cached stats, When stats granularity changes, Then load stats for given granularity from cache`() =
        testBlocking {
            whenViewModelIsCreated()
            givenNetworkConnectivity(connected = true)
            givenStatsForGranularityCached(ANY_SELECTED_STATS_GRANULARITY)

            sut.onStatsGranularityChanged(ANY_SELECTED_STATS_GRANULARITY)

            verify(getStats).invoke(refresh = false, ANY_SELECTED_STATS_GRANULARITY)
            verify(getTopPerformers).invoke(
                forceRefresh = false,
                ANY_SELECTED_STATS_GRANULARITY,
                ANY_TOP_PERFORMERS_COUNT
            )
        }

    @Test
    fun `Given no cached stats, When stats granularity changes, Then load stats forcing refresh from API`() =
        testBlocking {
            whenViewModelIsCreated()
            givenNetworkConnectivity(connected = true)
            givenStatsForGranularityNotCached(ANY_SELECTED_STATS_GRANULARITY)

            sut.onStatsGranularityChanged(ANY_SELECTED_STATS_GRANULARITY)

            verify(getStats).invoke(refresh = true, ANY_SELECTED_STATS_GRANULARITY)
            verify(getTopPerformers).invoke(
                forceRefresh = true,
                ANY_SELECTED_STATS_GRANULARITY,
                ANY_TOP_PERFORMERS_COUNT
            )
        }

    @Test
    fun `Given network connection, When on swipe to refresh, Then stats are refreshed for selected granularity`() =
        testBlocking {
            whenViewModelIsCreated()
            givenNetworkConnectivity(connected = true)

            sut.onSwipeToRefresh()

            verify(getStats).invoke(refresh = true, DEFAULT_STATS_GRANULARITY)
            verify(getTopPerformers).invoke(
                forceRefresh = true,
                DEFAULT_STATS_GRANULARITY,
                ANY_TOP_PERFORMERS_COUNT
            )
        }

    @Test
    fun `Given success loading revenue, When stats granularity changes, Then UI is updated with revenue stats`() =
        testBlocking {
            whenViewModelIsCreated()
            givenNetworkConnectivity(connected = true)
            givenStatsLoadingResult(GetStats.LoadStatsResult.RevenueStatsSuccess(null))

            sut.onStatsGranularityChanged(ANY_SELECTED_STATS_GRANULARITY)

            assertThat(sut.revenueStatsState.value).isEqualTo(
                MyStoreViewModel.RevenueStatsViewState.Content(
                    null,
                    ANY_SELECTED_STATS_GRANULARITY
                )
            )
        }

    @Test
    fun `Given error loading revenue, When stats granularity changes, Then UI is updated with error`() =
        testBlocking {
            whenViewModelIsCreated()
            givenNetworkConnectivity(connected = true)
            givenStatsLoadingResult(GetStats.LoadStatsResult.RevenueStatsError)

            sut.onStatsGranularityChanged(ANY_SELECTED_STATS_GRANULARITY)

            assertThat(sut.revenueStatsState.value).isEqualTo(
                MyStoreViewModel.RevenueStatsViewState.GenericError
            )
        }

    @Test
    fun `Given jetpack plugin not active, When stats granularity changes, Then UI is updated with jetpack error`() =
        testBlocking {
            whenViewModelIsCreated()
            givenNetworkConnectivity(connected = true)
            givenStatsLoadingResult(GetStats.LoadStatsResult.PluginNotActive)

            sut.onStatsGranularityChanged(ANY_SELECTED_STATS_GRANULARITY)

            assertThat(sut.revenueStatsState.value).isEqualTo(
                MyStoreViewModel.RevenueStatsViewState.PluginNotActiveError
            )
        }

    @Test
    fun `Given success loading visitor stats, When stats granularity changes, Then UI is updated with visitor stats`() =
        testBlocking {
            whenViewModelIsCreated()
            givenNetworkConnectivity(connected = true)
            givenStatsLoadingResult(GetStats.LoadStatsResult.VisitorsStatsSuccess(emptyMap()))

            sut.onStatsGranularityChanged(ANY_SELECTED_STATS_GRANULARITY)

            assertThat(sut.visitorStatsState.value).isEqualTo(
                MyStoreViewModel.VisitorStatsViewState.Content(emptyMap())
            )
        }

    @Test
    fun `Given error loading visitor stats, When stats granularity changes, Then UI is updated with error`() =
        testBlocking {
            whenViewModelIsCreated()
            givenNetworkConnectivity(connected = true)
            givenStatsLoadingResult(GetStats.LoadStatsResult.VisitorsStatsError)

            sut.onStatsGranularityChanged(ANY_SELECTED_STATS_GRANULARITY)

            assertThat(sut.visitorStatsState.value).isEqualTo(
                MyStoreViewModel.VisitorStatsViewState.Error
            )
        }

    @Test
    fun `Given jetpack CP connected, When stats granularity changes, Then show jetpack CP connected state`() =
        testBlocking {
            whenViewModelIsCreated()
            givenNetworkConnectivity(connected = true)
            givenStatsLoadingResult(GetStats.LoadStatsResult.IsJetPackCPEnabled)

            sut.onStatsGranularityChanged(ANY_SELECTED_STATS_GRANULARITY)

            assertThat(sut.visitorStatsState.value)
                .isInstanceOf(MyStoreViewModel.VisitorStatsViewState.JetpackCpConnected::class.java)
        }

    @Test
    fun `Given store has orders, When stats granularity changes, Then UI is updated with has orders state`() =
        testBlocking {
            whenViewModelIsCreated()
            givenNetworkConnectivity(connected = true)
            givenStatsLoadingResult(GetStats.LoadStatsResult.HasOrders(hasOrder = true))

            sut.onStatsGranularityChanged(ANY_SELECTED_STATS_GRANULARITY)

            assertThat(sut.hasOrders.value).isEqualTo(
                MyStoreViewModel.OrderState.AtLeastOne
            )
        }

    @Test
    fun `Given store has no orders, When stats granularity changes, Then UI is updated with no orders state`() =
        testBlocking {
            whenViewModelIsCreated()
            givenNetworkConnectivity(connected = true)
            givenStatsLoadingResult(GetStats.LoadStatsResult.HasOrders(hasOrder = false))

            sut.onStatsGranularityChanged(ANY_SELECTED_STATS_GRANULARITY)

            assertThat(sut.hasOrders.value).isEqualTo(
                MyStoreViewModel.OrderState.Empty
            )
        }

    @Test
    fun `Given top performers load success, When stats granularity changes, Then UI is updated with top performers`() =
        testBlocking {
            whenViewModelIsCreated()
            givenNetworkConnectivity(connected = true)
            givenToPerformersResult(GetTopPerformers.TopPerformersResult.TopPerformersSuccess(emptyList()))

            sut.onStatsGranularityChanged(ANY_SELECTED_STATS_GRANULARITY)

            assertThat(sut.topPerformersState.value).isEqualTo(
                MyStoreViewModel.TopPerformersViewState.Content(emptyList(), ANY_SELECTED_STATS_GRANULARITY)
            )
        }

    @Test
    fun `Given top performers error, When stats granularity changes, Then UI is updated with top performers error`() =
        testBlocking {
            whenViewModelIsCreated()
            givenNetworkConnectivity(connected = true)
            givenToPerformersResult(GetTopPerformers.TopPerformersResult.TopPerformersError)

            sut.onStatsGranularityChanged(ANY_SELECTED_STATS_GRANULARITY)

            assertThat(sut.topPerformersState.value).isEqualTo(
                MyStoreViewModel.TopPerformersViewState.Error
            )
        }

    @Test
    fun `Given successful Jetpack installation, When user returns to My Store, Then UI is updated with no JP banner`() =
        testBlocking {
            val siteBeforeInstallation = SiteModel().apply { setIsJetpackCPConnected(true) }
            val siteAfterInstallation = SiteModel().apply { setIsJetpackConnected(true) }

            val siteFlow = MutableStateFlow(siteBeforeInstallation)
            whenever(selectedSite.observe()).thenReturn(siteFlow)
            givenNetworkConnectivity(connected = true)
            givenStatsLoadingResult(GetStats.LoadStatsResult.IsJetPackCPEnabled)

            whenViewModelIsCreated()

            givenStatsLoadingResult(GetStats.LoadStatsResult.VisitorsStatsSuccess(emptyMap()))
            siteFlow.value = siteAfterInstallation

            assertThat(sut.visitorStatsState.value).isNotInstanceOf(
                MyStoreViewModel.VisitorStatsViewState.JetpackCpConnected::class.java
            )
        }

    private suspend fun givenStatsLoadingResult(result: GetStats.LoadStatsResult) {
        whenever(getStats.invoke(any(), any())).thenReturn(flow { emit(result) })
    }

    private suspend fun givenToPerformersResult(result: GetTopPerformers.TopPerformersResult) {
        whenever(getTopPerformers.invoke(any(), any(), any())).thenReturn(flow { emit(result) })
    }

    private fun givenStatsForGranularityCached(granularity: StatsGranularity) {
        sut.refreshStoreStats[granularity.ordinal] = false
        sut.refreshTopPerformerStats[granularity.ordinal] = false
    }

    private fun givenStatsForGranularityNotCached(granularity: StatsGranularity) {
        sut.refreshStoreStats[granularity.ordinal] = true
        sut.refreshTopPerformerStats[granularity.ordinal] = true
    }

    private fun whenViewModelIsCreated() {
        sut = MyStoreViewModel(
            savedState,
            networkStatus,
            resourceProvider,
            wooCommerceStore,
            getStats,
            getTopPerformers,
            currencyFormatter,
            selectedSite,
            appPrefsWrapper
        )
    }

    private fun givenNetworkConnectivity(connected: Boolean) {
        whenever(networkStatus.isConnected()).thenReturn(connected)
    }

    private companion object {
        val DEFAULT_STATS_GRANULARITY = StatsGranularity.DAYS
        val ANY_SELECTED_STATS_GRANULARITY = StatsGranularity.WEEKS
        const val ANY_TOP_PERFORMERS_COUNT = 5
    }
}
