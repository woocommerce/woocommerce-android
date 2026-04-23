package com.woocommerce.android.ui.woopos.cardreader.remote

import com.woocommerce.android.cardreader.CardReaderStore
import com.woocommerce.android.cardreader.remote.CardReaderRemoteTabletClient
import com.woocommerce.android.cardreader.remote.CollectPaymentOutcome
import com.woocommerce.android.cardreader.remote.ConnectOutcome
import com.woocommerce.android.cardreader.remote.DiscoveredRemoteReader
import com.woocommerce.android.ui.payments.cardreader.connect.CardReaderLocationRepository
import com.woocommerce.android.ui.payments.cardreader.connect.CardReaderLocationRepository.LocationIdFetchingResult
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosRemoteReaderSession @Inject constructor(
    private val cardReaderStore: CardReaderStore,
    private val locationRepository: CardReaderLocationRepository,
    private val cardReaderOnboardingChecker: CardReaderOnboardingChecker,
    private val clientProvider: WooPosRemoteReaderClientProvider,
    private val logger: WooPosLogWrapper,
) {
    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private var client: CardReaderRemoteTabletClient? = null
    private val mutex = Mutex()

    suspend fun connect(reader: WooPosDiscoveredReader.Phone): State = mutex.withLock {
        disconnectInternal()
        _state.value = State.Connecting(reader)

        if (reader.isSimulated) return@withLock simulateConnect(reader)

        val token = fetchToken() ?: return@withLock _state.value
        val locationId = fetchLocationIdOrFail() ?: return@withLock _state.value
        runConnect(reader, token, locationId)
    }

    private suspend fun simulateConnect(reader: WooPosDiscoveredReader.Phone): State {
        delay(SIMULATED_CONNECT_DELAY_MS)
        val connected = State.Connected(reader, readerSerial = "SIM-${reader.fingerprintBase64}")
        _state.value = connected
        return connected
    }

    private suspend fun fetchToken(): String? {
        val token = runCatching { cardReaderStore.fetchConnectionToken() }
            .getOrElse {
                fail("Failed to fetch connection token: ${it.message}")
                return null
            }
        if (token.isBlank()) {
            fail("Empty connection token")
            return null
        }
        return token
    }

    private suspend fun fetchLocationIdOrFail(): String? {
        return when (val result = fetchLocationId()) {
            is LocationIdFetchingResult.Success -> result.locationId
            is LocationIdFetchingResult.Error.MissingAddress -> fail(REASON_MISSING_ADDRESS).let { null }
            is LocationIdFetchingResult.Error.InvalidPostalCode -> fail(REASON_INVALID_POSTAL_CODE).let { null }
            is LocationIdFetchingResult.Error.Other ->
                fail(result.error ?: "Could not fetch merchant location").let { null }
        }
    }

    private suspend fun runConnect(
        reader: WooPosDiscoveredReader.Phone,
        token: String,
        locationId: String,
    ): State {
        val newClient = clientProvider.create().also { client = it }
        val discovered = DiscoveredRemoteReader(
            name = reader.name,
            host = reader.host,
            port = reader.port,
            fingerprintBase64 = reader.fingerprintBase64,
        )
        return when (val outcome = newClient.connect(discovered, token, locationId)) {
            is ConnectOutcome.Success -> State.Connected(reader, outcome.readerSerial)
                .also { _state.value = it }
            is ConnectOutcome.Rejected -> fail("${outcome.code}: ${outcome.description}")
            is ConnectOutcome.Failed -> fail(outcome.cause.message ?: "Connection failed")
        }
    }

    suspend fun disconnect() = mutex.withLock {
        disconnectInternal()
        _state.value = State.Idle
    }

    suspend fun sendCollectPayment(
        paymentIntentClientSecret: String,
        timeoutMillis: Long = CardReaderRemoteTabletClient.DEFAULT_COLLECT_PAYMENT_TIMEOUT_MILLIS,
    ): CollectPaymentOutcome =
        client?.collectPayment(paymentIntentClientSecret, timeoutMillis)
            ?: CollectPaymentOutcome.Failed(IllegalStateException("Remote session is not connected"))

    private fun disconnectInternal() {
        client?.disconnect()
        client = null
    }

    private suspend fun fetchLocationId(): LocationIdFetchingResult {
        val pluginType = cardReaderOnboardingChecker.getOnboardingState().preferredPlugin
            ?: PluginType.WOOCOMMERCE_PAYMENTS
        return locationRepository.getDefaultLocationId(pluginType)
    }

    private fun fail(message: String): State.Failed {
        logger.e("Remote reader session failed: $message")
        disconnectInternal()
        val failed = State.Failed(message)
        _state.value = failed
        return failed
    }

    sealed class State {
        data object Idle : State()
        data class Connecting(val reader: WooPosDiscoveredReader.Phone) : State()
        data class Connected(
            val reader: WooPosDiscoveredReader.Phone,
            val readerSerial: String?,
        ) : State()
        data class Failed(val message: String) : State()
    }

    private companion object {
        const val REASON_MISSING_ADDRESS = "missing_merchant_address"
        const val REASON_INVALID_POSTAL_CODE = "invalid_postal_code"
        const val SIMULATED_CONNECT_DELAY_MS = 800L
    }
}

class WooPosRemoteReaderClientProvider @Inject constructor() {
    fun create(): CardReaderRemoteTabletClient = CardReaderRemoteTabletClient.create()
}
