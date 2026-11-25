package com.woocommerce.android.cardreader.remote

import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.PaymentMethodType
import com.woocommerce.android.cardreader.payments.PaymentInfo
import com.woocommerce.android.cardreader.remote.CardReaderRemoteMessage.CancelPayment
import com.woocommerce.android.cardreader.remote.CardReaderRemoteMessage.CancelPaymentResult
import com.woocommerce.android.cardreader.remote.CardReaderRemoteMessage.CollectPaymentRequest
import com.woocommerce.android.cardreader.remote.CardReaderRemoteMessage.ConnectAck
import com.woocommerce.android.cardreader.remote.CardReaderRemoteMessage.ConnectRequest
import com.woocommerce.android.cardreader.remote.CardReaderRemoteMessage.ErrorMessage
import com.woocommerce.android.cardreader.remote.CardReaderRemoteMessage.PaymentIntentResult
import com.woocommerce.android.cardreader.remote.CardReaderRemoteMessage.Ping
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

interface CardReaderRemoteTabletClient {
    val connectionClosed: StateFlow<Boolean>

    suspend fun connect(
        reader: DiscoveredRemoteReader,
        connectionToken: String,
        locationId: String,
        timeoutMillis: Long = DEFAULT_CONNECT_TIMEOUT_MILLIS,
    ): ConnectOutcome

    suspend fun collectPayment(
        paymentInfo: PaymentInfo,
        timeoutMillis: Long = DEFAULT_COLLECT_PAYMENT_TIMEOUT_MILLIS,
    ): CollectPaymentOutcome

    suspend fun cancelPayment(timeoutMillis: Long = DEFAULT_CANCEL_TIMEOUT_MILLIS): RemotePaymentCancellation

    fun disconnect()

    companion object {
        const val DEFAULT_COLLECT_PAYMENT_TIMEOUT_MILLIS: Long = 90_000
        const val DEFAULT_CONNECT_TIMEOUT_MILLIS: Long = 30_000
        const val DEFAULT_CANCEL_TIMEOUT_MILLIS: Long = 30_000

        fun create(logWrapper: LogWrapper): CardReaderRemoteTabletClient =
            DefaultCardReaderRemoteTabletClient(
                tlsClient = CardReaderRemoteTlsClient(logWrapper),
                logWrapper = logWrapper,
                scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            )
    }
}

sealed class ConnectOutcome {
    data class Success(val readerSerial: String?) : ConnectOutcome()
    data class Rejected(val error: CardReaderRemoteError, val description: String) : ConnectOutcome()
    data class Failed(val cause: Throwable) : ConnectOutcome()
}

sealed class CollectPaymentOutcome {
    data class Success(
        val paymentIntentId: String,
        val status: String,
        val paymentMethodType: PaymentMethodType?,
    ) : CollectPaymentOutcome()
    data class Rejected(
        val error: CardReaderRemoteError,
        val description: String,
        val stripeErrorCode: String? = null,
    ) : CollectPaymentOutcome()
    data object TimedOut : CollectPaymentOutcome()
    data class Failed(val cause: Throwable) : CollectPaymentOutcome()
}

enum class RemotePaymentCancellation { CONFIRMED, FAILED, UNCONFIRMED, UNSUPPORTED }

