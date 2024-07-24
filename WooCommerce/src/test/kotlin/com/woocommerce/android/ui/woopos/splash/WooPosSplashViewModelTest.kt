package com.woocommerce.android.ui.woopos.splash

import com.woocommerce.android.ui.woopos.home.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.home.products.WooPosProductsDataSource.ProductsResult
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.Test

@ExperimentalCoroutinesApi
class WooPosSplashViewModelTest {
    private val productsDataSource: WooPosProductsDataSource = mock()

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
    fun `given products load successfully, when vm created, should update state to Loaded`() = runTest {
        // GIVEN
        whenever(productsDataSource.loadSimpleProducts(forceRefreshProducts = true)).thenReturn(
            flowOf(ProductsResult.Remote(Result.success(emptyList())))
        )

        // WHEN
        val sut = createSut()

        // THEN
        assertThat(sut.state.value).isEqualTo(WooPosSplashState.Loaded)
    }

    @Test
    fun `given products are cached, when vm created, should remain in loading state`() = runTest {
        // GIVEN
        whenever(productsDataSource.loadSimpleProducts(forceRefreshProducts = true)).thenReturn(
            flowOf(ProductsResult.Cached(emptyList()))
        )

        // WHEN
        val sut = createSut()

        // THEN
        assertThat(sut.state.value).isEqualTo(WooPosSplashState.Loading)
    }

    private fun createSut() = WooPosSplashViewModel(productsDataSource)
}
