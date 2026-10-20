package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.ReaderReadySignal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class WooPosPaymentStateAnalyticsTracker @Inject constructor(
    private val analyticsData: WooPosAnalyticsTrackingDataKeeper,
    private val readerReadyForPaymentTracker: WooPosReaderReadyForPaymentTracker,
) {
    suspend fun trackPaymentStates(paymentState: StateFlow<CardReaderPaymentOrRefundState>?) {
        paymentState?.distinctUntilChanged { old, new -> old::class == new::class }?.collect {
            when (it) {
                is CardReaderPaymentState.LoadingData -> analyticsData.startPaymentAttempt(System.currentTimeMillis())

                is CardReaderPaymentState.ProcessingPayment.BuiltInReaderProcessingPayment ->
                    readerReadyForPaymentTracker.track(ReaderReadySignal.COLLECTION_STARTED)

                is CardReaderPaymentState.ProcessingPayment.ExternalReaderProcessingPayment,

                is CardReaderPaymentState.PaymentCapturing -> {
                    analyticsData.cardTappedTimestamp = System.currentTimeMillis()
                }

                is CardReaderPaymentOrRefundState.CardReaderInteracRefundState.CollectingInteracRefund,
                is CardReaderPaymentOrRefundState.CardReaderInteracRefundState.InteracRefundFailure.Cancelable,
                is CardReaderPaymentOrRefundState.CardReaderInteracRefundState.InteracRefundFailure.NonCancelable,
                is CardReaderPaymentOrRefundState.CardReaderInteracRefundState.InteracRefundSuccessful,
                is CardReaderPaymentOrRefundState.CardReaderInteracRefundState.LoadingData,
                is CardReaderPaymentOrRefundState.CardReaderInteracRefundState.ProcessingInteracRefund,
                is CardReaderPaymentState.PaymentFailed.BuiltInReaderFailedPayment.Cancelable,
                is CardReaderPaymentState.PaymentFailed.BuiltInReaderFailedPayment.NonCancelable,
                is CardReaderPaymentState.PaymentFailed.ExternalReaderFailedPayment.Cancelable,
                is CardReaderPaymentState.PaymentFailed.ExternalReaderFailedPayment.NonCancelable,
                is CardReaderPaymentState.PaymentSuccessful.BuiltInReaderPaymentSuccessful,
                is CardReaderPaymentState.PaymentSuccessful.BuiltInReaderPaymentSuccessfulReceiptSentAutomatically,
                is CardReaderPaymentState.PaymentSuccessful.ExternalReaderPaymentSuccessful,
                is CardReaderPaymentState.PaymentSuccessful.ExternalReaderPaymentSuccessfulReceiptSentAutomatically,
                is CardReaderPaymentState.PrintingReceipt,
                CardReaderPaymentState.ReFetchingOrder,
                CardReaderPaymentState.SharingReceipt -> Unit
            }
        }
    }

    suspend fun trackReaderReadyForCardInput(readerReadyForCardInput: Flow<Unit>?) {
        readerReadyForCardInput?.collect { readerReadyForPaymentTracker.track(ReaderReadySignal.INPUT_REQUESTED) }
    }
}
