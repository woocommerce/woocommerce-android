package com.woocommerce.android.cardreader.internal.payments

import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.PaymentIntentStatus
import com.stripe.stripeterminal.external.models.PaymentIntentStatus.CANCELED
import com.woocommerce.android.cardreader.CardReaderStore
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse
import com.woocommerce.android.cardreader.config.CardReaderConfigFactory
import com.woocommerce.android.cardreader.config.CardReaderConfigForSupportedCountry
import com.woocommerce.android.cardreader.internal.payments.actions.CancelPaymentAction
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction.CreatePaymentStatus.Failure
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction.CreatePaymentStatus.Success
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessPaymentIntentAction
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessPaymentIntentAction.ProcessPaymentIntentStatus
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import com.woocommerce.android.cardreader.payments.CardPaymentStatus
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CapturingPayment
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.Generic
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.InitializingPayment
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.PaymentCompleted
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.PaymentFailed
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.PaymentMethodType
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.ProcessingPayment
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.ProcessingPaymentCompleted
import com.woocommerce.android.cardreader.payments.CreatePaymentIntentResult
import com.woocommerce.android.cardreader.payments.PaymentData
import com.woocommerce.android.cardreader.payments.PaymentInfo
import com.woocommerce.android.cardreader.payments.RetrieveAndCollectResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow

