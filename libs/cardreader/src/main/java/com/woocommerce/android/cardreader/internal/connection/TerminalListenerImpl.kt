package com.woocommerce.android.cardreader.internal.connection

import com.stripe.stripeterminal.external.callable.TerminalListener
import com.stripe.stripeterminal.external.models.ConnectionStatus
import com.stripe.stripeterminal.external.models.PaymentStatus
import com.stripe.stripeterminal.external.models.Reader
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.internal.LOG_TAG
import com.woocommerce.android.cardreader.internal.wrappers.LogWrapper
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class TerminalListenerImpl(
    private val terminal: TerminalWrapper,
    private val logWrapper: LogWrapper,
) : TerminalListener {
    private val _readerStatus = MutableStateFlow<CardReaderStatus>(CardReaderStatus.NotConnected)
    val readerStatus: StateFlow<CardReaderStatus> = _readerStatus

    override fun onUnexpectedReaderDisconnect(reader: Reader) {
        _readerStatus.value = CardReaderStatus.NotConnected
        logWrapper.d(LOG_TAG, "onUnexpectedReaderDisconnect")
    }

    override fun onConnectionStatusChange(status: ConnectionStatus) {
        super.onConnectionStatusChange(status)
        _readerStatus.value = when (status) {
            ConnectionStatus.NOT_CONNECTED -> CardReaderStatus.NotConnected
            ConnectionStatus.CONNECTING -> CardReaderStatus.Connecting
            ConnectionStatus.CONNECTED -> CardReaderStatus.Connected(terminal.getConnectedReader()!!)
        }
        logWrapper.d(LOG_TAG, "onConnectionStatusChange: ${status.name}")
    }

    override fun onPaymentStatusChange(status: PaymentStatus) {
        super.onPaymentStatusChange(status)
        // TODO cardreader: Not Implemented
        logWrapper.d(LOG_TAG, "onPaymentStatusChange: ${status.name}")
    }
}
