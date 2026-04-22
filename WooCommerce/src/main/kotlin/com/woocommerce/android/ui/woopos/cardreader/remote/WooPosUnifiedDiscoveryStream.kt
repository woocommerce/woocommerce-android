package com.woocommerce.android.ui.woopos.cardreader.remote

import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.connection.CardReaderTypesToDiscover
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.InetAddress
import javax.inject.Inject

sealed class WooPosDiscoveredReader {
    abstract val transport: WooPosDiscoveryTransport

    data class Bluetooth(val cardReader: CardReader) : WooPosDiscoveredReader() {
        override val transport = WooPosDiscoveryTransport.Bluetooth
    }

    data class Phone(
        val name: String,
        val host: InetAddress,
        val port: Int,
        val fingerprintBase64: String,
    ) : WooPosDiscoveredReader() {
        override val transport = WooPosDiscoveryTransport.WifiLan
    }
}

sealed interface WooPosDiscoveryTransport {
    data object Bluetooth : WooPosDiscoveryTransport
    data object WifiLan : WooPosDiscoveryTransport
}

sealed class WooPosUnifiedDiscoveryEvent {
    data object Started : WooPosUnifiedDiscoveryEvent()
    data class ReadersFound(val readers: List<WooPosDiscoveredReader>) : WooPosUnifiedDiscoveryEvent()
    data class Failed(val msg: String) : WooPosUnifiedDiscoveryEvent()
    data object Succeeded : WooPosUnifiedDiscoveryEvent()
}

class WooPosUnifiedDiscoveryStream @Inject constructor(
    private val cardReaderManager: CardReaderManager,
    private val remoteDiscovery: WooPosRemoteReaderDiscovery,
    private val featureFlagRepository: FeatureFlagRepository,
) {
    fun discover(
        isSimulated: Boolean,
        cardReaderTypesToDiscover: CardReaderTypesToDiscover,
    ): Flow<WooPosUnifiedDiscoveryEvent> = channelFlow {
        val mutex = Mutex()
        val state = DiscoveryState()

        launch {
            cardReaderManager.discoverReaders(isSimulated, cardReaderTypesToDiscover).collect { event ->
                when (event) {
                    is CardReaderDiscoveryEvents.Started -> send(WooPosUnifiedDiscoveryEvent.Started)
                    is CardReaderDiscoveryEvents.ReadersFound -> {
                        updateBluetoothAndSendSnapshot(mutex, state, event.list)
                    }
                    is CardReaderDiscoveryEvents.Failed -> send(WooPosUnifiedDiscoveryEvent.Failed(event.msg))
                    is CardReaderDiscoveryEvents.Succeeded -> send(WooPosUnifiedDiscoveryEvent.Succeeded)
                }
            }
        }

        if (featureFlagRepository.isEnabled(FeatureFlag.REMOTE_TAP_TO_PAY)) {
            launch {
                remoteDiscovery.discover()
                    // NSD failures must not tear down BT discovery — degrade to BT-only silently.
                    .catch { }
                    .collect { phone ->
                        addPhoneAndSendSnapshot(mutex, state, phone)
                    }
            }
        }
    }

    private suspend fun ProducerScope<WooPosUnifiedDiscoveryEvent>.updateBluetoothAndSendSnapshot(
        mutex: Mutex,
        state: DiscoveryState,
        readers: List<CardReader>,
    ) {
        mutex.withLock {
            state.bluetoothReaders = readers
            send(WooPosUnifiedDiscoveryEvent.ReadersFound(state.snapshot()))
        }
    }

    private suspend fun ProducerScope<WooPosUnifiedDiscoveryEvent>.addPhoneAndSendSnapshot(
        mutex: Mutex,
        state: DiscoveryState,
        phone: WooPosDiscoveredReader.Phone,
    ) {
        mutex.withLock {
            val key = phone.fingerprintBase64
            val isUpdate = state.phonesByFingerprint.containsKey(key)
            if (isUpdate || state.phonesByFingerprint.size < MAX_DISCOVERED_PHONES) {
                state.phonesByFingerprint[key] = phone
                send(WooPosUnifiedDiscoveryEvent.ReadersFound(state.snapshot()))
            }
        }
    }

    private class DiscoveryState {
        var bluetoothReaders: List<CardReader> = emptyList()
        val phonesByFingerprint: LinkedHashMap<String, WooPosDiscoveredReader.Phone> = linkedMapOf()

        fun snapshot(): List<WooPosDiscoveredReader> =
            bluetoothReaders.map(WooPosDiscoveredReader::Bluetooth) + phonesByFingerprint.values
    }

    private companion object {
        const val MAX_DISCOVERED_PHONES = 32
    }
}
