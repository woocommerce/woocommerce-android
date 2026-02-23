package com.woocommerce.android.ui.woopos.cardpayment

import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState
import com.woocommerce.android.ui.woopos.util.analytics.WooPosPaymentStateAnalyticsTracker
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class WooPosCardPaymentAnalyticsTracker @Inject constructor(
    private val paymentStateTracker: WooPosPaymentStateAnalyticsTracker,
) {
    suspend fun trackPaymentStates(paymentState: StateFlow<CardReaderPaymentOrRefundState>?) {
        paymentStateTracker.trackPaymentStates(paymentState)
    }
}
