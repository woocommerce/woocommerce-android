package com.woocommerce.android.ui.woopos.splash

import com.woocommerce.android.ui.woopos.common.data.WooPosPopularProductsProvider
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource.WooPosPrepopulatingDataStatus
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersDataSource
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersInMemoryCache
import com.woocommerce.android.ui.woopos.tab.WooPosCanBeLaunchedInTab
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test

@ExperimentalCoroutinesApi
class WooPosSplashViewModelTest {
    private val productsDataSource: WooPosProductsDataSource = mock()
    private val ordersCache: WooPosOrdersInMemoryCache = mock()
    private val ordersDataSource: WooPosOrdersDataSource = mock()
    private val analyticsTracker: WooPosAnalyticsTracker = mock()
    private val popularProductsProvider: WooPosPopularProductsProvider = mock()
    private val posCanBeLaunchedInTab: WooPosCanBeLaunchedInTab = mock()
    private val preferencesRepository: com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository =
        mock()

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    @Before
    fun setup() = runTest {
        whenever(posCanBeLaunchedInTab()).thenReturn(WooPosLaunchability.Launchable)
        whenever(productsDataSource.prepopulateCache()).thenReturn(
            flowOf(WooPosPrepopulatingDataStatus.Syncing, WooPosPrepopulatingDataStatus.Completed)
        )
        whenever(productsDataSource.prepopulateCache()).thenReturn(
            flowOf(WooPosPrepopulatingDataStatus.Completed)
        )
        whenever(productsDataSource.getCurrentSyncStrategy())
            .thenReturn(WooPosProductsDataSource.SyncStrategy.LOCAL_CATALOG_FILE)
        whenever(ordersDataSource.loadOrders()).thenReturn(emptyFlow())
    }

    @Test
    fun `given eligible site and sync in progress, when vm created, then state is Syncing`() = runTest {
        // GIVEN
        whenever(posCanBeLaunchedInTab()).thenReturn(WooPosLaunchability.Launchable)
        whenever(productsDataSource.prepopulateCache()).thenReturn(
            flowOf(WooPosPrepopulatingDataStatus.Syncing)
        )

        // WHEN
        val sut = createSut()

        // THEN
        assertThat(sut.state.value).isEqualTo(WooPosSplashState.Syncing)
    }

    @Test
    fun `given site not eligible, when vm created, then state is NotEligible`() = runTest {
        // GIVEN
        whenever(posCanBeLaunchedInTab()).thenReturn(
            WooPosLaunchability.NotLaunchable(WooPosLaunchability.NonLaunchabilityReason.UnsupportedCurrency)
        )
        whenever(productsDataSource.prepopulateCache()).thenReturn(
            flowOf(WooPosPrepopulatingDataStatus.Completed)
        )

        // WHEN
        val sut = createSut()

        // THEN
        assertThat(sut.state.value).isEqualTo(
            WooPosSplashState.NotEligible(WooPosLaunchability.NonLaunchabilityReason.UnsupportedCurrency)
        )
    }

    @Test
    fun `given sync completes, when vm created, then state is Loaded`() = runTest {
        // GIVEN
        whenever(productsDataSource.prepopulateCache()).thenReturn(
            flowOf(WooPosPrepopulatingDataStatus.Syncing, WooPosPrepopulatingDataStatus.Completed)
        )

        // WHEN
        val sut = createSut()

        // THEN
        assertThat(sut.state.value).isEqualTo(WooPosSplashState.Loaded)
    }

    @Test
    fun `given products prepopulation completes, when vm created, then state is Loaded`() = runTest {
        // GIVEN
        whenever(productsDataSource.prepopulateCache()).thenReturn(
            flowOf(WooPosPrepopulatingDataStatus.Completed)
        )

        // WHEN
        val sut = createSut()

        // THEN
        assertThat(sut.state.value).isEqualTo(WooPosSplashState.Loaded)
    }

    @Test
    fun `given products prepopulation completes, when vm created, then tracks event`() = runTest {
        // GIVEN
        whenever(productsDataSource.prepopulateCache()).thenReturn(
            flowOf(WooPosPrepopulatingDataStatus.Completed)
        )

        // WHEN
        createSut()

        // THEN
        verify(analyticsTracker).track(any())
    }

    @Test
    fun `when vm created, then calls both product sources`() = runTest {
        // WHEN
        createSut()

        // THEN
        verify(productsDataSource).prepopulateCache()
        verify(popularProductsProvider).fetchAndCachePopularProducts()
    }

    @Test
    fun `when vm created, then clears orders cache`() = runTest {
        // WHEN
        createSut()

        // THEN
        verify(ordersCache).clear()
    }

