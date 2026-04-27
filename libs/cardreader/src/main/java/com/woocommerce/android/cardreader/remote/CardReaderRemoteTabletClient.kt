package com.woocommerce.android.cardreader.remote

import android.util.Log
import com.woocommerce.android.cardreader.payments.PaymentInfo
import com.woocommerce.android.cardreader.remote.CardReaderRemoteMessage.CollectPaymentRequest
import com.woocommerce.android.cardreader.remote.CardReaderRemoteMessage.ConnectAck
import com.woocommerce.android.cardreader.remote.CardReaderRemoteMessage.ConnectRequest
import com.woocommerce.android.cardreader.remote.CardReaderRemoteMessage.ErrorMessage
import com.woocommerce.android.cardreader.remote.CardReaderRemoteMessage.PaymentIntentResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import java.util.UUID

interface CardReaderRemoteTabletClient {
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

        fun create(): CardReaderRemoteTabletClient =
            DefaultCardReaderRemoteTabletClient(CardReaderRemoteTlsClient())
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
) : CardReaderRemoteTabletClient {
    private var connection: CardReaderRemoteConnection? = null

    override suspend fun connect(
        reader: DiscoveredRemoteReader,
        connectionToken: String,
        locationId: String,
        timeoutMillis: Long,
    ): ConnectOutcome {
        disconnect()
        return try {
            withTimeout(timeoutMillis) {
                Log.d(TAG, "Opening TLS connection")
                val opened = tlsClient.connect(reader.host, reader.port, reader.fingerprintBase64)
                connection = opened
                val requestId = UUID.randomUUID().toString()
                Log.d(TAG, "Sending ConnectRequest requestId=$requestId")
                opened.send(ConnectRequest(requestId, connectionToken, locationId))
                Log.d(TAG, "ConnectRequest sent, awaiting reply")
                when (val reply = opened.receive().first { it.requestId == requestId }) {
                    is ConnectAck -> ConnectOutcome.Success(reply.readerSerial)
                    is ErrorMessage -> ConnectOutcome.Rejected(reply.code, reply.description)
                    is ConnectRequest,
                    is CollectPaymentRequest,
                    is PaymentIntentResult -> ConnectOutcome.Rejected(
                        CODE_UNEXPECTED_REPLY,
                        "Unexpected reply type: ${reply::class.simpleName}",
                    )
                }
            }
        } catch (@Suppress("SwallowedException") timeout: TimeoutCancellationException) {
            disconnect()
            ConnectOutcome.Failed(IllegalStateException("Timed out connecting to remote reader"))
        } catch (cancel: CancellationException) {
            disconnect()
            throw cancel
        } catch (@Suppress("TooGenericExceptionCaught") cause: Throwable) {
            disconnect()
            ConnectOutcome.Failed(cause)
        }
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
        } catch (@Suppress("TooGenericExceptionCaught") cause: Throwable) {
            CollectPaymentOutcome.Failed(cause)
        }
    }

    override fun disconnect() {
        connection?.close()
        connection = null
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