internal class DefaultCardReaderRemoteTabletClient(
    private val tlsClient: CardReaderRemoteTlsClient,
    private val logWrapper: LogWrapper,
    private val scope: CoroutineScope,
) : CardReaderRemoteTabletClient {
    private var connection: CardReaderRemoteConnection? = null
    private var closedBridgeJob: Job? = null
    private var heartbeatJob: Job? = null
    private var phoneSupportsCancelPayment = false
    private var phoneSupportsConfirmedCancellation = false
    private var replies: ReplyChannels? = null
    private var lastCollection: Pair<CardReaderRemoteConnection, String>? = null
    private val _connectionClosed = MutableStateFlow(true)
    override val connectionClosed: StateFlow<Boolean> = _connectionClosed.asStateFlow()

    override suspend fun connect(
        reader: DiscoveredRemoteReader,
        connectionToken: String,
        locationId: String,
        timeoutMillis: Long,
    ): ConnectOutcome {
        disconnect()
        _connectionClosed.value = false
        return try {
            withTimeout(timeoutMillis) {
                logWrapper.d(TAG, "Opening TLS connection")
                val opened = tlsClient.connect(reader.host, reader.port, reader.fingerprintBase64)
                connection = opened
                startReplyPump(opened)
                val requestId = UUID.randomUUID().toString()
                logWrapper.d(TAG, "Sending ConnectRequest requestId=$requestId")
                val reply = requestReply(opened, ConnectRequest(requestId, connectionToken, locationId))
                when (reply) {
                    is ConnectAck -> {
                        phoneSupportsCancelPayment = reply.supportsCancelPayment
                        phoneSupportsConfirmedCancellation = reply.supportsConfirmedCancellation
                        bridgeClosedSignal(opened)
                        startHeartbeat(opened)
                        ConnectOutcome.Success(reply.readerSerial)
                    }
                    is ErrorMessage -> {
                        disconnect()
                        ConnectOutcome.Rejected(CardReaderRemoteError.fromCode(reply.code), reply.description)
                    }
                    is CancelPaymentResult,
                    is CancelPayment,
                    is ConnectRequest,
                    is CollectPaymentRequest,
                    is Ping,
                    is PaymentIntentResult -> {
                        disconnect()
                        ConnectOutcome.Rejected(
                            CardReaderRemoteError.UnexpectedReply,
                            "Unexpected reply type: ${reply::class.simpleName}",
                        )
                    }
                }
            }
        } catch (@Suppress("SwallowedException") timeout: TimeoutCancellationException) {
            disconnect()
            ConnectOutcome.Failed(IllegalStateException("Timed out connecting to remote reader"))
        } catch (cancel: CancellationException) {
            disconnect()
            throw cancel
        } catch (cause: IOException) {
            disconnect()
            ConnectOutcome.Failed(IllegalStateException(CONNECT_FAILED_MESSAGE, cause))
        } catch (@Suppress("TooGenericExceptionCaught") cause: Exception) {
            disconnect()
            ConnectOutcome.Failed(mapToConnectFailedIfIo(cause))
        }
    }

    private fun mapToConnectFailedIfIo(cause: Exception): Exception =
        if (cause is IOException || cause is CardReaderRemoteConnectionLostException) {
            IllegalStateException(CONNECT_FAILED_MESSAGE, cause)
        } else {
            cause
        }

    private fun bridgeClosedSignal(connection: CardReaderRemoteConnection) {
        closedBridgeJob?.cancel()
        closedBridgeJob = scope.launch {
            connection.closed.collect { isClosed ->
                if (this@DefaultCardReaderRemoteTabletClient.connection !== connection) return@collect
                _connectionClosed.value = isClosed
                if (isClosed) {
                    heartbeatJob?.cancel()
                    heartbeatJob = null
                    this@DefaultCardReaderRemoteTabletClient.connection = null
                }
            }
        }
    }

    private fun startHeartbeat(connection: CardReaderRemoteConnection) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launchHeartbeat(connection, logWrapper)
    }

    override suspend fun collectPayment(
        paymentInfo: PaymentInfo,
        timeoutMillis: Long,
    ): CollectPaymentOutcome {
        val active = connection ?: return CollectPaymentOutcome.Failed(
            IllegalStateException("Not connected to a remote reader"),
        )
        val requestId = UUID.randomUUID().toString()
        lastCollection = active to requestId
        return try {
            val reply = withTimeout(timeoutMillis) {
                requestReply(active, paymentInfo.toCollectPaymentRequest(requestId))
            }
            when (reply) {
                is PaymentIntentResult -> CollectPaymentOutcome.Success(
                    reply.paymentIntentId,
                    reply.status,
                    reply.paymentMethodType
                )
                is ErrorMessage -> CollectPaymentOutcome.Rejected(
                    error = CardReaderRemoteError.fromCode(reply.code),
                    description = reply.description,
                    stripeErrorCode = reply.stripeErrorCode,
                )
                is CancelPaymentResult,
                is CancelPayment,
                is ConnectAck,
                is ConnectRequest,
                is Ping,
                is CollectPaymentRequest -> CollectPaymentOutcome.Rejected(
                    CardReaderRemoteError.UnexpectedReply,
                    "Unexpected reply type: ${reply::class.simpleName}",
                )
            }
        } catch (@Suppress("SwallowedException") timeout: TimeoutCancellationException) {
            disconnect()
            CollectPaymentOutcome.TimedOut
        } catch (cancel: CancellationException) {
            cancelOnPhone(active, requestId)
            throw cancel
        } catch (@Suppress("TooGenericExceptionCaught") cause: Exception) {
            CollectPaymentOutcome.Failed(cause)
        }
    }

    override suspend fun cancelPayment(timeoutMillis: Long): RemotePaymentCancellation {
        val (active, requestId) = lastCollection ?: return RemotePaymentCancellation.UNCONFIRMED
        val channels = replies?.takeIf { it.connection === active }
            ?: return RemotePaymentCancellation.UNCONFIRMED
        if (!phoneSupportsConfirmedCancellation) {
            cancelOnPhone(active, requestId)
            return RemotePaymentCancellation.UNSUPPORTED
        }
        val cancellationId = UUID.randomUUID().toString()
        val response = CompletableDeferred<CancelPaymentResult>()
        channels.cancellations[cancellationId] = response
        val completion = channels.job?.invokeOnCompletion {
            response.completeExceptionally(CardReaderRemoteConnectionLostException(it))
        }
        return try {
            active.send(CancelPayment(requestId, cancellationId))
            val reply = withTimeout(timeoutMillis) { response.await() }
            if (reply.cancelled) RemotePaymentCancellation.CONFIRMED else RemotePaymentCancellation.FAILED
        } catch (@Suppress("SwallowedException") timeout: TimeoutCancellationException) {
            RemotePaymentCancellation.UNCONFIRMED
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") error: Exception) {
            RemotePaymentCancellation.UNCONFIRMED
        } finally {
            completion?.dispose()
            channels.cancellations.remove(cancellationId, response)
        }
    }

    private fun startReplyPump(active: CardReaderRemoteConnection) {
        val channels = ReplyChannels(active)
        replies = channels
        channels.job = scope.launch {
            try {
                active.receive().collect { reply ->
                    if (reply is CancelPaymentResult) {
                        channels.cancellations[reply.cancellationId]?.complete(reply)
                    } else {
                        channels.payments[reply.requestId]?.complete(reply)
                    }
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") error: Exception) {
                logWrapper.w(TAG, "Remote reply stream failed: ${error::class.java.simpleName}")
            }
        }
    }

    private suspend fun requestReply(
        active: CardReaderRemoteConnection,
        request: CardReaderRemoteMessage,
    ): CardReaderRemoteMessage {
        val channels = replies?.takeIf { it.connection === active }
            ?: throw CardReaderRemoteConnectionLostException(null)
        val response = CompletableDeferred<CardReaderRemoteMessage>()
        channels.payments[request.requestId] = response
        val completion = channels.job?.invokeOnCompletion {
            response.completeExceptionally(CardReaderRemoteConnectionLostException(it))
        }
        return try {
            active.send(request)
            response.await()
        } finally {
            completion?.dispose()
            channels.payments.remove(request.requestId, response)
        }
    }

    private class ReplyChannels(val connection: CardReaderRemoteConnection) {
        val payments = ConcurrentHashMap<String, CompletableDeferred<CardReaderRemoteMessage>>()
        val cancellations = ConcurrentHashMap<String, CompletableDeferred<CancelPaymentResult>>()
        var job: Job? = null
    }

    private fun cancelOnPhone(active: CardReaderRemoteConnection, requestId: String) {
        if (!phoneSupportsCancelPayment) return
        scope.launch {
            runCatching { active.send(CancelPayment(requestId)) }
                .onFailure { logWrapper.w(TAG, "Could not cancel the collect: ${it::class.java.simpleName}") }
        }
    }

    override fun disconnect() {
        closedBridgeJob?.cancel()
        closedBridgeJob = null
        heartbeatJob?.cancel()
        heartbeatJob = null
        connection?.close()
        replies?.job?.cancel()
        replies = null
        lastCollection = null
        connection = null
        _connectionClosed.value = true
    }

    private companion object {
        const val TAG = "CardReaderRemoteTabletClient"

        // Reported while establishing a session - the reader was never connected.
        const val CONNECT_FAILED_MESSAGE = "Could not connect to phone reader"
    }
}

private fun PaymentInfo.toCollectPaymentRequest(requestId: String): CollectPaymentRequest =
    CollectPaymentRequest(
        requestId = requestId,
        paymentDescription = paymentDescription,
        statementDescriptorRaw = statementDescriptor.value,
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
        countryCode = countryCode,
        cardPresentCaptureMethod = cardPresentCaptureMethod,
        terminalPaymentPreparation = terminalPaymentPreparation,
    )
