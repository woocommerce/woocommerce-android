package com.woocommerce.android.ui.woopos.cardreader.remote

import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.connection.CardReaderTypesToDiscover
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.util.siteIdHash
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
        val siteHash: String,
        val isSimulated: Boolean = false,
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
    private val simulatedRemoteDiscovery: WooPosSimulatedRemoteReaderDiscovery,
    private val selectedSite: SelectedSite,
    private val logger: WooPosLogWrapper,
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

        val phoneSource: WooPosPhoneDiscoverySource =
            if (isSimulated) simulatedRemoteDiscovery else remoteDiscovery
        launch {
            phoneSource.discover()
                // NSD failures must not tear down BT discovery — degrade to BT-only.
                .catch { throwable -> logger.e("NSD discovery failed, degrading to BT-only", throwable) }
                .collect { event ->
                    when (event) {
                        is WooPosPhoneDiscoveryEvent.Added ->
                            addPhoneAndSendSnapshot(mutex, state, event.phone)
                        is WooPosPhoneDiscoveryEvent.Removed ->
                            removePhoneAndSendSnapshot(mutex, state, event.name)
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
            val expectedSiteHash = selectedSite.getOrNull()?.remoteId()?.value?.let(::siteIdHash)
            if (expectedSiteHash == null || phone.siteHash != expectedSiteHash) {
                logger.d("Dropping NSD phone with site mismatch")
                return@withLock
            }

            val fingerprint = phone.fingerprintBase64

            // The phone may have re-registered with a new fingerprint+name without us
            // receiving the Lost event for the previous one (Android NSD goodbye is
            // unreliable). Drop any earlier entries from the same host so we don't show
            // the same device twice.
            val staleNames = state.phonesByFingerprint.values
                .filter { it.host == phone.host && it.fingerprintBase64 != fingerprint }
                .map { it.name }
            staleNames.forEach { name ->
                val staleFp = state.fingerprintByName.remove(name)
                if (staleFp != null) state.phonesByFingerprint.remove(staleFp)
            }

            val isUpdate = state.phonesByFingerprint.containsKey(fingerprint)
            if (isUpdate || state.phonesByFingerprint.size < MAX_DISCOVERED_PHONES) {
                state.phonesByFingerprint[fingerprint] = phone
                state.fingerprintByName[phone.name] = fingerprint
                send(WooPosUnifiedDiscoveryEvent.ReadersFound(state.snapshot()))
            }
        }
    }

    private suspend fun ProducerScope<WooPosUnifiedDiscoveryEvent>.removePhoneAndSendSnapshot(
        mutex: Mutex,
        state: DiscoveryState,
        serviceName: String,
    ) {
        mutex.withLock {
            val fingerprint = state.fingerprintByName.remove(serviceName) ?: return@withLock
            state.phonesByFingerprint.remove(fingerprint)
            send(WooPosUnifiedDiscoveryEvent.ReadersFound(state.snapshot()))
        }
    }

    private class DiscoveryState {
        var bluetoothReaders: List<CardReader> = emptyList()
        val phonesByFingerprint: LinkedHashMap<String, WooPosDiscoveredReader.Phone> = linkedMapOf()
        val fingerprintByName: MutableMap<String, String> = mutableMapOf()

        fun snapshot(): List<WooPosDiscoveredReader> =
            bluetoothReaders.map(WooPosDiscoveredReader::Bluetooth) + phonesByFingerprint.values
    }

    private companion object {
        const val MAX_DISCOVERED_PHONES = 8
    }
}
