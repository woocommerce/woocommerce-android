package com.woocommerce.android.cardreader.internal.connection

import com.stripe.stripeterminal.callable.ReaderCallback
import com.stripe.stripeterminal.callable.TerminalListener
import com.stripe.stripeterminal.model.external.ConnectionStatus
import com.stripe.stripeterminal.model.external.ConnectionStatus.CONNECTED
import com.stripe.stripeterminal.model.external.ConnectionStatus.CONNECTING
import com.stripe.stripeterminal.model.external.ConnectionStatus.NOT_CONNECTED
import com.stripe.stripeterminal.model.external.PaymentStatus
import com.stripe.stripeterminal.model.external.Reader
import com.stripe.stripeterminal.model.external.ReaderEvent
import com.stripe.stripeterminal.model.external.TerminalException
import com.woocommerce.android.cardreader.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.CardReaderStatus
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus
import com.woocommerce.android.cardreader.internal.wrappers.LogWrapper
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class ConnectionManager(
    private val terminal: TerminalWrapper,
    private val logWrapper: LogWrapper,
    private val discoverReadersAction: DiscoverReadersAction
) : TerminalListener {
    val readerStatus: MutableStateFlow<CardReaderStatus> = MutableStateFlow(CardReaderStatus.NOT_CONNECTED)

    fun startDiscovery(isSimulated: Boolean) =
        discoverReadersAction.discoverReaders(isSimulated).map { state ->
            when (state) {
                is DiscoverReadersStatus.Started -> {
                    CardReaderDiscoveryEvents.Started
                }
                is DiscoverReadersStatus.Failure -> {
                    CardReaderDiscoveryEvents.Failed(state.exception.toString())
                }
                is DiscoverReadersStatus.FoundReaders -> {
                    CardReaderDiscoveryEvents.ReadersFound(state.readers.mapNotNull { it.serialNumber })
                }
                DiscoverReadersStatus.Success -> {
                    CardReaderDiscoveryEvents.Succeeded
                }
            }
        }

    fun connectToReader(readerId: String) {
        discoverReadersAction.foundReaders.find { it.serialNumber == readerId }?.let {
            terminal.connectToReader(it, object : ReaderCallback {
                override fun onFailure(e: TerminalException) {
                    logWrapper.d("CardReader", "connecting to reader failed: ${e.errorMessage}")
                }

                override fun onSuccess(reader: Reader) {
                    logWrapper.d("CardReader", "connecting to reader succeeded")
                }
            })
        } ?: logWrapper.e("CardReader", "Connecting to reader failed: reader not found")
    }

    override fun onUnexpectedReaderDisconnect(reader: Reader) {
        readerStatus.value = CardReaderStatus.NOT_CONNECTED
        logWrapper.d("CardReader", "onUnexpectedReaderDisconnect")
    }

    override fun onConnectionStatusChange(status: ConnectionStatus) {
        super.onConnectionStatusChange(status)
        readerStatus.value = when (status) {
            NOT_CONNECTED -> CardReaderStatus.NOT_CONNECTED
            CONNECTING -> CardReaderStatus.CONNECTING
            CONNECTED -> CardReaderStatus.CONNECTED
        }
        logWrapper.d("CardReader", "onConnectionStatusChange: ${status.name}")
    }

    override fun onPaymentStatusChange(status: PaymentStatus) {
        super.onPaymentStatusChange(status)
        // TODO cardreader: Not Implemented
        logWrapper.d("CardReader", "onPaymentStatusChange: ${status.name}")
    }

    override fun onReportLowBatteryWarning() {
        super.onReportLowBatteryWarning()
        // TODO cardreader: Not Implemented
        logWrapper.d("CardReader", "onReportLowBatteryWarning")
    }

    override fun onReportReaderEvent(event: ReaderEvent) {
        super.onReportReaderEvent(event)
        // TODO cardreader: Not Implemented
        logWrapper.d("CardReader", "onReportReaderEvent: $event.name")
    }
}
