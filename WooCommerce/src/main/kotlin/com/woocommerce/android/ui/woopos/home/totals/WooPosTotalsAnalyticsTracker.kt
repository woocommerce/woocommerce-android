package com.woocommerce.android.ui.woopos.home.totals

import com.woocommerce.android.WooException
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.CreateNewOrderTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.EmailReceiptTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTrackingDataKeeper
import com.woocommerce.android.ui.woopos.util.analytics.WooPosPaymentStateAnalyticsTracker
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class WooPosTotalsAnalyticsTracker @Inject constructor(
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val analyticsData: WooPosAnalyticsTrackingDataKeeper,
    private val productsDataSource: WooPosProductsDataSource,
    private val paymentStateTracker: WooPosPaymentStateAnalyticsTracker,
) {
    suspend fun trackPaymentStates(paymentState: StateFlow<CardReaderPaymentOrRefundState>?) {
        paymentStateTracker.trackPaymentStates(paymentState)
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
