package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.ReaderReadySignal
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
class WooPosPaymentStateAnalyticsTrackerTest {
    private val analyticsData = WooPosAnalyticsTrackingDataKeeper(mock())
    private val readerReadyForPaymentTracker: WooPosReaderReadyForPaymentTracker = mock()

    private val tracker = WooPosPaymentStateAnalyticsTracker(
        analyticsData = analyticsData,
        readerReadyForPaymentTracker = readerReadyForPaymentTracker,
    )

    @Test
    fun `given external reader processing, when no card is presented, then card tapped is not stamped`() = runTest {
        // GIVEN
        val paymentState = MutableStateFlow<CardReaderPaymentOrRefundState>(CardReaderPaymentState.LoadingData {})
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { tracker.trackPaymentStates(paymentState) }

        // WHEN
        paymentState.value = CardReaderPaymentState.ProcessingPayment.ExternalReaderProcessingPayment("", {})

        // THEN
        assertThat(analyticsData.cardTappedTimestamp).isNull()
        job.cancel()
    }

    @Test
    fun `given external reader processing, when the card is captured, then card tapped is stamped`() = runTest {
        // GIVEN
        val paymentState = MutableStateFlow<CardReaderPaymentOrRefundState>(CardReaderPaymentState.LoadingData {})
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { tracker.trackPaymentStates(paymentState) }
        paymentState.value = CardReaderPaymentState.ProcessingPayment.ExternalReaderProcessingPayment("", {})

        // WHEN
        paymentState.value = CardReaderPaymentState.PaymentCapturing.ExternalReaderPaymentCapturing("")

        // THEN
        assertThat(analyticsData.cardTappedTimestamp).isNotNull()
        job.cancel()
    }

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
