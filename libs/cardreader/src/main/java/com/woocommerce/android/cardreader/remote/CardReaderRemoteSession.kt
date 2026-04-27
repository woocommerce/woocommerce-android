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

    fun start(parentScope: CoroutineScope, isSimulated: Boolean = false) {
        useSimulatedReader = isSimulated
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

        val registration = nsdFactory.create(context)
            .advertise(server.port, server.fingerprint, deviceName())
        nsdRegistration = registration

        logWrapper.d(
            LOG_TAG,
            "Advertising NSD fp=${server.fingerprint} pairingCode=" +
                CardReaderRemoteFingerprint.pairingCodeFromBase64(server.fingerprint)
        )

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
        logWrapper.d(LOG_TAG, "Waiting for TLS accept...")
        val accepted = server.acceptOne()
        logWrapper.d(LOG_TAG, "TLS accepted, starting protocol loop")
        connection = accepted

        val tokenProvider = remoteTokenProviderFactory.create()
        remoteTokenProvider = tokenProvider
        cardReaderManager.connectionTokenProvider.useRemote(tokenProvider)

        accepted.receive().collect { message ->
            logWrapper.d(LOG_TAG, "Received message: ${message::class.java.simpleName}")
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
        logWrapper.d(LOG_TAG, "handleConnectRequest started")
        val supplyJob = launch { tokenProvider.supply(request.connectionToken) }
        try {
            runCatching {
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
                when (val collectResult = cardReaderManager.retrieveAndCollectPayment(createResult.clientSecret)) {
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
        countryCode = countryCode,
    )

    private fun readyToPairState(server: CardReaderRemoteTlsServer) =
        CardReaderRemoteSessionState.ReadyToPair(
            deviceName = deviceName(),
            fingerprintSuffix = CardReaderRemoteFingerprint.pairingCodeFromBase64(server.fingerprint),
        )

    private fun deviceName(): String = Build.MODEL ?: DEFAULT_DEVICE_NAME

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
        private const val CODE_COLLECT_FAILED = "collect_failed"
        private const val CODE_CREATE_INTENT_FAILED = "create_intent_failed"
        private const val DEFAULT_DEVICE_NAME = "Android"

        private fun defaultDisconnectScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
