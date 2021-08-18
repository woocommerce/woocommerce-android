package com.woocommerce.android.cardreader.internal.connection

import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.ReaderCallback
import com.stripe.stripeterminal.external.callable.TerminalListener
import com.stripe.stripeterminal.external.models.ConnectionStatus
import com.stripe.stripeterminal.external.models.ConnectionStatus.CONNECTED
import com.stripe.stripeterminal.external.models.ConnectionStatus.CONNECTING
import com.stripe.stripeterminal.external.models.ConnectionStatus.NOT_CONNECTED
import com.stripe.stripeterminal.external.models.PaymentStatus
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.connection.CardReaderImpl
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus
import com.woocommerce.android.cardreader.internal.wrappers.LogWrapper
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class ConnectionManager(
    private val terminal: TerminalWrapper,
    private val logWrapper: LogWrapper,
    private val discoverReadersAction: DiscoverReadersAction
) : TerminalListener {
    val readerStatus: MutableStateFlow<CardReaderStatus> = MutableStateFlow(CardReaderStatus.NotConnected)

    fun discoverReaders(isSimulated: Boolean) =
        discoverReadersAction.discoverReaders(isSimulated).map { state ->
            when (state) {
                is DiscoverReadersStatus.Started -> {
                    CardReaderDiscoveryEvents.Started
                }
                is DiscoverReadersStatus.Failure -> {
                    CardReaderDiscoveryEvents.Failed(state.exception.errorMessage)
                }
                is DiscoverReadersStatus.FoundReaders -> {
                    CardReaderDiscoveryEvents.ReadersFound(state.readers.map { CardReaderImpl(it) })
                }
                DiscoverReadersStatus.Success -> {
                    CardReaderDiscoveryEvents.Succeeded
                }
            }
        }

    suspend fun connectToReader(cardReader: CardReader) = suspendCoroutine<Boolean> { continuation ->
        (cardReader as CardReaderImpl).let {
        }
    }

    suspend fun disconnectReader() = suspendCoroutine<Boolean> { continuation ->
        terminal.disconnectReader(object : Callback {
            override fun onFailure(e: TerminalException) {
                continuation.resume(false)
            }

            override fun onSuccess() {
                continuation.resume(true)
            }
        })
    }

    override fun onUnexpectedReaderDisconnect(reader: Reader) {
        readerStatus.value = CardReaderStatus.NotConnected
        logWrapper.d("CardReader", "onUnexpectedReaderDisconnect")
    }

    override fun onConnectionStatusChange(status: ConnectionStatus) {
        super.onConnectionStatusChange(status)
        readerStatus.value = when (status) {
            NOT_CONNECTED -> CardReaderStatus.NotConnected
            CONNECTING -> CardReaderStatus.Connecting
            CONNECTED -> CardReaderStatus.Connected(terminal.getConnectedReader()!!)
        }
        logWrapper.d("CardReader", "onConnectionStatusChange: ${status.name}")
    }

    override fun onPaymentStatusChange(status: PaymentStatus) {
        super.onPaymentStatusChange(status)
        // TODO cardreader: Not Implemented
        logWrapper.d("CardReader", "onPaymentStatusChange: ${status.name}")
    }
}