    @Test
    fun `given product prepopulation fails, when vm created, then state is SyncFailed`() = runTest {
        // GIVEN
        whenever(productsDataSource.prepopulateCache()).thenReturn(
            flowOf(WooPosPrepopulatingDataStatus.Failed("Test error"))
        )

        // WHEN
        val sut = createSut()

        // THEN
        assertThat(sut.state.value).isEqualTo(WooPosSplashState.SyncFailed("Test error"))
    }

    @Test
    fun `given popular products fetch fails, when vm created, then state is Loaded`() = runTest {
        // GIVEN
        whenever(popularProductsProvider.fetchAndCachePopularProducts()).thenReturn(
            Result.failure(
                Exception("Test exception")
            )
        )

        // WHEN
        val sut = createSut()

        // THEN
        assertThat(sut.state.value).isEqualTo(WooPosSplashState.Loaded)
    }

    @Test
    fun `when retry sync succeeds, then state is Loaded`() = runTest {
        // GIVEN
        whenever(productsDataSource.prepopulateCache()).thenReturn(
            flowOf(WooPosPrepopulatingDataStatus.Failed(""))
        )
        val sut = createSut()

        whenever(productsDataSource.prepopulateCache()).thenReturn(
            flowOf(WooPosPrepopulatingDataStatus.Completed)
        )

        // WHEN
        sut.onRetrySync()

        // THEN
        assertThat(sut.state.value).isEqualTo(WooPosSplashState.Loaded)
    }

    @Test
    fun `when retry sync fails, then state is SyncFailed`() = runTest {
        // GIVEN
        whenever(productsDataSource.prepopulateCache()).thenReturn(
            flowOf(WooPosPrepopulatingDataStatus.Failed(""))
        )
        val sut = createSut()

        whenever(productsDataSource.prepopulateCache()).thenReturn(
            flowOf(WooPosPrepopulatingDataStatus.Failed("Network error"))
        )

        // WHEN
        sut.onRetrySync()

        // THEN
        assertThat(sut.state.value).isEqualTo(WooPosSplashState.SyncFailed("Network error"))
    }

    @Test
    fun `given local catalog enabled and sync required, when sync starts, then tracks LocalCatalogDownloadingScreenShown`() = runTest {
        // GIVEN
        whenever(productsDataSource.prepopulateCache()).thenReturn(
            flowOf(WooPosPrepopulatingDataStatus.Syncing)
        )

        // WHEN
        createSut()

        // THEN
        verify(analyticsTracker).track(WooPosAnalyticsEvent.Event.LocalCatalogDownloadingScreenShown)
    }

    @Test
    fun `given local catalog sync fails, when vm created, then tracks SplashScreenErrorShown`() = runTest {
        // GIVEN
        whenever(productsDataSource.prepopulateCache()).thenReturn(
            flowOf(WooPosPrepopulatingDataStatus.Syncing, WooPosPrepopulatingDataStatus.Failed("Sync error"))
        )

        // WHEN
        createSut()

        // THEN
        verify(analyticsTracker).track(WooPosAnalyticsEvent.Event.SplashScreenErrorShown)
    }

    @Test
    fun `when retry sync is clicked, then tracks SplashScreenRetryTapped`() = runTest {
        // GIVEN
        whenever(productsDataSource.prepopulateCache()).thenReturn(
            flowOf(WooPosPrepopulatingDataStatus.Failed(""))
        )
        val sut = createSut()

        whenever(productsDataSource.prepopulateCache()).thenReturn(
            flowOf(WooPosPrepopulatingDataStatus.Completed)
        )

        // WHEN
        sut.onRetrySync()

        // THEN
        verify(analyticsTracker).track(WooPosAnalyticsEvent.Event.SplashScreenRetryTapped)
    }

    @Test
    fun `given state is Syncing, when exit POS is clicked, then tracks LocalCatalogDownloadingScreenExitPosTapped`() = runTest {
        // GIVEN
        whenever(productsDataSource.prepopulateCache()).thenReturn(
            flowOf(WooPosPrepopulatingDataStatus.Syncing)
        )
        val sut = createSut()
        assertThat(sut.state.value).isEqualTo(WooPosSplashState.Syncing)

        // WHEN
        sut.onExitPosClicked()

        // THEN
        verify(analyticsTracker).track(WooPosAnalyticsEvent.Event.LocalCatalogDownloadingScreenExitPosTapped)
    }

    private fun createSut(): WooPosSplashViewModel {
        return WooPosSplashViewModel(
            productsDataSource,
            popularProductsProvider,
            analyticsTracker,
            posCanBeLaunchedInTab,
            ordersCache,
            ordersDataSource,
            preferencesRepository,
            CoroutineScope(coroutinesTestRule.testDispatcher),
        )
    }
}
