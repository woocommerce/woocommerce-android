package com.woocommerce.android.ui.woopos.splash

import com.woocommerce.android.ui.woopos.common.data.WooPosPopularProductsProvider
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource.WooPosPrepopulatingDataStatus
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersInMemoryCache
import com.woocommerce.android.ui.woopos.tab.WooPosCanBeLaunchedInTab
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
            .thenReturn(WooPosAnalyticsEventConstant.SyncStrategy.LOCAL_CATALOG)
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

    private fun createSut(): WooPosSplashViewModel {
        return WooPosSplashViewModel(
            productsDataSource,
            popularProductsProvider,
            analyticsTracker,
            posCanBeLaunchedInTab,
            ordersCache,
            preferencesRepository,
        )
    }
}
