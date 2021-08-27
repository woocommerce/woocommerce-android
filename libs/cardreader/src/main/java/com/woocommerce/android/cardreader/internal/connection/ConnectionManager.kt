package com.woocommerce.android.cardreader.internal.connection

import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.ReaderCallback
import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.connection.CardReaderImpl
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateAvailability
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatus
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class ConnectionManager(
    private val terminal: TerminalWrapper,
    private val bluetoothReaderListener: BluetoothReaderListenerImpl,
    private val discoverReadersAction: DiscoverReadersAction,
) {
    val softwareUpdateStatus = bluetoothReaderListener.events.filterIsInstance<SoftwareUpdateStatus>()
    val softwareUpdateAvailability: Flow<SoftwareUpdateAvailability> = bluetoothReaderListener.events.filterIsInstance()

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
            val locationId = cardReader.locationId ?: throw IllegalStateException(
                "Only attached to a location readers are supported at the moment"
            )
            val configuration = ConnectionConfiguration.BluetoothConnectionConfiguration(locationId)
            val readerCallback = object : ReaderCallback {
                override fun onSuccess(reader: Reader) {
                    continuation.resume(true)
                }

                override fun onFailure(e: TerminalException) {
                    continuation.resume(false)
                }
            }

            terminal.connectToReader(
                cardReader.cardReader,
                configuration,
                readerCallback,
                bluetoothReaderListener,
            )
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
}
