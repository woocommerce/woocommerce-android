package com.woocommerce.android.ui.woopos.home.totals

import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.CreateNewOrderTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.EmailReceiptTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ReaderReadyForCardPayment
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTrackingDataKeeper
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class WooPosTotalsAnalyticsTracker @Inject constructor(
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val analyticsData: WooPosAnalyticsTrackingDataKeeper,
) {
    suspend fun trackPaymentStates(paymentState: StateFlow<CardReaderPaymentOrRefundState>?) {
        paymentState?.distinctUntilChanged { old, new -> old::class == new::class }?.collect {
            when (it) {
                is CardReaderPaymentState.CollectingPayment -> {
                    analyticsData.readerReadyForPaymentTimestamp = System.currentTimeMillis()
                    trackReaderReadyForPayment()
                }

                is CardReaderPaymentState.ProcessingPayment -> {
                    analyticsData.cardTappedTimestamp = System.currentTimeMillis()
                }

                is CardReaderPaymentOrRefundState.CardReaderInteracRefundState.CollectingInteracRefund,
                is CardReaderPaymentOrRefundState.CardReaderInteracRefundState.InteracRefundFailure.Cancelable,
                is CardReaderPaymentOrRefundState.CardReaderInteracRefundState.InteracRefundFailure.NonCancelable,
                is CardReaderPaymentOrRefundState.CardReaderInteracRefundState.InteracRefundSuccessful,
                is CardReaderPaymentOrRefundState.CardReaderInteracRefundState.LoadingData,
                is CardReaderPaymentOrRefundState.CardReaderInteracRefundState.ProcessingInteracRefund,
                is CardReaderPaymentState.LoadingData,
                is CardReaderPaymentState.PaymentCapturing.BuiltInReaderPaymentCapturing,
                is CardReaderPaymentState.PaymentCapturing.ExternalReaderPaymentCapturing,
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
            ReaderReadyForCardPayment.apply {
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

    fun incrementCheckoutButtonTaps() {
        analyticsData.checkoutButtonTapsCount = analyticsData.checkoutButtonTapsCount + 1
    }

    suspend fun trackOrderCreationSuccess() {
        analyticsTracker.track(WooPosAnalyticsEvent.Event.OrderCreationSuccess)
        analyticsData.orderSyncSuccessTimestamp = System.currentTimeMillis()
    }

    suspend fun trackOrderCreationFailed(error: Throwable) {
        analyticsTracker.track(
            WooPosAnalyticsEvent.Error.OrderCreationError(
                errorContext = WooPosTotalsViewModel::class,
                errorType = error::class.simpleName,
                errorDescription = error.message
            )
        )
    }

    suspend fun trackEmailReceiptTapped() {
        analyticsTracker.track(EmailReceiptTapped)
    }

    suspend fun trackCreateNewOrderTapped() {
        analyticsTracker.track(CreateNewOrderTapped)
    }
}
