package com.woocommerce.android.ui.woopos.home.totals

import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.ReaderReadySignal
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTrackingDataKeeper
import com.woocommerce.android.ui.woopos.util.analytics.WooPosReaderReadyForPaymentTracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosTotalsAnalyticsTrackerTest {
    private val analyticsData = WooPosAnalyticsTrackingDataKeeper(mock())
    private val readerReadyForPaymentTracker: WooPosReaderReadyForPaymentTracker = mock()

    private val tracker = WooPosTotalsAnalyticsTracker(
        analyticsTracker = mock<WooPosAnalyticsTracker>(),
        analyticsData = analyticsData,
        productsDataSource = mock<WooPosProductsDataSource>(),
        readerReadyForPaymentTracker = readerReadyForPaymentTracker,
    )

    @Test
    fun `when payment data starts loading, then a new payment attempt starts`() = runTest {
        // WHEN
        val paymentState = MutableStateFlow<CardReaderPaymentOrRefundState>(CardReaderPaymentState.LoadingData {})
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { tracker.trackPaymentStates(paymentState) }

        // THEN
        assertThat(analyticsData.paymentAttemptStartedTimestamp).isNotNull()
        job.cancel()
    }

    @Test
    fun `when an external reader starts processing, then reader ready is not tracked`() = runTest {
        // GIVEN
        val paymentState = MutableStateFlow<CardReaderPaymentOrRefundState>(CardReaderPaymentState.LoadingData {})
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { tracker.trackPaymentStates(paymentState) }

        // WHEN
        paymentState.value = CardReaderPaymentState.ProcessingPayment.ExternalReaderProcessingPayment("", {})

        // THEN
        verify(readerReadyForPaymentTracker, never()).track(any())
        job.cancel()
    }

    @Test
    fun `when Tap to Pay starts processing, then reader ready is tracked as collection started`() = runTest {
        // GIVEN
        val paymentState = MutableStateFlow<CardReaderPaymentOrRefundState>(CardReaderPaymentState.LoadingData {})
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { tracker.trackPaymentStates(paymentState) }

        // WHEN
        paymentState.value = CardReaderPaymentState.ProcessingPayment.BuiltInReaderProcessingPayment("")

        // THEN
        verify(readerReadyForPaymentTracker).track(ReaderReadySignal.COLLECTION_STARTED)
        job.cancel()
    }

    @Test
    fun `when the reader asks for card input, then reader ready is tracked as input requested`() = runTest {
        // GIVEN
        val readerReadyForCardInput = MutableSharedFlow<Unit>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            tracker.trackReaderReadyForCardInput(readerReadyForCardInput)
        }

        // WHEN
        readerReadyForCardInput.emit(Unit)

        // THEN
        verify(readerReadyForPaymentTracker).track(ReaderReadySignal.INPUT_REQUESTED)
        job.cancel()
    }
}
