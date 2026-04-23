package com.woocommerce.android.ui.woopos.cardreader.remote

import com.woocommerce.android.cardreader.CardReaderStore
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse
import com.woocommerce.android.cardreader.remote.CollectPaymentOutcome
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosRemoteReaderPaymentFlow @Inject constructor(
    private val session: WooPosRemoteReaderSession,
    private val cardReaderStore: CardReaderStore,
    private val logger: WooPosLogWrapper,
) {
    suspend fun collectAndCapture(
        paymentIntentClientSecret: String,
        orderId: Long,
    ): Result {
        if (session.state.value !is WooPosRemoteReaderSession.State.Connected) {
            return Result.NotConnected
        }

        val result = when (val outcome = session.sendCollectPayment(paymentIntentClientSecret)) {
            is CollectPaymentOutcome.Success -> when (outcome.status) {
                STATUS_REQUIRES_CAPTURE -> capture(orderId, outcome.paymentIntentId)
                else -> Result.UnexpectedStatus(outcome.status)
            }
            is CollectPaymentOutcome.Rejected,
            is CollectPaymentOutcome.Failed,
            CollectPaymentOutcome.TimedOut -> Result.CollectFailed(outcome)
        }
        logger.d("Remote payment result: $result")
        return result
    }

    private suspend fun capture(orderId: Long, paymentIntentId: String): Result {
        val response = runCatching { cardReaderStore.capturePaymentIntent(orderId, paymentIntentId) }
            .getOrElse { return Result.CaptureFailed(it.message ?: "Capture threw") }
        return when (response) {
            is CapturePaymentResponse.Successful -> Result.Captured(paymentIntentId)
            is CapturePaymentResponse.Error -> Result.CaptureFailed(response.message)
        }
    }

    sealed class Result {
        data class Captured(val paymentIntentId: String) : Result()
        data object NotConnected : Result()
        data class UnexpectedStatus(val status: String) : Result()
        data class CollectFailed(val outcome: CollectPaymentOutcome) : Result()
        data class CaptureFailed(val message: String) : Result()
    }

    private companion object {
        const val STATUS_REQUIRES_CAPTURE = "requires_capture"
    }
}
