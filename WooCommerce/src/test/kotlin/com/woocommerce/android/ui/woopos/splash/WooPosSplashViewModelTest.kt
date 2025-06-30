package com.woocommerce.android.ui.woopos.splash

import com.woocommerce.android.ui.woopos.common.data.WooPosPopularProductsProvider
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test

@ExperimentalCoroutinesApi
class WooPosSplashViewModelTest {
    private val productsDataSource: WooPosProductsDataSource = mock()
    private val analyticsTracker: WooPosAnalyticsTracker = mock()
    private val popularProductsProvider: WooPosPopularProductsProvider = mock()

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    @Test
    fun `when vm created, should be in loading state`() {
        Dispatchers.setMain(StandardTestDispatcher())

        // WHEN
        val sut = createSut()

        // THEN
        assertThat(sut.state.value).isEqualTo(WooPosSplashState.Loading)

        Dispatchers.resetMain()
    }

    @Test
    fun `when products are prepopulated, should update state to Loaded`() = runTest {
        // WHEN
        val sut = createSut()

        // THEN
        assertThat(sut.state.value).isEqualTo(WooPosSplashState.Loaded)
    }

    @Test
    fun `when products are prepopulated, should track event`() = runTest {
        // WHEN
        createSut()

        // THEN
        verify(analyticsTracker).track(any())
    }

    @Test
    fun `when products are prepopulated, should track event with timing properties`() = runTest {
        // WHEN
        createSut()

        // THEN
        verify(analyticsTracker).track(any())
    }

    @Test
    fun `when products are prepopulated, should call both product sources`() = runTest {
        // WHEN
        createSut()

        // THEN
        verify(productsDataSource).prepopulateProductsCache()
        verify(popularProductsProvider).fetchAndCachePopularProducts()
    }

    @Test
    fun `given product population fails, should still update state to Loaded`() = runTest {
        // GIVEN
        whenever(productsDataSource.prepopulateProductsCache()).thenReturn(
            Result.failure<Unit>(
                Exception("Test exception")
            )
        )

        // WHEN
        val sut = createSut()

        // THEN
        assertThat(sut.state.value).isEqualTo(WooPosSplashState.Loaded)
    }

    @Test
    fun `given popular products fetch fails, should still update state to Loaded`() = runTest {
        // GIVEN
        whenever(popularProductsProvider.fetchAndCachePopularProducts()).thenReturn(
            Result.failure<Unit>(
                Exception("Test exception")
            )
        )

        // WHEN
        val sut = createSut()

        // THEN
        assertThat(sut.state.value).isEqualTo(WooPosSplashState.Loaded)
    }

    private fun createSut() = WooPosSplashViewModel(productsDataSource, popularProductsProvider, analyticsTracker)
}
