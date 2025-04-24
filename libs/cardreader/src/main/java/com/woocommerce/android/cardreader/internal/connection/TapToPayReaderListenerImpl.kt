package com.woocommerce.android.cardreader.internal.connection

import com.stripe.stripeterminal.external.callable.TapToPayReaderListener
import com.stripe.stripeterminal.external.models.DisconnectReason
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.callable.Cancelable
import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.internal.LOG_TAG

class TapToPayReaderListenerImpl(
    private val logWrapper: LogWrapper
) : TapToPayReaderListener {
    override fun onDisconnect(reason: DisconnectReason) {
        logWrapper.d(LOG_TAG, "onUnexpectedReaderDisconnect")
    }

    override fun onReaderReconnectFailed(reader: Reader) {
        logWrapper.d(LOG_TAG, "onReaderReconnectFailed")
    }

    override fun onReaderReconnectStarted(
        reader: Reader,
        cancelReconnect: Cancelable,
        reason: DisconnectReason
    ) {
        logWrapper.d(LOG_TAG, "onReaderReconnectStarted")
    }

    override fun onReaderReconnectSucceeded(reader: Reader) {
        logWrapper.d(LOG_TAG, "onReaderReconnectSucceeded")
    }
}
