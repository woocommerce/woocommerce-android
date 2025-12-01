package com.woocommerce.android.cardreader.internal.connection

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
    override fun onDisconnect(reason: DisconnectReason) {
        logWrapper.d(LOG_TAG, "onDisconnect: reason=$reason")
        terminalListenerImpl.updateReaderStatus(CardReaderStatus.NotConnected())
    }

    override fun onReaderReconnectFailed(reader: Reader) {
        logWrapper.d(LOG_TAG, "onReaderReconnectFailed")
        terminalListenerImpl.updateReaderStatus(CardReaderStatus.NotConnected())
    }

    override fun onReaderReconnectStarted(
        reader: Reader,
        cancelReconnect: Cancelable,
        reason: DisconnectReason
    ) {
        logWrapper.d(LOG_TAG, "onReaderReconnectStarted: reason=$reason")
        terminalListenerImpl.updateReaderStatus(CardReaderStatus.Reconnecting)
    }

    override fun onReaderReconnectSucceeded(reader: Reader) {
        logWrapper.d(LOG_TAG, "onReaderReconnectSucceeded")
        terminalListenerImpl.updateReaderStatus(CardReaderStatus.Connected(CardReaderImpl(reader)))
    }
}
