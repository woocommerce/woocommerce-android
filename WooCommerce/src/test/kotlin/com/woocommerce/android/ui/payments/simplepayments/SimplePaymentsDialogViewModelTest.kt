package com.woocommerce.android.ui.payments.simplepayments

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.creation.OrderCreateEditRepository
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class SimplePaymentsDialogViewModelTest {
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
}
