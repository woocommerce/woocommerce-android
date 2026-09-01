package com.woocommerce.android.ui.woopos.cardreader.remote

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.CardReaderStore
import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.describeWithCauses
import com.woocommerce.android.cardreader.payments.PaymentInfo
import com.woocommerce.android.cardreader.remote.CardReaderRemoteTabletClient
import com.woocommerce.android.cardreader.remote.CollectPaymentOutcome
import com.woocommerce.android.cardreader.remote.ConnectOutcome
import com.woocommerce.android.cardreader.remote.DiscoveredRemoteReader
import com.woocommerce.android.ui.payments.cardreader.connect.CardReaderLocationRepository
import com.woocommerce.android.ui.payments.cardreader.connect.CardReaderLocationRepository.LocationIdFetchingResult
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class WooPosRemoteReaderSession @Inject constructor(
    private val cardReaderStore: CardReaderStore,
    private val locationRepository: CardReaderLocationRepository,
    private val cardReaderOnboardingChecker: CardReaderOnboardingChecker,
    private val clientProvider: WooPosRemoteReaderClientProvider,
    private val logger: WooPosLogWrapper,
    private val resourceProvider: ResourceProvider,
    private val errorMapper: WooPosRemoteReaderErrorMapper,
) {
    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private var client: CardReaderRemoteTabletClient? = null
    private val mutex = Mutex()
    private var monitorScope: CoroutineScope? = null

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

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchToken(): String? {
        val token = try {
            cardReaderStore.fetchConnectionToken()
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (cause: Throwable) {
            logger.e("Failed to fetch connection token", cause)
            failWith(
                message = R.string.woopos_remote_reader_failed_token_invalid,
                reason = State.Failed.Reason.TOKEN_FETCH_FAILED,
                errorDescription = cause.describeWithCauses(),
            )
            return null
        }
        if (token.isBlank()) {
            logger.e("Connection token was empty")
            failWith(
                message = R.string.woopos_remote_reader_failed_token_invalid,
                reason = State.Failed.Reason.TOKEN_FETCH_FAILED,
                errorDescription = "connection token was empty",
            )
            return null
        }
        return token
    }

    private suspend fun fetchLocationIdOrFail(): String? {
        return when (val result = fetchLocationId()) {
            is LocationIdFetchingResult.Success -> result.locationId
            is LocationIdFetchingResult.Error.MissingAddress ->
                failWith(
                    message = R.string.card_reader_connect_missing_address,
                    reason = State.Failed.Reason.LOCATION_MISSING_ADDRESS,
                    errorDescription = "merchant address is missing",
                ).let { null }
            is LocationIdFetchingResult.Error.InvalidPostalCode ->
                failWith(
                    message = R.string.card_reader_connect_invalid_postal_code_hint,
                    reason = State.Failed.Reason.LOCATION_INVALID_POSTAL_CODE,
                    errorDescription = "merchant postal code is invalid",
                ).let { null }
            is LocationIdFetchingResult.Error.Other -> {
                logger.e("Could not fetch merchant location: ${result.error}")
                failWith(
                    message = R.string.woopos_remote_reader_connect_failed_generic,
                    reason = State.Failed.Reason.LOCATION_FETCH_FAILED,
                    errorDescription = result.error ?: "unknown location error",
                ).let { null }
            }
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
            deviceName = reader.name,
            siteHash = reader.siteHash,
            deviceId = reader.deviceId,
        )
        return when (val outcome = newClient.connect(discovered, token, locationId)) {
            is ConnectOutcome.Success -> State.Connected(reader, outcome.readerSerial)
                .also {
                    _state.value = it
                    watchForRemoteClose(newClient)
                }
            is ConnectOutcome.Rejected -> {
                logger.e("Remote reader connect rejected: ${outcome.error.code} - ${outcome.description}")
                fail(
                    message = errorMapper.toUserMessage(
                        error = outcome.error,
                        fallback = R.string.woopos_remote_reader_connect_failed_generic,
                    ),
                    reason = State.Failed.Reason.CONNECT_REJECTED,
                    errorDescription = "${outcome.error.code} - ${outcome.description}",
                )
            }
            is ConnectOutcome.Failed -> {
                logger.e(
                    "Remote reader connect failed: ${outcome.cause::class.java.simpleName}",
                    outcome.cause
                )
                fail(
                    message = resourceProvider.getString(R.string.woopos_remote_reader_connect_failed_generic),
                    reason = State.Failed.Reason.CONNECT_EXCEPTION,
                    errorDescription = outcome.cause.describeWithCauses(),
                )
            }
        }
    }

    private fun watchForRemoteClose(watchedClient: CardReaderRemoteTabletClient) {
        monitorScope?.cancel()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        monitorScope = scope
        scope.launch {
            watchedClient.connectionClosed.collect { isClosed ->
                if (!isClosed) return@collect
                mutex.withLock {
                    if (client === watchedClient && _state.value is State.Connected) {
                        logger.d("Remote reader connection closed by the phone")
                        disconnectInternal()
                        _state.value = State.Idle
                    }
                }
            }
        }
    }

    suspend fun disconnect() = mutex.withLock {
        disconnectInternal()
        _state.value = State.Idle
    }

    suspend fun sendCollectPayment(
        paymentInfo: PaymentInfo,
        timeoutMillis: Long = CardReaderRemoteTabletClient.DEFAULT_COLLECT_PAYMENT_TIMEOUT_MILLIS,
    ): CollectPaymentOutcome {
        val activeClient = mutex.withLock {
            if (_state.value !is State.Connected) return@withLock null
            client
        } ?: return CollectPaymentOutcome.Failed(IllegalStateException("Reader not connected"))
        return activeClient.collectPayment(paymentInfo, timeoutMillis)
    }

    private fun disconnectInternal() {
        monitorScope?.cancel()
        monitorScope = null
        client?.disconnect()
        client = null
    }

    private suspend fun fetchLocationId(): LocationIdFetchingResult {
        val pluginType = cardReaderOnboardingChecker.getOnboardingState().preferredPlugin
            ?: PluginType.WOOCOMMERCE_PAYMENTS
        return locationRepository.getDefaultLocationId(pluginType)
    }

    private fun failWith(
        @StringRes message: Int,
        reason: State.Failed.Reason,
        errorDescription: String,
    ): State.Failed = fail(resourceProvider.getString(message), reason, errorDescription)

    private fun fail(
        message: String,
        reason: State.Failed.Reason,
        errorDescription: String,
    ): State.Failed {
        logger.e("Remote reader session failed: ${reason.analyticsValue} - $errorDescription")
        disconnectInternal()
        val failed = State.Failed(message, reason, errorDescription)
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
        data class Failed(
            val message: String,
            val reason: Reason,
            val errorDescription: String,
        ) : State() {
            enum class Reason(val analyticsValue: String) {
                TOKEN_FETCH_FAILED("token_fetch_failed"),
                LOCATION_MISSING_ADDRESS("location_missing_address"),
                LOCATION_INVALID_POSTAL_CODE("location_invalid_postal_code"),
                LOCATION_FETCH_FAILED("location_fetch_failed"),
                CONNECT_REJECTED("connect_rejected"),
                CONNECT_EXCEPTION("connect_exception"),
            }
        }
    }

    private companion object {
        const val SIMULATED_CONNECT_DELAY_MS = 800L
    }
}

class WooPosRemoteReaderClientProvider @Inject constructor(
    private val logWrapper: LogWrapper,
) {
    fun create(): CardReaderRemoteTabletClient = CardReaderRemoteTabletClient.create(logWrapper)
}
