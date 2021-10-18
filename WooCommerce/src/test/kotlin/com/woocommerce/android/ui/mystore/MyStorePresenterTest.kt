package com.woocommerce.android.ui.mystore

import android.content.Context
import android.content.SharedPreferences
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.mystore.StatsRepository.StatsException
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsError
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsErrorType.PLUGIN_NOT_ACTIVE
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.util.concurrent.TimeUnit

class MyStorePresenterTest : BaseUnitTest() {
    private val myStoreView: MyStoreContract.View = mock()
    private val dispatcher: Dispatcher = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val statsRepository: StatsRepository = mock {
        onBlocking { it.fetchProductLeaderboards(any(), any(), any()) } doReturn Result.success(emptyList())
        onBlocking { it.fetchRevenueStats(any(), any()) } doReturn Result.success(WCRevenueStatsModel())
        onBlocking { it.fetchVisitorStats(any(), any()) } doReturn Result.success(emptyMap())
        onBlocking { it.checkIfStoreHasNoOrders() } doReturn Result.success(false)
    }

    private val networkStatus: NetworkStatus = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()

    private lateinit var presenter: MyStorePresenter

    private lateinit var actionCaptor: KArgumentCaptor<Action<*>>

    @Before
    @ExperimentalCoroutinesApi
    fun setup() {
        presenter = spy(
            MyStorePresenter(
                dispatcher = dispatcher,
                wooCommerceStore = wooCommerceStore,
                statsRepository = statsRepository,
                selectedSite = selectedSite,
                networkStatus = networkStatus,
                appPrefsWrapper = appPrefsWrapper
            )
        )

        // Use a dummy selected site
        doReturn(true).whenever(networkStatus).isConnected()
        actionCaptor = argumentCaptor()
        Dispatchers.setMain(Dispatchers.Unconfined)

        val editor = mock<SharedPreferences.Editor> { whenever(it.putBoolean(any(), any())).thenReturn(mock()) }
        val preferences = mock<SharedPreferences> { whenever(it.edit()).thenReturn(editor) }
        mock<Context> {
            whenever(it.applicationContext).thenReturn(it)
            whenever(it.getSharedPreferences(any(), any())).thenReturn(preferences)
            AppPrefs.init(it)
        }
    }

    @Test
    fun `when the stats screen loads, then fetch the revenue stats`() = testBlocking {
        whenever(statsRepository.fetchRevenueStats(any(), any())).thenReturn(
            Result.success(WCRevenueStatsModel())
        )
        presenter.takeView(myStoreView)
        presenter.loadStats(DAYS)

        verify(statsRepository).fetchRevenueStats(DAYS, false)

        verify(myStoreView).showStats(anyOrNull(), eq(DAYS))
    }

    @Test
    fun `when fetching the stats revenue fails, then show an error`() = testBlocking {
        whenever(statsRepository.fetchRevenueStats(any(), any())).thenReturn(
            Result.failure(Exception())
        )

        presenter.takeView(myStoreView)
        presenter.loadStats(DAYS)

        verify(myStoreView, times(1)).showStatsError(DAYS)
    }

    @Test
    fun `when v4 stats are not available, then show appropriate error`() = testBlocking {
        whenever(statsRepository.fetchRevenueStats(any(), any())).thenReturn(
            Result.failure(
                StatsException(
                    error = OrderStatsError(type = PLUGIN_NOT_ACTIVE)
                )
            )
        )
        presenter.takeView(myStoreView)
        presenter.loadStats(DAYS)

        verify(myStoreView).updateStatsAvailabilityError()
    }

    @Test
    fun `given there is a pending refresh, when connection is restored, then refresh my store`() {
        presenter.takeView(myStoreView)
        doReturn(true).whenever(myStoreView).isRefreshPending

        // Simulate the network connected event
        presenter.onEventMainThread(ConnectionChangeEvent(true))
        verify(myStoreView, times(1)).refreshMyStoreStats(forced = any())
    }

    @Test
    fun `given there is a pending refresh, when connection is restored, then don't refresh my store`() {
        presenter.takeView(myStoreView)
        doReturn(false).whenever(myStoreView).isRefreshPending

        // Simulate the network connected event
        presenter.onEventMainThread(ConnectionChangeEvent(true))
        verify(myStoreView, times(0)).refreshMyStoreStats(forced = any())
    }

    @Test
    fun `when network is disconnected, then ignore the event`() {
        presenter.takeView(myStoreView)

        // Simulate the network disconnected event
        presenter.onEventMainThread(ConnectionChangeEvent(false))
        verify(myStoreView, times(0)).refreshMyStoreStats(forced = any())
    }

    @Test
    fun `when loading top performers, then show their data`() = testBlocking {
        presenter.takeView(myStoreView)
        presenter.loadTopPerformersStats(DAYS, false)

        verify(statsRepository).fetchProductLeaderboards(DAYS, MyStorePresenter.NUM_TOP_PERFORMERS, false)
        verify(myStoreView).showTopPerformers(emptyList(), DAYS)
    }

    @Test
    fun `when force refreshing the top performers, then force fetch from repository`() = testBlocking {
        presenter.takeView(myStoreView)
        presenter.loadTopPerformersStats(DAYS, true)

        verify(statsRepository).fetchProductLeaderboards(DAYS, MyStorePresenter.NUM_TOP_PERFORMERS, true)
    }

