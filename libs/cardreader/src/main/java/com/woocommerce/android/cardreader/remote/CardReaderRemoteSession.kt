package com.woocommerce.android.cardreader.remote

import android.content.Context
import android.os.Build
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.connection.CardReaderTypesToDiscover
import com.woocommerce.android.cardreader.connection.ReaderType
import com.woocommerce.android.cardreader.connection.RemoteTokenChannelProvider
import com.woocommerce.android.cardreader.remote.CardReaderRemoteMessage.CollectPaymentRequest
import com.woocommerce.android.cardreader.remote.CardReaderRemoteMessage.ConnectAck
import com.woocommerce.android.cardreader.remote.CardReaderRemoteMessage.ConnectRequest
import com.woocommerce.android.cardreader.remote.CardReaderRemoteMessage.ErrorMessage
import com.woocommerce.android.cardreader.remote.CardReaderRemoteMessage.PaymentIntentResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException

class CardReaderRemoteSession internal constructor(
    private val context: Context,
    private val cardReaderManager: CardReaderManager,
    private val logWrapper: LogWrapper,
    private val tlsServerFactory: TlsServerFactory,
    private val nsdFactory: NsdFactory,
    private val remoteTokenProviderFactory: RemoteTokenProviderFactory,
    private val disconnectScope: CoroutineScope = defaultDisconnectScope(),
) {
    constructor(
        context: Context,
        cardReaderManager: CardReaderManager,
        logWrapper: LogWrapper,
    ) : this(
        context = context,
        cardReaderManager = cardReaderManager,
        logWrapper = logWrapper,
        tlsServerFactory = TlsServerFactory { CardReaderRemoteTlsServer() },
        nsdFactory = NsdFactory { ctx -> CardReaderRemoteNsd(ctx) },
        remoteTokenProviderFactory = RemoteTokenProviderFactory { RemoteTokenChannelProvider() },
    )

    private val _state = MutableStateFlow<CardReaderRemoteSessionState>(CardReaderRemoteSessionState.Idle)
    val state: StateFlow<CardReaderRemoteSessionState> = _state.asStateFlow()

    private var sessionScope: CoroutineScope? = null
    private var tlsServer: CardReaderRemoteTlsServer? = null
    private var nsdRegistration: CardReaderRemoteNsdRegistration? = null
    private var remoteTokenProvider: RemoteTokenChannelProvider? = null
    private var connection: CardReaderRemoteConnection? = null
    private var readerWasConnected: Boolean = false

    fun start(parentScope: CoroutineScope) {
        if (sessionScope != null) return
        val parentJob = parentScope.coroutineContext[Job]
        val scope = CoroutineScope(parentScope.coroutineContext + SupervisorJob(parentJob))
        sessionScope = scope
        scope.launch {
            @Suppress("TooGenericExceptionCaught")
            try {
                runSession()
                _state.value = CardReaderRemoteSessionState.Idle
            } catch (c: CancellationException) {
                _state.value = CardReaderRemoteSessionState.Idle
                throw c
            } catch (t: Throwable) {
                logWrapper.e(LOG_TAG, "Session ended with error: ${t::class.java.name}: ${t.message}")
                _state.value = CardReaderRemoteSessionState.Error(message = t.toString())
            } finally {
                cleanupSync()
                if (sessionScope === scope) {
                    sessionScope = null
                }
            }
        }
    }

    fun stop() {
        val scope = sessionScope ?: return
        sessionScope = null
        scope.cancel()
        if (readerWasConnected) {
            readerWasConnected = false
            disconnectScope.launch {
                runCatching { cardReaderManager.disconnectReader() }
            }
        }
    }

    private suspend fun runSession() {
        _state.value = CardReaderRemoteSessionState.Starting

        val server = tlsServerFactory.create().also { tlsServer = it }
        server.start()

        val registration = nsdFactory.create(context).advertise(server.port, server.fingerprint)
        nsdRegistration = registration

        _state.value = readyToPairState(server)

        while (currentCoroutineContext().isActive) {
            try {
                acceptAndRunProtocolLoop(server)
                break
            } catch (e: SocketTimeoutException) {
                logWrapper.d(LOG_TAG, "Waiting for tablet to connect: ${e.message}")
            }
        }
    }

    private suspend fun acceptAndRunProtocolLoop(server: CardReaderRemoteTlsServer) {
        val accepted = server.acceptOne()
        connection = accepted

        val tokenProvider = remoteTokenProviderFactory.create()
        remoteTokenProvider = tokenProvider
        cardReaderManager.connectionTokenProvider.useRemote(tokenProvider)

        accepted.receive().collect { message ->
            handleMessage(message, accepted, tokenProvider)
        }
    }

    internal suspend fun handleMessage(
        message: CardReaderRemoteMessage,
        accepted: CardReaderRemoteConnection,
        tokenProvider: RemoteTokenChannelProvider,
    ) {
        when (message) {
            is ConnectRequest -> handleConnectRequest(message, accepted, tokenProvider)
            is CollectPaymentRequest -> handleCollectPaymentRequest(message, accepted)
            is ConnectAck,
            is PaymentIntentResult,
            is ErrorMessage -> Unit
        }
    }

    private suspend fun handleConnectRequest(
        request: ConnectRequest,
        accepted: CardReaderRemoteConnection,
        tokenProvider: RemoteTokenChannelProvider,
    ) = coroutineScope {
        val supplyJob = launch { tokenProvider.supply(request.connectionToken) }
        try {
            runCatching {
                val reader = discoverFirstTapToPayReader()
                cardReaderManager.startConnectionToReader(reader, request.locationId)
                reader
            }.onSuccess { reader ->
                readerWasConnected = true
                accepted.send(ConnectAck(requestId = request.requestId, readerSerial = reader.id))
                _state.value = CardReaderRemoteSessionState.WaitingForPayment(tabletName = null)
            }.onFailure { err ->
                accepted.send(
                    ErrorMessage(
                        requestId = request.requestId,
                        code = CODE_CONNECT_FAILED,
                        description = err.message.orEmpty(),
                    )
                )
                tlsServer?.let { _state.value = readyToPairState(it) }
            }
        } finally {
            supplyJob.cancel()
        }
    }

    private suspend fun discoverFirstTapToPayReader(): CardReader {
        val found = cardReaderManager.discoverReaders(
            isSimulated = false,
            cardReaderTypesToDiscover = CardReaderTypesToDiscover.SpecificReaders.BuiltInReaders(
                listOf(ReaderType.BuildInReader.TapToPayDevice)
            )
        ).filterIsInstance<CardReaderDiscoveryEvents.ReadersFound>()
            .first { it.list.isNotEmpty() }
        return found.list.first()
    }

    @Suppress("ForbiddenComment")
    private suspend fun handleCollectPaymentRequest(
        request: CollectPaymentRequest,
        accepted: CardReaderRemoteConnection,
    ) {
        // TODO: real Stripe retrieve/collect/confirm against request.paymentIntentClientSecret.
        //  Deferred as WOOMOB-2739-b until a cardreader-owned API is in place.
        accepted.send(
            ErrorMessage(
                requestId = request.requestId,
                code = CODE_NOT_IMPLEMENTED,
                description = "Payment collection deferred to WOOMOB-2739-b",
            )
        )
        _state.value = CardReaderRemoteSessionState.WaitingForPayment(tabletName = null)
    }

    private fun readyToPairState(server: CardReaderRemoteTlsServer) =
        CardReaderRemoteSessionState.ReadyToPair(
            deviceName = Build.MODEL ?: DEFAULT_DEVICE_NAME,
            fingerprintSuffix = CardReaderRemoteFingerprint.pairingCodeFromBase64(server.fingerprint),
        )

    private fun cleanupSync() {
        runCatching { connection?.close() }
        connection = null
        runCatching { cardReaderManager.connectionTokenProvider.useDefault() }
        runCatching { remoteTokenProvider?.close() }
        remoteTokenProvider = null
        runCatching { nsdRegistration?.close() }
        nsdRegistration = null
        runCatching { tlsServer?.close() }
        tlsServer = null
        if (readerWasConnected) {
            readerWasConnected = false
            disconnectScope.launch {
                runCatching { cardReaderManager.disconnectReader() }
            }
        }
    }

    internal fun interface TlsServerFactory {
        fun create(): CardReaderRemoteTlsServer
    }

    internal fun interface NsdFactory {
        fun create(context: Context): CardReaderRemoteNsd
    }

    internal fun interface RemoteTokenProviderFactory {
        fun create(): RemoteTokenChannelProvider
    }

    companion object {
        private const val LOG_TAG = "CardReaderRemoteSession"
        private const val CODE_CONNECT_FAILED = "connect_failed"
        private const val CODE_NOT_IMPLEMENTED = "not_implemented"
        private const val DEFAULT_DEVICE_NAME = "Android"

        private fun defaultDisconnectScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
