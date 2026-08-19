package com.woocommerce.android.ui.woopos.home.totals

import com.woocommerce.android.WooException
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.CreateNewOrderTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.EmailReceiptTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTrackingDataKeeper
import com.woocommerce.android.ui.woopos.util.analytics.WooPosReaderReadyForPaymentTracker
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class WooPosTotalsAnalyticsTracker @Inject constructor(
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val analyticsData: WooPosAnalyticsTrackingDataKeeper,
    private val productsDataSource: WooPosProductsDataSource,
    private val readerReadyForPaymentTracker: WooPosReaderReadyForPaymentTracker,
) {
    suspend fun trackPaymentStates(paymentState: StateFlow<CardReaderPaymentOrRefundState>?) {
        paymentState?.distinctUntilChanged { old, new -> old::class == new::class }?.collect {
            when (it) {
                is CardReaderPaymentState.ProcessingPayment -> readerReadyForPaymentTracker.track()

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
                errorType = (error as? WooException)?.error?.type?.name,
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

    suspend fun trackCashPaymentTapped() {
        analyticsTracker.track(WooPosAnalyticsEvent.Event.CheckoutCashPaymentTapped)
    }

    suspend fun trackCheckoutTapToPayPaymentTapped() {
        analyticsTracker.track(WooPosAnalyticsEvent.Event.CheckoutTapToPayPaymentTapped)
    }

    suspend fun trackCheckoutScanToPayPaymentTapped() {
        analyticsTracker.track(WooPosAnalyticsEvent.Event.CheckoutScanToPayPaymentTapped)
    }

    suspend fun trackCheckoutMarkAsPaidTapped() {
        analyticsTracker.track(WooPosAnalyticsEvent.Event.CheckoutMarkAsPaidTapped)
    }

    suspend fun trackCheckoutOutdatedItemDetectedScreenShown() {
        val syncStrategy = productsDataSource.getCurrentSyncStrategy()
        analyticsTracker.track(
            WooPosAnalyticsEvent.Event.CheckoutOutdatedItemDetectedScreenShown(
                reason = "deleted",
                syncStrategy = syncStrategy
            )
        )
    }

    suspend fun trackCheckoutOutdatedItemDetectedEditOrderTapped() {
        val syncStrategy = productsDataSource.getCurrentSyncStrategy()
        analyticsTracker.track(
            WooPosAnalyticsEvent.Event.CheckoutOutdatedItemDetectedEditOrderTapped(
                reason = "deleted",
                syncStrategy = syncStrategy
            )
        )
    }

    suspend fun trackCheckoutOutdatedItemDetectedRemoveTapped() {
        val syncStrategy = productsDataSource.getCurrentSyncStrategy()
        analyticsTracker.track(
            WooPosAnalyticsEvent.Event.CheckoutOutdatedItemDetectedRemoveTapped(
                reason = "deleted",
                syncStrategy = syncStrategy
            )
        )
    }
}
