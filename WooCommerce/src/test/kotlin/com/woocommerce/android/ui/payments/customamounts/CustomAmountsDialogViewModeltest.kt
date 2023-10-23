package com.woocommerce.android.ui.payments.customamounts

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.creation.OrderCreateEditRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import java.math.BigDecimal
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CustomAmountsDialogViewModeltest : BaseUnitTest() {
    private val networkStatus: NetworkStatus = mock()
    private val orderCreateEditRepository: OrderCreateEditRepository = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()

    private val viewModel = CustomAmountsDialogViewModel(
        SavedStateHandle(),
        networkStatus,
        orderCreateEditRepository,
        analyticsTracker,
    )

    @Test
    fun `when view model is initialised, then done button is not enabled`() {
        assertFalse(viewModel.viewState.isDoneButtonEnabled)
    }

    @Test
    fun `when custom amount is zero, then done button is not enabled`() {
        viewModel.currentPrice = BigDecimal.ZERO
        assertFalse(viewModel.viewState.isDoneButtonEnabled)
    }

    @Test
    fun `when custom amount is not zero, then done button is enabled`() {
        viewModel.currentPrice = BigDecimal.TEN
        assertTrue(viewModel.viewState.isDoneButtonEnabled)
    }
}
