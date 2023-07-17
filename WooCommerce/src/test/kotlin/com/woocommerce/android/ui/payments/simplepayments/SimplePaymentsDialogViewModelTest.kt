package com.woocommerce.android.ui.payments.simplepayments

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.creation.OrderCreateEditRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SimplePaymentsDialogViewModelTest: BaseUnitTest() {
    private val networkStatus: NetworkStatus = mock()
    private val orderCreateEditRepository: OrderCreateEditRepository = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()

    private val viewModel = SimplePaymentsDialogViewModel(
        SavedStateHandle(),
        networkStatus,
        orderCreateEditRepository,
        analyticsTracker,
    )

    @Test
    fun `when on cancel dialog clicked, then payment flow canceled tracked`() {
        // WHEN
        viewModel.onCancelDialogClicked()

        // THEN
        verify(analyticsTracker).track(
            AnalyticsEvent.PAYMENTS_FLOW_CANCELED,
            mapOf(AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FLOW)
        )
    }

    @Test
    fun `given network available, when done clicked, then should create order`() = testBlocking {
        // given
        viewModel.viewState = viewModel.viewState.copy(currentPrice = 23.toBigDecimal())
        whenever(networkStatus.isConnected()).thenReturn(true)
        whenever(orderCreateEditRepository.createSimplePaymentOrder(viewModel.viewState.currentPrice))
            .thenReturn(Result.success(Order.EMPTY.copy(total = 23.toBigDecimal())))

        // when
        viewModel.onDoneButtonClicked()

        val latestViewState = viewModel.viewStateLiveData.liveData.value

        // then
        assertThat(latestViewState).isNotNull()
    }
}
