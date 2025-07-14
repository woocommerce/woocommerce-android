package com.woocommerce.android.ui.woopos.util.analytics

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosAnalyticsTrackingDataKeeper @Inject constructor() {
    var interactionWithCustomerStartedTimestamp: Long? = null
    var orderSyncSuccessTimestamp: Long? = null
    var readerReadyForPaymentTimestamp: Long? = null
    var cardTappedTimestamp: Long? = null
    var checkoutButtonTapsCount: Int = 0

    fun reset() {
        interactionWithCustomerStartedTimestamp = null
        orderSyncSuccessTimestamp = null
        readerReadyForPaymentTimestamp = null
        cardTappedTimestamp = null
        checkoutButtonTapsCount = 0
    }
}
