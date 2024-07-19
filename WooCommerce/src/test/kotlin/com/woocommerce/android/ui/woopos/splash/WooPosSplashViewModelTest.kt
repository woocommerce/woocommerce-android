package com.woocommerce.android.ui.woopos.splash

import com.woocommerce.android.ui.woopos.home.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
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
    fun `given products load successfully, when vm created, should update state to Loaded`() = runTest {
        // GIVEN
        whenever(productsDataSource.loadSimpleProducts(forceRefreshProducts = true)).thenReturn(
            Result.success(Unit)
        )

        // WHEN
        val sut = createSut()

        // THEN
        assertThat(sut.state.value).isEqualTo(WooPosSplashState.Loaded)
    }

    @Test
    fun `given products load with error, when vm created, should update state to error`() = runTest {
        // GIVEN
        whenever(productsDataSource.loadSimpleProducts(forceRefreshProducts = true)).thenReturn(
            Result.failure(Exception())
        )

        // WHEN
        val sut = createSut()

        // THEN
        assertThat(sut.state.value).isEqualTo(WooPosSplashState.Error)
    }

    private fun createSut() = WooPosSplashViewModel(productsDataSource)
}
