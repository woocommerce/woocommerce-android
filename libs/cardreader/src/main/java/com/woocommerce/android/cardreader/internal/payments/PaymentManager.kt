package com.woocommerce.android.cardreader.internal.payments

import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.PaymentIntentStatus
import com.stripe.stripeterminal.external.models.PaymentIntentStatus.CANCELED
import com.woocommerce.android.cardreader.CardReaderStore
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse
import com.woocommerce.android.cardreader.internal.payments.actions.CancelPaymentAction
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction.CollectPaymentStatus
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction.CreatePaymentStatus.Failure
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction.CreatePaymentStatus.Success
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessPaymentAction
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessPaymentAction.ProcessPaymentStatus
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import com.woocommerce.android.cardreader.payments.CardPaymentStatus
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.*
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.Generic
import com.woocommerce.android.cardreader.payments.PaymentData
import com.woocommerce.android.cardreader.payments.PaymentInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

@Suppress("LongParameterList")
internal class PaymentManager(
    private val terminalWrapper: TerminalWrapper,
    private val cardReaderStore: CardReaderStore,
    private val createPaymentAction: CreatePaymentAction,
    private val collectPaymentAction: CollectPaymentAction,
    private val processPaymentAction: ProcessPaymentAction,
    private val cancelPaymentAction: CancelPaymentAction,
    private val paymentUtils: PaymentUtils,
    private val errorMapper: PaymentErrorMapper,
) {
    suspend fun acceptPayment(paymentInfo: PaymentInfo): Flow<CardPaymentStatus> = flow {
        if (isInvalidState(paymentInfo)) return@flow

        val paymentIntent = createPaymentIntent(paymentInfo)
        if (paymentIntent?.status != PaymentIntentStatus.REQUIRES_PAYMENT_METHOD) {
            return@flow
        }
        processPaymentIntent(paymentInfo.orderId, paymentIntent).collect { emit(it) }
    }

    fun retryPayment(orderId: Long, paymentData: PaymentData) =
        processPaymentIntent(orderId, (paymentData as PaymentDataImpl).paymentIntent)

    fun cancelPayment(paymentData: PaymentData) {
        val paymentIntent = (paymentData as PaymentDataImpl).paymentIntent
        /* If the paymentIntent is in REQUIRES_CAPTURE state the app should not cancel the payment intent as it
        doesn't know if it was already captured or not during one of the previous attempts to capture it. */
        if (paymentIntent.status == PaymentIntentStatus.REQUIRES_PAYMENT_METHOD ||
            paymentIntent.status == PaymentIntentStatus.REQUIRES_CONFIRMATION
        ) {
            cancelPaymentAction.cancelPayment(paymentIntent)
        }
    }

    private fun processPaymentIntent(orderId: Long, data: PaymentIntent) = flow {
        var paymentIntent = data
        if (paymentIntent.status == null && paymentIntent.status == CANCELED) {
            emit(errorMapper.mapError(errorMessage = "Cannot retry paymentIntent with status ${paymentIntent.status}"))
            return@flow
        }

        if (paymentIntent.status == PaymentIntentStatus.REQUIRES_PAYMENT_METHOD) {
            paymentIntent = collectPayment(paymentIntent)
            if (paymentIntent.status != PaymentIntentStatus.REQUIRES_CONFIRMATION) {
                return@flow
            }
        }
        if (paymentIntent.status == PaymentIntentStatus.REQUIRES_CONFIRMATION) {
            paymentIntent = processPayment(paymentIntent)
            if (paymentIntent.status != PaymentIntentStatus.REQUIRES_CAPTURE) {
                return@flow
            }
        }

        if (paymentIntent.status == PaymentIntentStatus.REQUIRES_CAPTURE) {
            retrieveReceiptUrl(paymentIntent)?.let { receiptUrl ->
                capturePayment(receiptUrl, orderId, cardReaderStore, paymentIntent)
            }
        }
    }

    private suspend fun FlowCollector<CardPaymentStatus>.retrieveReceiptUrl(
        paymentIntent: PaymentIntent
    ): String? {
        return paymentIntent.getCharges().takeIf { it.isNotEmpty() }?.get(0)?.receiptUrl ?: run {
            emit(PaymentFailed(Generic, null, "ReceiptUrl not available"))
            null
        }
    }

    private suspend fun FlowCollector<CardPaymentStatus>.createPaymentIntent(paymentInfo: PaymentInfo): PaymentIntent? {
        var paymentIntent: PaymentIntent? = null
        emit(InitializingPayment)
        createPaymentAction.createPaymentIntent(enrichPaymentInfoWithCustomerId(paymentInfo)).collect {
            when (it) {
                is Failure -> emit(errorMapper.mapTerminalError(paymentIntent, it.exception))
                is Success -> paymentIntent = it.paymentIntent
            }
        }
        return paymentIntent
    }

    private suspend fun FlowCollector<CardPaymentStatus>.collectPayment(
        paymentIntent: PaymentIntent
    ): PaymentIntent {
        var result = paymentIntent
        emit(CollectingPayment)
        collectPaymentAction.collectPayment(paymentIntent).collect {
            when (it) {
                is CollectPaymentStatus.Failure -> emit(errorMapper.mapTerminalError(paymentIntent, it.exception))
                is CollectPaymentStatus.Success -> result = it.paymentIntent
            }
        }
        return result
    }

    private suspend fun FlowCollector<CardPaymentStatus>.processPayment(
        paymentIntent: PaymentIntent
    ): PaymentIntent {
        var result = paymentIntent
        emit(ProcessingPayment)
        processPaymentAction.processPayment(paymentIntent).collect {
            when (it) {
                is ProcessPaymentStatus.Failure -> emit(errorMapper.mapTerminalError(paymentIntent, it.exception))
                is ProcessPaymentStatus.Success -> result = it.paymentIntent
            }
        }
        return result
    }

    private suspend fun FlowCollector<CardPaymentStatus>.capturePayment(
        receiptUrl: String,
        orderId: Long,
        cardReaderStore: CardReaderStore,
        paymentIntent: PaymentIntent
    ) {
        emit(CapturingPayment)
        when (val captureResponse = cardReaderStore.capturePaymentIntent(orderId, paymentIntent.id)) {
            is CapturePaymentResponse.Successful -> emit(PaymentCompleted(receiptUrl))
            is CapturePaymentResponse.Error -> emit(errorMapper.mapCapturePaymentError(paymentIntent, captureResponse))
        }
    }

    private suspend fun FlowCollector<CardPaymentStatus>.isInvalidState(paymentInfo: PaymentInfo) =
        when {
            !paymentUtils.isSupportedCurrency(paymentInfo.currency) -> {
                emit(errorMapper.mapError(errorMessage = "Unsupported currency: $paymentInfo.currency"))
                true
            }
            !terminalWrapper.isInitialized() -> {
                emit(errorMapper.mapError(errorMessage = "Reader not connected"))
                true
            }
            else -> false
        }

    private suspend fun enrichPaymentInfoWithCustomerId(paymentInfo: PaymentInfo): PaymentInfo {
        val customerId = cardReaderStore.fetchCustomerIdByOrderId(paymentInfo.orderId)
        return paymentInfo.copy(customerId = customerId)
    }
}

internal data class PaymentDataImpl(val paymentIntent: PaymentIntent) : PaymentData
