package com.woocommerce.android.cardreader.internal.connection

import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.TapToPayReaderListener
import com.stripe.stripeterminal.external.models.DisconnectReason
import com.stripe.stripeterminal.external.models.Reader
import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.connection.CardReaderImpl
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.internal.LOG_TAG

internal class TapToPayReaderListenerImpl(
    private val logWrapper: LogWrapper,
    private val terminalListenerImpl: TerminalListenerImpl
) : TapToPayReaderListener {
    var cancelReconnectAction: Cancelable? = null

    override fun onDisconnect(reason: DisconnectReason) {
        logWrapper.d(LOG_TAG, "onDisconnect: reason=$reason")
        terminalListenerImpl.updateReaderStatus(CardReaderStatus.NotConnected())
    }

    override fun onReaderReconnectFailed(reader: Reader) {
        logWrapper.d(LOG_TAG, "onReaderReconnectFailed")
        cancelReconnectAction = null
        terminalListenerImpl.updateReaderStatus(CardReaderStatus.NotConnected())
    }

    override fun onReaderReconnectStarted(
        reader: Reader,
        cancelReconnect: Cancelable,
        reason: DisconnectReason
    ) {
        logWrapper.d(LOG_TAG, "onReaderReconnectStarted: reason=$reason")
        cancelReconnectAction = cancelReconnect
        terminalListenerImpl.updateReaderStatus(CardReaderStatus.Reconnecting)
    }

    override fun onReaderReconnectSucceeded(reader: Reader) {
        logWrapper.d(LOG_TAG, "onReaderReconnectSucceeded")
        cancelReconnectAction = null
        terminalListenerImpl.updateReaderStatus(CardReaderStatus.Connected(CardReaderImpl(reader)))
    }

    fun cancelReconnection(callback: Callback) {
        cancelReconnectAction?.cancel(callback)
        cancelReconnectAction = null
    }
}
