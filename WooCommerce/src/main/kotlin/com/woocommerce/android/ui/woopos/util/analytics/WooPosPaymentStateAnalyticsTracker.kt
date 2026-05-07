package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class WooPosPaymentStateAnalyticsTracker @Inject constructor(
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val analyticsData: WooPosAnalyticsTrackingDataKeeper,
) {
    suspend fun trackPaymentStates(paymentState: StateFlow<CardReaderPaymentOrRefundState>?) {
        paymentState?.distinctUntilChanged { old, new -> old::class == new::class }?.collect {
            when (it) {
                is CardReaderPaymentState.ProcessingPayment -> {
                    analyticsData.readerReadyForPaymentTimestamp = System.currentTimeMillis()
                    trackReaderReadyForPayment()
                }

                is CardReaderPaymentState.PaymentCapturing -> {
                    analyticsData.cardTappedTimestamp = System.currentTimeMillis()
                }

                is CardReaderPaymentOrRefundState.CardReaderInteracRefundState.CollectingInteracRefund,
                is CardReaderPaymentOrRefundState.CardReaderInteracRefundState.InteracRefundFailure.Cancelable,
                is CardReaderPaymentOrRefundState.CardReaderInteracRefundState.InteracRefundFailure.NonCancelable,
                is CardReaderPaymentOrRefundState.CardReaderInteracRefundState.InteracRefundSuccessful,
                is CardReaderPaymentOrRefundState.CardReaderInteracRefundState.LoadingData,
                is CardReaderPaymentOrRefundState.CardReaderInteracRefundState.ProcessingInteracRefund,
                is CardReaderPaymentState.LoadingData,
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

    private suspend fun trackReaderReadyForPayment() {
        analyticsTracker.track(
            WooPosAnalyticsEvent.Event.ReaderReadyForCardPayment.apply {
                val props = mutableMapOf<String, String>()
                val readerReadyForPaymentTimestamp = analyticsData.readerReadyForPaymentTimestamp
                val orderSyncTimestamp = analyticsData.orderSyncSuccessTimestamp
                if (readerReadyForPaymentTimestamp != null && orderSyncTimestamp != null) {
                    @Suppress("MagicNumber")
                    val waitingTimeSeconds = (readerReadyForPaymentTimestamp - orderSyncTimestamp) / 1000
                    props["waiting_time"] = "$waitingTimeSeconds"
                }
                addProperties(props)
            }
        )
    }
}