@Suppress("LongParameterList")
internal class PaymentManager(
    private val terminalWrapper: TerminalWrapper,
    private val cardReaderStore: CardReaderStore,
    private val createPaymentAction: CreatePaymentAction,
    private val processPaymentIntentAction: ProcessPaymentIntentAction,
    private val cancelPaymentAction: CancelPaymentAction,
    private val paymentUtils: PaymentUtils,
    private val errorMapper: PaymentErrorMapper,
    private val cardReaderConfigFactory: CardReaderConfigFactory,
) {
    fun acceptPayment(paymentInfo: PaymentInfo): Flow<CardPaymentStatus> = flow {
        if (isInvalidState(paymentInfo)) return@flow

        val paymentIntent = createPaymentIntent(paymentInfo)
        if (paymentIntent?.status != PaymentIntentStatus.REQUIRES_PAYMENT_METHOD) {
            return@flow
        }
        processPaymentIntent(paymentInfo.orderId, paymentIntent).collect { emit(it) }
    }

    fun retryPayment(orderId: Long, paymentData: PaymentData) =
        processPaymentIntent(orderId, (paymentData as PaymentDataImpl).paymentIntent)

    suspend fun createPaymentIntentOnly(paymentInfo: PaymentInfo): CreatePaymentIntentResult =
        when (val result = createPaymentAction.createPaymentIntent(paymentInfo)) {
            is Success -> CreatePaymentIntentResult.Success(
                paymentIntentId = result.paymentIntent.id.orEmpty(),
                clientSecret = result.paymentIntent.clientSecret.orEmpty(),
            )
            is Failure -> CreatePaymentIntentResult.Failed(result.exception)
        }

    @Suppress("TooGenericExceptionCaught")
    suspend fun retrieveAndCollectPayment(clientSecret: String): RetrieveAndCollectResult =
        try {
            val retrieved = terminalWrapper.retrievePaymentIntent(clientSecret)
            val processed = terminalWrapper.processPaymentIntent(retrieved)
            RetrieveAndCollectResult.Success(
                paymentIntentId = processed.id.orEmpty(),
                status = processed.status?.name?.lowercase() ?: "unknown",
            )
        } catch (cause: Throwable) {
            RetrieveAndCollectResult.Failed(cause)
        }

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
        if (paymentIntent.status == null || paymentIntent.status == CANCELED) {
            emit(errorMapper.mapError(errorMessage = "Cannot retry paymentIntent with status ${paymentIntent.status}"))
            return@flow
        }

        if (paymentIntent.status == PaymentIntentStatus.REQUIRES_PAYMENT_METHOD ||
            paymentIntent.status == PaymentIntentStatus.REQUIRES_CONFIRMATION
        ) {
            paymentIntent = processPayment(paymentIntent)
        }

        /*
            At this point,

            if this was an Interac payment. The payment has already been captured successfully
            in the previous step (Processing step). In the next capture step, we will inform the backend about
            the successful Interac payment transaction that has already happened and it's not the success/failure
            of the actual Interac payment itself.

            If this was a non-Interac payment. We expect the payment intent's status to be REQUIRES_CAPTURE and in
            the next step we capture the payment in the backend. Here, the success/failure of the capture step defines
            the success/failure of the actual payment.
         */

        if (paymentIntent.status == PaymentIntentStatus.REQUIRES_CAPTURE || isInteracPaymentSuccessful(paymentIntent)) {
            retrieveReceiptUrl(paymentIntent)?.let { receiptUrl ->
                capturePayment(receiptUrl, orderId, cardReaderStore, paymentIntent)
            }
        }
    }

    private fun isInteracPayment(paymentIntent: PaymentIntent): Boolean {
        return !paymentIntent.getCharges().isNullOrEmpty() &&
            paymentIntent.getCharges().getOrNull(0)?.paymentMethodDetails?.interacPresentDetails != null
    }

    private fun isInteracPaymentSuccessful(paymentIntent: PaymentIntent): Boolean {
        return isInteracPayment(paymentIntent) && paymentIntent.status == PaymentIntentStatus.SUCCEEDED
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
        emit(InitializingPayment)
        return when (val result = createPaymentAction.createPaymentIntent(paymentInfo)) {
            is Failure -> {
                emit(errorMapper.mapTerminalError(null, result.exception))
                null
            }
            is Success -> result.paymentIntent
        }
    }

    private suspend fun FlowCollector<CardPaymentStatus>.processPayment(
        paymentIntent: PaymentIntent
    ): PaymentIntent {
        emit(ProcessingPayment)
        return when (val result = processPaymentIntentAction.processPaymentIntent(paymentIntent)) {
            is ProcessPaymentIntentStatus.Failure -> {
                emit(errorMapper.mapTerminalError(paymentIntent, result.exception))
                paymentIntent
            }
            is ProcessPaymentIntentStatus.Success -> {
                val paymentMethodType = determinePaymentMethodType(result)
                emit(ProcessingPaymentCompleted(paymentMethodType))
                result.paymentIntent
            }
        }
    }

    // Stripe new SDk now has paymentIntent.id as nullable to support offline payment. But we don't support offline yet
    // so adding paymentIntent.id!! here for now. Code need to be refactored once we support offline payment.
    private suspend fun FlowCollector<CardPaymentStatus>.capturePayment(
        receiptUrl: String,
        orderId: Long,
        cardReaderStore: CardReaderStore,
        paymentIntent: PaymentIntent
    ) {
        emit(CapturingPayment)
        when (val captureResponse = cardReaderStore.capturePaymentIntent(orderId, paymentIntent.id!!)) {
            is CapturePaymentResponse.Successful -> emit(PaymentCompleted(receiptUrl))
            is CapturePaymentResponse.Error -> emit(errorMapper.mapCapturePaymentError(paymentIntent, captureResponse))
        }
    }

    private suspend fun FlowCollector<CardPaymentStatus>.isInvalidState(paymentInfo: PaymentInfo): Boolean {
        val cardReaderConfig = cardReaderConfigFactory.getCardReaderConfigFor(paymentInfo.countryCode)
        return when {
            cardReaderConfig !is CardReaderConfigForSupportedCountry ||
                !paymentUtils.isSupportedCurrency(paymentInfo.currency, cardReaderConfig) -> {
                emit(errorMapper.mapError(errorMessage = "Unsupported currency: $paymentInfo.currency"))
                true
            }
            !terminalWrapper.isInitialized() -> {
                emit(errorMapper.mapError(errorMessage = "Reader not connected"))
                true
            }
            else -> false
        }
    }

    private fun determinePaymentMethodType(status: ProcessPaymentIntentStatus.Success): PaymentMethodType {
        val charge = status.paymentIntent.getCharges().firstOrNull()
        return when {
            charge?.paymentMethodDetails?.interacPresentDetails != null -> PaymentMethodType.INTERAC_PRESENT
            charge?.paymentMethodDetails?.cardPresentDetails != null -> PaymentMethodType.CARD_PRESENT
            else -> PaymentMethodType.UNKNOWN
        }
    }
}

internal data class PaymentDataImpl(val paymentIntent: PaymentIntent) : PaymentData
