package com.woocommerce.android.ui.readermode

import android.content.Context
import android.os.Build
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.connection.CardReaderTypesToDiscover
import com.woocommerce.android.cardreader.connection.ReaderType
import com.woocommerce.android.cardreader.connection.RemoteTokenChannelProvider
import com.woocommerce.android.cardreader.remote.NsdRegistration
import com.woocommerce.android.cardreader.remote.RemoteReaderConnection
import com.woocommerce.android.cardreader.remote.RemoteReaderFingerprint
import com.woocommerce.android.cardreader.remote.RemoteReaderMessage
import com.woocommerce.android.cardreader.remote.RemoteReaderMessage.CollectPaymentRequest
import com.woocommerce.android.cardreader.remote.RemoteReaderMessage.ConnectAck
import com.woocommerce.android.cardreader.remote.RemoteReaderMessage.ConnectRequest
import com.woocommerce.android.cardreader.remote.RemoteReaderMessage.ErrorMessage
import com.woocommerce.android.cardreader.remote.RemoteReaderMessage.PaymentIntentResult
import com.woocommerce.android.cardreader.remote.RemoteReaderNsd
import com.woocommerce.android.cardreader.remote.RemoteReaderTlsServer
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayReadyToPair
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayWaitingForPayment
import com.woocommerce.android.ui.payments.cardreader.payment.remote.ExitCardReaderMode
import com.woocommerce.android.ui.payments.cardreader.payment.remote.RemoteTapToPayReaderStateBridge
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.CARD_READER
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardReaderModeSession internal constructor(
    @ApplicationContext private val appContext: Context,
    private val cardReaderManager: CardReaderManager,
    private val bridge: RemoteTapToPayReaderStateBridge,
    private val tlsServerFactory: TlsServerFactory,
    private val nsdFactory: NsdFactory,
    private val remoteTokenProviderFactory: RemoteTokenProviderFactory,
    private val disconnectScope: CoroutineScope = defaultDisconnectScope(),
) {
    @Inject
    constructor(
        @ApplicationContext appContext: Context,
        cardReaderManager: CardReaderManager,
        bridge: RemoteTapToPayReaderStateBridge,
    ) : this(
        appContext = appContext,
        cardReaderManager = cardReaderManager,
        bridge = bridge,
        tlsServerFactory = TlsServerFactory { RemoteReaderTlsServer() },
        nsdFactory = NsdFactory { context -> RemoteReaderNsd(context) },
        remoteTokenProviderFactory = RemoteTokenProviderFactory { RemoteTokenChannelProvider() },
    )

    private var sessionScope: CoroutineScope? = null
    private var tlsServer: RemoteReaderTlsServer? = null
    private var nsdRegistration: NsdRegistration? = null
    private var remoteTokenProvider: RemoteTokenChannelProvider? = null
    private var connection: RemoteReaderConnection? = null
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
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                WooLog.e(CARD_READER, "Card reader mode session ended with error", t)
            } finally {
                cleanupSync()
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
        val server = tlsServerFactory.create().also { tlsServer = it }
        server.start()

        val registration = nsdFactory.create(appContext).advertise(server.port, server.fingerprint)
        nsdRegistration = registration

        pushReadyToPair(server)

        acceptAndRunProtocolLoop(server)
    }

    private suspend fun acceptAndRunProtocolLoop(server: RemoteReaderTlsServer) {
        val accepted = server.acceptOne()
        connection = accepted

        val tokenProvider = remoteTokenProviderFactory.create()
        remoteTokenProvider = tokenProvider
        cardReaderManager.connectionTokenProvider.use(tokenProvider)

        accepted.receive().collect { message ->
            handleMessage(message, accepted, tokenProvider)
        }
    }

    internal suspend fun handleMessage(
        message: RemoteReaderMessage,
        accepted: RemoteReaderConnection,
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
        accepted: RemoteReaderConnection,
        tokenProvider: RemoteTokenChannelProvider,
    ) = coroutineScope {
        // supply() suspends on a rendezvous channel until the Stripe SDK calls fetchConnectionToken
        // during startConnectionToReader; launch it so the outer coroutine can proceed.
        val supplyJob = launch { tokenProvider.supply(request.connectionToken) }
        try {
            runCatching {
                val reader = discoverFirstTapToPayReader()
                cardReaderManager.startConnectionToReader(reader, request.locationId)
                reader
            }.onSuccess { reader ->
                readerWasConnected = true
                accepted.send(ConnectAck(requestId = request.requestId, readerSerial = reader.id))
                bridge.push(
                    RemoteTapToPayWaitingForPayment(
                        tabletName = null,
                        onPrimaryActionClicked = { bridge.emitEvent(ExitCardReaderMode) },
                    )
                )
            }.onFailure { err ->
                accepted.send(
                    ErrorMessage(
                        requestId = request.requestId,
                        code = CODE_CONNECT_FAILED,
                        description = err.message.orEmpty(),
                    )
                )
                tlsServer?.let { pushReadyToPair(it) }
            }
        } finally {
            supplyJob.cancel()
        }
    }

    private fun pushReadyToPair(server: RemoteReaderTlsServer) {
        bridge.push(
            RemoteTapToPayReadyToPair(
                deviceName = Build.MODEL ?: DEFAULT_DEVICE_NAME,
                fingerprintSuffix = RemoteReaderFingerprint.suffix4FromBase64(server.fingerprint),
                onPrimaryActionClicked = { bridge.emitEvent(ExitCardReaderMode) },
            )
        )
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

    private suspend fun handleCollectPaymentRequest(
        request: CollectPaymentRequest,
        accepted: RemoteReaderConnection,
    ) {
        // TODO(WOOMOB-2739-b): Implement Stripe Terminal retrieve/collect/confirm in cardreader library
        //  and expose a suspending API on CardReaderManager. The Stripe SDK is not on the main app's
        //  classpath; wiring it here requires either adding the dependency or a new cardreader wrapper.
        accepted.send(
            ErrorMessage(
                requestId = request.requestId,
                code = CODE_NOT_IMPLEMENTED,
                description = "Payment collection deferred to WOOMOB-2739-b",
            )
        )
        bridge.push(
            RemoteTapToPayWaitingForPayment(
                tabletName = null,
                onPrimaryActionClicked = { bridge.emitEvent(ExitCardReaderMode) },
            )
        )
    }

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
        runCatching { bridge.clear() }
    }

    fun interface TlsServerFactory {
        fun create(): RemoteReaderTlsServer
    }

    fun interface NsdFactory {
        fun create(context: Context): RemoteReaderNsd
    }

    fun interface RemoteTokenProviderFactory {
        fun create(): RemoteTokenChannelProvider
    }

    companion object {
        private const val CODE_CONNECT_FAILED = "connect_failed"
        private const val CODE_NOT_IMPLEMENTED = "not_implemented"
        private const val DEFAULT_DEVICE_NAME = "Android"

        private fun defaultDisconnectScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
