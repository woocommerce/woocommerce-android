package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.mock
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosPaymentStateAnalyticsTrackerTest {
    private val analyticsData = WooPosAnalyticsTrackingDataKeeper()
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
}
