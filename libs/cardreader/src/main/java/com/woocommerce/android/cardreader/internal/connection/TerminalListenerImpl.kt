package com.woocommerce.android.cardreader.internal.connection

import com.stripe.stripeterminal.external.callable.TerminalListener
import com.stripe.stripeterminal.external.models.ConnectionStatus
import com.stripe.stripeterminal.external.models.PaymentStatus
import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.internal.LOG_TAG
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class TerminalListenerImpl(
    private val logWrapper: LogWrapper,
) : TerminalListener {
    private val _readerStatus = MutableStateFlow<CardReaderStatus>(CardReaderStatus.NotConnected())
    val readerStatus: StateFlow<CardReaderStatus> = _readerStatus

    fun updateReaderStatus(newStatus: CardReaderStatus) {
        _readerStatus.value = newStatus
    }

    override fun onConnectionStatusChange(status: ConnectionStatus) {
        logWrapper.d(LOG_TAG, "onConnectionStatusChange: ${status.name}")

        if (status == ConnectionStatus.NOT_CONNECTED) {
            _readerStatus.value = CardReaderStatus.NotConnected()
        }
    }

    override fun onPaymentStatusChange(status: PaymentStatus) {
        logWrapper.d(LOG_TAG, "onPaymentStatusChange: ${status.name}")
    }
}
