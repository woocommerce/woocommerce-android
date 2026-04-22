package com.woocommerce.android.ui.woopos.cardreader.remote

import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.connection.CardReaderTypesToDiscover
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
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
                        mutex.withLock { state.bluetoothReaders = event.list }
                        sendSnapshot(mutex, state)
                    }
                    is CardReaderDiscoveryEvents.Failed -> send(WooPosUnifiedDiscoveryEvent.Failed(event.msg))
                    is CardReaderDiscoveryEvents.Succeeded -> send(WooPosUnifiedDiscoveryEvent.Succeeded)
                }
            }
        }

        if (featureFlagRepository.isEnabled(FeatureFlag.REMOTE_TAP_TO_PAY)) {
            launch {
                remoteDiscovery.discover().collect { phone ->
                    mutex.withLock { state.phonesByName[phone.name] = phone }
                    sendSnapshot(mutex, state)
                }
            }
        }
    }

    private suspend fun ProducerScope<WooPosUnifiedDiscoveryEvent>.sendSnapshot(
        mutex: Mutex,
        state: DiscoveryState,
    ) {
        val snapshot = mutex.withLock {
            state.bluetoothReaders.map(WooPosDiscoveredReader::Bluetooth) + state.phonesByName.values
        }
        send(WooPosUnifiedDiscoveryEvent.ReadersFound(snapshot))
    }

    private class DiscoveryState {
        var bluetoothReaders: List<CardReader> = emptyList()
        val phonesByName: LinkedHashMap<String, WooPosDiscoveredReader.Phone> = linkedMapOf()
    }
}
