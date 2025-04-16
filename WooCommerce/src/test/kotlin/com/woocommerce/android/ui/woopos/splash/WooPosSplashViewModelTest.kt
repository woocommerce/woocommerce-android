package com.woocommerce.android.ui.woopos.splash

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
import kotlin.test.Test

@ExperimentalCoroutinesApi
class WooPosSplashViewModelTest {
    private val productsDataSource: WooPosProductsDataSource = mock()
    private val analyticsTracker: WooPosAnalyticsTracker = mock()

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

    private fun createSut() = WooPosSplashViewModel(productsDataSource, analyticsTracker)
}
