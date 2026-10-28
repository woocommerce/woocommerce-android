package com.woocommerce.android.ui.woopos.home.totals

import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTrackingDataKeeper
import com.woocommerce.android.ui.woopos.util.analytics.WooPosPaymentStateAnalyticsTracker
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.test.Test

class WooPosTotalsAnalyticsTrackerTest {
    private val paymentStateTracker: WooPosPaymentStateAnalyticsTracker = mock()

    private val tracker = WooPosTotalsAnalyticsTracker(
        analyticsTracker = mock<WooPosAnalyticsTracker>(),
        analyticsData = WooPosAnalyticsTrackingDataKeeper(mock()),
        productsDataSource = mock<WooPosProductsDataSource>(),
        paymentStateTracker = paymentStateTracker,
    )

    @Test
    fun `when payment states are tracked, then they are delegated to the shared tracker`() = runTest {
        // GIVEN
        val paymentState = MutableStateFlow<CardReaderPaymentOrRefundState>(CardReaderPaymentState.LoadingData {})

        // WHEN
        tracker.trackPaymentStates(paymentState)

        // THEN
        verify(paymentStateTracker).trackPaymentStates(paymentState)
    }

    @Test
    fun `when the reader asks for card input, then it is delegated to the shared tracker`() = runTest {
        // GIVEN
        val readerReadyForCardInput = MutableSharedFlow<Unit>()

        // WHEN
        tracker.trackReaderReadyForCardInput(readerReadyForCardInput)

        // THEN
        verify(paymentStateTracker).trackReaderReadyForCardInput(readerReadyForCardInput)
    }
}
