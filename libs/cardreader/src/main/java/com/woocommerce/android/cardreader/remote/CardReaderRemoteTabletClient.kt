package com.woocommerce.android.cardreader.remote

import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.payments.PaymentInfo
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
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID

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

    fun disconnect()

    companion object {
        const val DEFAULT_COLLECT_PAYMENT_TIMEOUT_MILLIS: Long = 90_000
        const val DEFAULT_CONNECT_TIMEOUT_MILLIS: Long = 30_000

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
    data class Rejected(val code: String, val description: String) : ConnectOutcome()
    data class Failed(val cause: Throwable) : ConnectOutcome()
}

sealed class CollectPaymentOutcome {
    data class Success(val paymentIntentId: String, val status: String) : CollectPaymentOutcome()
    data class Rejected(val code: String, val description: String) : CollectPaymentOutcome()
    data object TimedOut : CollectPaymentOutcome()
    data class Failed(val cause: Throwable) : CollectPaymentOutcome()
}

internal class DefaultCardReaderRemoteTabletClient(
    private val tlsClient: CardReaderRemoteTlsClient,
    private val logWrapper: LogWrapper,
    private val scope: CoroutineScope,
) : CardReaderRemoteTabletClient {
    private var connection: CardReaderRemoteConnection? = null
    private var closedBridgeJob: Job? = null
    private var heartbeatJob: Job? = null
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
                val requestId = UUID.randomUUID().toString()
                logWrapper.d(TAG, "Sending ConnectRequest requestId=$requestId")
                opened.send(ConnectRequest(requestId, connectionToken, locationId))
                logWrapper.d(TAG, "ConnectRequest sent, awaiting reply")
                when (val reply = opened.receive().first { it.requestId == requestId }) {
                    is ConnectAck -> {
                        bridgeClosedSignal(opened)
                        startHeartbeat(opened)
                        ConnectOutcome.Success(reply.readerSerial)
                    }
                    is ErrorMessage -> {
                        disconnect()
                        ConnectOutcome.Rejected(reply.code, reply.description)
                    }
                    is ConnectRequest,
                    is CollectPaymentRequest,
                    is Ping,
                    is PaymentIntentResult -> {
                        disconnect()
                        ConnectOutcome.Rejected(
                            CODE_UNEXPECTED_REPLY,
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
        } catch (@Suppress("TooGenericExceptionCaught") cause: Exception) {
            disconnect()
            ConnectOutcome.Failed(cause)
        }
    }

    private fun bridgeClosedSignal(connection: CardReaderRemoteConnection) {
        closedBridgeJob?.cancel()
        closedBridgeJob = scope.launch {
            connection.closed.collect { isClosed ->
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
        return try {
            val requestId = UUID.randomUUID().toString()
            active.send(paymentInfo.toCollectPaymentRequest(requestId))
            val reply = withTimeout(timeoutMillis) {
                active.receive().first { it.requestId == requestId }
            }
            when (reply) {
                is PaymentIntentResult -> CollectPaymentOutcome.Success(reply.paymentIntentId, reply.status)
                is ErrorMessage -> CollectPaymentOutcome.Rejected(reply.code, reply.description)
                is ConnectAck,
                is ConnectRequest,
                is Ping,
                is CollectPaymentRequest -> CollectPaymentOutcome.Rejected(
                    CODE_UNEXPECTED_REPLY,
                    "Unexpected reply type: ${reply::class.simpleName}",
                )
            }
        } catch (@Suppress("SwallowedException") timeout: TimeoutCancellationException) {
            disconnect()
            CollectPaymentOutcome.TimedOut
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (@Suppress("TooGenericExceptionCaught") cause: Exception) {
            CollectPaymentOutcome.Failed(cause)
        }
    }

    override fun disconnect() {
        closedBridgeJob?.cancel()
        closedBridgeJob = null
        heartbeatJob?.cancel()
        heartbeatJob = null
        connection?.close()
        connection = null
        _connectionClosed.value = true
    }

    private companion object {
        const val CODE_UNEXPECTED_REPLY = "unexpected_reply"
        const val TAG = "CardReaderRemoteTabletClient"
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
    )
