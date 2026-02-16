package com.woocommerce.android.ui.woopos.cardpayment

import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.analytics.WooPosPaymentStateAnalyticsTracker
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class WooPosCardPaymentAnalyticsTracker @Inject constructor(
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val paymentStateTracker: WooPosPaymentStateAnalyticsTracker,
) {
    suspend fun trackPaymentStates(paymentState: StateFlow<CardReaderPaymentOrRefundState>?) {
        paymentStateTracker.trackPaymentStates(paymentState)
    }

    suspend fun trackEmailReceiptTapped() {
        analyticsTracker.track(WooPosAnalyticsEvent.Event.EmailReceiptTapped)
    }
}
