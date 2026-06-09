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
import com.woocommerce.android.cardreader.payments.CreatePaymentIntentResult
import com.woocommerce.android.cardreader.payments.PaymentInfo
import com.woocommerce.android.cardreader.payments.RetrieveAndCollectResult
import com.woocommerce.android.cardreader.payments.StatementDescriptor
import com.woocommerce.android.cardreader.remote.CardReaderRemoteMessage.CollectPaymentRequest
import com.woocommerce.android.cardreader.remote.CardReaderRemoteMessage.ConnectAck
import com.woocommerce.android.cardreader.remote.CardReaderRemoteMessage.ConnectRequest
import com.woocommerce.android.cardreader.remote.CardReaderRemoteMessage.ErrorMessage
import com.woocommerce.android.cardreader.remote.CardReaderRemoteMessage.PaymentIntentResult
import com.woocommerce.android.cardreader.remote.CardReaderRemoteMessage.Ping
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.SocketException
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
        tlsServerFactory = TlsServerFactory { CardReaderRemoteTlsServer(logWrapper) },
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
    private var useSimulatedReader: Boolean = false
    private var siteHash: String = ""
    private var deviceId: String = ""

    fun start(parentScope: CoroutineScope, siteHash: String, deviceId: String, isSimulated: Boolean = false) {
        useSimulatedReader = isSimulated
        this.siteHash = siteHash
        this.deviceId = deviceId
        startInternal(parentScope)
    }

    private fun startInternal(parentScope: CoroutineScope) {
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

        // Capture and clear transport before close so a racing start() can't have its new
        // resources clobbered by the old session's async cleanupSync().
        val nsd = nsdRegistration
        val tls = tlsServer
        nsdRegistration = null
        tlsServer = null

        scope.cancel()

        // Close synchronously: scope.cancel() does not interrupt the blocking socket.accept()
        // inside withContext(ioDispatcher), so without an explicit close NSD stays advertising
        // until ACCEPT_TIMEOUT_MILLIS expires.
        runCatching { nsd?.close() }
        runCatching { tls?.close() }

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

        val registration = nsdFactory.create(context)
            .advertise(server.port, server.fingerprint, deviceName(), siteHash, deviceId)
        nsdRegistration = registration

        logWrapper.d(
            LOG_TAG,
            "Advertising NSD fp=${server.fingerprint.takeLast(FINGERPRINT_LOG_SUFFIX_LENGTH)} pairingCode=" +
                CardReaderRemoteFingerprint.pairingCodeFromBase64(server.fingerprint)
        )

        _state.value = readyToPairState(server)

        while (currentCoroutineContext().isActive) {
            try {
                acceptAndRunProtocolLoop(server)
                logWrapper.d(LOG_TAG, "Tablet disconnected, resetting to ready-to-pair")
                cleanupConnectionOnly()
                _state.value = readyToPairState(server)
            } catch (e: SocketTimeoutException) {
                logWrapper.d(LOG_TAG, "Waiting for tablet to connect: ${e.message}")
            } catch (e: SocketException) {
                // Server socket was closed externally (typically stop() during shutdown).
                // Exit the loop gracefully so cleanup runs without an Error state.
                logWrapper.d(LOG_TAG, "Server socket closed: ${e.message}")
                return
            }
        }
    }

    private suspend fun acceptAndRunProtocolLoop(server: CardReaderRemoteTlsServer) = coroutineScope {
        logWrapper.d(LOG_TAG, "Waiting for TLS accept...")
        val accepted = server.acceptOne()
        logWrapper.d(LOG_TAG, "TLS accepted, starting protocol loop")
        connection = accepted

        val tokenProvider = remoteTokenProviderFactory.create()
        remoteTokenProvider = tokenProvider
        cardReaderManager.connectionTokenProvider.useRemote(tokenProvider)

        val pingJob = launchHeartbeat(accepted, logWrapper)
        try {
            accepted.receive().collect { message ->
                logWrapper.d(LOG_TAG, "Received message: ${message::class.java.simpleName}")
                handleMessage(message, accepted, tokenProvider)
            }
        } finally {
            pingJob.cancel()
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
            is Ping,
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
        logWrapper.d(LOG_TAG, "handleConnectRequest started")
        val supplyJob = launch { tokenProvider.supply(request.connectionToken) }
        try {
            runCatching {
                runCatching { cardReaderManager.disconnectReader() }
                val reader = discoverFirstTapToPayReader()
                logWrapper.d(LOG_TAG, "Discovered ${reader.id}, connecting...")
                cardReaderManager.startConnectionToReader(reader, request.locationId)
                logWrapper.d(LOG_TAG, "Connected to reader ${reader.id}")
                reader
            }.onSuccess { reader ->
                readerWasConnected = true
                logWrapper.d(LOG_TAG, "Sending ConnectAck...")
                accepted.send(ConnectAck(requestId = request.requestId, readerSerial = reader.id))
                logWrapper.d(LOG_TAG, "ConnectAck sent, transitioning to WaitingForPayment")
                _state.value = CardReaderRemoteSessionState.WaitingForPayment(tabletName = null)
            }.onFailure { err ->
                logWrapper.e(LOG_TAG, "Connect failed: ${err::class.java.simpleName}: ${err.message}")
                runCatching { cardReaderManager.disconnectReader() }
                accepted.send(
                    ErrorMessage(
                        requestId = request.requestId,
                        code = CODE_CONNECT_FAILED,
                        description = "${err::class.java.simpleName}: ${err.message.orEmpty()}",
                    )
                )
                tlsServer?.let { _state.value = readyToPairState(it) }
            }
        } finally {
            supplyJob.cancel()
        }
    }

    private suspend fun discoverFirstTapToPayReader(): CardReader {
        logWrapper.d(LOG_TAG, "Discovering TTP reader (isSimulated=$useSimulatedReader)")
        val config = CardReaderTypesToDiscover.SpecificReaders.BuiltInReaders(
            listOf(ReaderType.BuildInReader.TapToPayDevice)
        )
        return cardReaderManager.discoverReaders(useSimulatedReader, config)
            .mapNotNull { event ->
                when (event) {
                    is CardReaderDiscoveryEvents.ReadersFound -> event.list.firstOrNull()
                    is CardReaderDiscoveryEvents.Failed -> error(event.msg)
                    CardReaderDiscoveryEvents.Started,
                    CardReaderDiscoveryEvents.Succeeded -> null
                }
            }
            .first()
    }

    private suspend fun handleCollectPaymentRequest(
        request: CollectPaymentRequest,
        accepted: CardReaderRemoteConnection,
    ) {
        val paymentInfo = request.toPaymentInfo()
        when (val createResult = cardReaderManager.createPaymentIntent(paymentInfo)) {
            is CreatePaymentIntentResult.Success -> {
                when (
                    val collectResult = cardReaderManager.retrieveAndCollectPayment(
                        createResult.clientSecret,
                        paymentInfo
                    )
                ) {
                    is RetrieveAndCollectResult.Success -> accepted.send(
                        PaymentIntentResult(
                            requestId = request.requestId,
                            paymentIntentId = collectResult.paymentIntentId,
                            status = collectResult.status,
                        )
                    )
                    is RetrieveAndCollectResult.Failed -> {
                        logWrapper.e(LOG_TAG, "Collect payment failed: ${collectResult.cause.message}")
                        accepted.send(
                            ErrorMessage(
                                requestId = request.requestId,
                                code = CODE_COLLECT_FAILED,
                                description = collectResult.cause.message.orEmpty(),
                            )
                        )
                    }
                }
            }
            is CreatePaymentIntentResult.Failed -> {
                logWrapper.e(LOG_TAG, "Create payment intent failed: ${createResult.cause.message}")
                accepted.send(
                    ErrorMessage(
                        requestId = request.requestId,
                        code = CODE_CREATE_INTENT_FAILED,
                        description = createResult.cause.message.orEmpty(),
                    )
                )
            }
        }
        _state.value = CardReaderRemoteSessionState.WaitingForPayment(tabletName = null)
    }

    private fun CollectPaymentRequest.toPaymentInfo(): PaymentInfo = PaymentInfo(
        paymentDescription = paymentDescription,
        statementDescriptor = StatementDescriptor(statementDescriptorRaw),
        orderId = orderId,
        amount = amount,
        currency = currency,
        customerEmail = customerEmail,
        isPluginCanSendReceipt = isPluginCanSendReceipt,
        customerName = customerName,
        storeName = storeName,
        siteUrl = siteUrl,
        orderKey = orderKey,
        feeAmount = feeAmount,
        channel = PaymentInfo.PaymentChannel.Pos,
        cardPresentCaptureMethod = cardPresentCaptureMethod,
        terminalPaymentPreparation = terminalPaymentPreparation ?: PaymentInfo.TerminalPaymentPreparation.NONE,
        countryCode = countryCode,
    )

    private fun readyToPairState(server: CardReaderRemoteTlsServer) =
        CardReaderRemoteSessionState.ReadyToPair(
            deviceName = deviceName(),
            fingerprintSuffix = CardReaderRemoteFingerprint.pairingCodeFromBase64(server.fingerprint),
        )

    private fun deviceName(): String = Build.MODEL ?: DEFAULT_DEVICE_NAME

    private suspend fun cleanupConnectionOnly() {
        runCatching { connection?.close() }
        connection = null
        runCatching { cardReaderManager.connectionTokenProvider.useDefault() }
        runCatching { remoteTokenProvider?.close() }
        remoteTokenProvider = null
        if (readerWasConnected) {
            readerWasConnected = false
            runCatching { cardReaderManager.disconnectReader() }
        }
    }

    private fun cleanupSync() {
        // Capture locals first: a racing start() may have already overwritten the fields with
        // new resources, and we must close only what this session owned.
        val conn = connection
        val tokenProv = remoteTokenProvider
        val nsd = nsdRegistration
        val tls = tlsServer
        connection = null
        remoteTokenProvider = null
        nsdRegistration = null
        tlsServer = null

        runCatching { conn?.close() }
        runCatching { cardReaderManager.connectionTokenProvider.useDefault() }
        runCatching { tokenProv?.close() }
        runCatching { nsd?.close() }
        runCatching { tls?.close() }
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
        private const val CODE_COLLECT_FAILED = "collect_failed"
        private const val CODE_CREATE_INTENT_FAILED = "create_intent_failed"
        private const val DEFAULT_DEVICE_NAME = "Android"
        private const val FINGERPRINT_LOG_SUFFIX_LENGTH = 8

        private fun defaultDisconnectScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