    @Test
    fun `when fetching top performers fail, then show error`() = testBlocking {
        whenever(statsRepository.fetchProductLeaderboards(any(), any(), any()))
            .thenReturn(Result.failure(Exception()))

        presenter.takeView(myStoreView)
        presenter.loadTopPerformersStats(DAYS)

        verify(myStoreView).showTopPerformersError(DAYS)
    }

    @Test
    fun `when the store has no orders, then show empty view`() = testBlocking {
        whenever(statsRepository.checkIfStoreHasNoOrders()).thenReturn(Result.success(true))
        presenter.takeView(myStoreView)
        presenter.loadStats(DAYS)
        verify(myStoreView).showEmptyView(true)
    }

    @Test
    fun `when the store has orders, then hide empty view`() = testBlocking {
        whenever(statsRepository.checkIfStoreHasNoOrders()).thenReturn(Result.success(false))
        presenter.takeView(myStoreView)
        presenter.loadStats(DAYS)
        verify(myStoreView).showEmptyView(false)
    }

    @Test
    fun `when screen starts, then fetch visitor stats`() = testBlocking {
        whenever(statsRepository.fetchVisitorStats(any(), any()))
            .thenReturn(Result.success(emptyMap()))

        presenter.takeView(myStoreView)
        presenter.loadStats(DAYS)

        verify(statsRepository).fetchVisitorStats(DAYS, false)
        verify(myStoreView).showVisitorStats(any(), eq(DAYS))
    }

    @Test
    fun `give the site is using jetpack cp, when the stats are loaded, then show an empty view for visitor stats`() =
        testBlocking {
            val site = SiteModel().apply {
                setIsJetpackCPConnected(true)
            }
            whenever(selectedSite.getIfExists()).thenReturn(site)

            presenter.takeView(myStoreView)
            presenter.loadStats(DAYS)

            verify(statsRepository, never()).fetchVisitorStats(DAYS, false)
            verify(myStoreView).showEmptyVisitorStatsForJetpackCP()
        }

    @Test
    fun `when fetching visitor stats fails, then show visitor stats error`() = testBlocking {
        whenever(statsRepository.fetchVisitorStats(any(), any()))
            .thenReturn(Result.failure(Exception()))

        presenter.takeView(myStoreView)
        presenter.loadStats(DAYS)

        verify(myStoreView).showVisitorStatsError(DAYS)
    }

    @Test
    fun `when force fetching stats, then show skeleton`() {
        presenter.takeView(myStoreView)
        presenter.loadStats(DAYS, forced = true)
        verify(myStoreView, times(1)).showChartSkeleton(true)
    }

    @Test
    fun `when data loads, then hide skeleton`() = testBlocking {
        presenter.takeView(myStoreView)
        presenter.loadStats(DAYS, forced = true)

        verify(myStoreView).showChartSkeleton(false)
    }

    @Test
    fun `when force refreshing top performers, then show skeleton`() = testBlocking {
        presenter.takeView(myStoreView)
        presenter.loadTopPerformersStats(DAYS, forced = true)
        verify(myStoreView).showTopPerformersSkeleton(true)
    }

    @Test
    fun `when top performers data loads, then hide skeleton`() = testBlocking {
        presenter.takeView(myStoreView)
        presenter.loadTopPerformersStats(DAYS, forced = true)
        verify(myStoreView).showTopPerformersSkeleton(false)
    }

    @Test
    fun `given jetpack cp and the banner not dismissed, when the screen loads, then show the banner`() {
        whenever(appPrefsWrapper.getJetpackBenefitsDismissalDate()).thenReturn(0L)
        val site = SiteModel().apply {
            setIsJetpackCPConnected(true)
        }
        whenever(selectedSite.getIfExists()).thenReturn(site)

        presenter.takeView(myStoreView)

        verify(myStoreView).showJetpackBenefitsBanner(true)
    }

    @Test
    fun `given jetpack cp and the banner dismissed recently, when the screen loads, then don't show the banner`() {
        val nowPlus2Days = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2)
        whenever(appPrefsWrapper.getJetpackBenefitsDismissalDate()).thenReturn(nowPlus2Days)
        val site = SiteModel().apply {
            setIsJetpackCPConnected(true)
        }
        whenever(selectedSite.getIfExists()).thenReturn(site)

        presenter.takeView(myStoreView)

        verify(myStoreView).showJetpackBenefitsBanner(false)
    }

    @Test
    fun `given jetpack cp and the banner dismissed 5 days ago, when the screen loads, then show the banner`() {
        val nowPlus5Days = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(5)
        whenever(appPrefsWrapper.getJetpackBenefitsDismissalDate()).thenReturn(nowPlus5Days)
        val site = SiteModel().apply {
            setIsJetpackCPConnected(true)
        }
        whenever(selectedSite.getIfExists()).thenReturn(site)

        presenter.takeView(myStoreView)

        verify(myStoreView).showJetpackBenefitsBanner(false)
    }

    @Test
    fun `given the site is not using jetpack cp, when the screen loads, then don't show the benefits banner`() {
        val site = SiteModel().apply {
            setIsJetpackCPConnected(false)
        }
        whenever(selectedSite.getIfExists()).thenReturn(site)

        presenter.takeView(myStoreView)

        verify(myStoreView).showJetpackBenefitsBanner(false)
    }
}
