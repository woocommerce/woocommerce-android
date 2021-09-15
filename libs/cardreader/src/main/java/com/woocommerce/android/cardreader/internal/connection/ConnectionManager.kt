package com.woocommerce.android.cardreader.internal.connection

import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.ReaderCallback
import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.connection.CardReaderImpl
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.CardReaderTypesToDiscover
import com.woocommerce.android.cardreader.internal.LOG_TAG
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction
import com.woocommerce.android.cardreader.internal.connection.actions.DiscoverReadersAction.DiscoverReadersStatus
import com.woocommerce.android.cardreader.internal.wrappers.LogWrapper
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class ConnectionManager(
    private val terminal: TerminalWrapper,
    private val bluetoothReaderListener: BluetoothReaderListenerImpl,
    private val discoverReadersAction: DiscoverReadersAction,
    private val terminalListenerImpl: TerminalListenerImpl,
    private val logWrapper: LogWrapper,
) {
    val softwareUpdateStatus = bluetoothReaderListener.updateStatusEvents
    val softwareUpdateAvailability = bluetoothReaderListener.updateAvailabilityEvents

    suspend fun listenForBluetoothCardReaderMessages() = callbackFlow {
        val listener = object : BluetoothCardReaderMessagesObserver {
            override fun sendMessage(message: BluetoothCardReaderMessages) {
                this@callbackFlow.sendAndLog(message)
            }
        }
        bluetoothReaderListener.registerBluetoothCardReaderMessagesObserver(listener)
        awaitClose {
            bluetoothReaderListener.unregisterBluetoothCardReaderMessages()
        }
    }

    fun discoverReaders(isSimulated: Boolean, cardReaderTypesToDiscover: CardReaderTypesToDiscover) =
        discoverReadersAction.discoverReaders(isSimulated).map { state ->
            when (state) {
                is DiscoverReadersStatus.Started -> {
                    CardReaderDiscoveryEvents.Started
                }
                is DiscoverReadersStatus.Failure -> {
                    CardReaderDiscoveryEvents.Failed(state.exception.errorMessage)
                }
                is DiscoverReadersStatus.FoundReaders -> {
                    val filtering: (Reader) -> Boolean = when (cardReaderTypesToDiscover) {
                        is CardReaderTypesToDiscover.SpecificReaders -> { reader ->
                            cardReaderTypesToDiscover.readers.map { it.name }.contains(reader.deviceType.name)
                        }
                        CardReaderTypesToDiscover.UnspecifiedReaders -> { _ -> true }
                    }
                    CardReaderDiscoveryEvents.ReadersFound(
                        state.readers
                            .filter(filtering)
                            .map { CardReaderImpl(it) }
                    )
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
            updateReaderStatus(CardReaderStatus.Connecting)
            val configuration = ConnectionConfiguration.BluetoothConnectionConfiguration(locationId)
            val readerCallback = object : ReaderCallback {
                override fun onSuccess(reader: Reader) {
                    updateReaderStatus(CardReaderStatus.Connected(CardReaderImpl(reader)))
                    continuation.resume(true)
                }

                override fun onFailure(e: TerminalException) {
                    updateReaderStatus(CardReaderStatus.NotConnected)
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
                updateReaderStatus(CardReaderStatus.NotConnected)
                continuation.resume(false)
            }

            override fun onSuccess() {
                updateReaderStatus(CardReaderStatus.NotConnected)
                continuation.resume(true)
            }
        })
    }

    private fun startStateResettingJobIfNeeded(currentStatus: CardReaderStatus) {
        if (currentStatus !is CardReaderStatus.Connecting) return

        val connectedScope = CoroutineScope(Dispatchers.Default)
        connectedScope.launch {
            terminalListenerImpl.readerStatus.collect { connectionStatus ->
                if (connectionStatus is CardReaderStatus.NotConnected) {
                    bluetoothReaderListener.resetConnectionState()
                    connectedScope.cancel()
                }
            }
        }
    }

    private fun updateReaderStatus(status: CardReaderStatus) {
        terminalListenerImpl.updateReaderStatus(status)
        startStateResettingJobIfNeeded(status)
    }

    private fun ProducerScope<BluetoothCardReaderMessages>.sendAndLog(status: BluetoothCardReaderMessages) {
        trySendBlocking(status)
            .onClosed { logWrapper.e(LOG_TAG, it?.message.orEmpty()) }
            .onFailure { logWrapper.e(LOG_TAG, it?.message.orEmpty()) }
    }
}
