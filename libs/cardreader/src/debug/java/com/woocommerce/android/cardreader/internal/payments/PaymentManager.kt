package com.woocommerce.android.cardreader.internal.payments

import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.Terminal.Companion
import com.stripe.stripeterminal.model.external.PaymentIntent
import com.stripe.stripeterminal.model.external.PaymentIntentStatus
import com.stripe.stripeterminal.model.external.PaymentIntentStatus.CANCELED
import com.stripe.stripeterminal.model.external.TerminalException
import com.stripe.stripeterminal.model.external.TerminalException.TerminalErrorCode
import com.woocommerce.android.cardreader.CardPaymentStatus
import com.woocommerce.android.cardreader.CardPaymentStatus.CapturingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.CardPaymentStatusErrorType
import com.woocommerce.android.cardreader.CardPaymentStatus.CardPaymentStatusErrorType.CARD_READ_TIMED_OUT
import com.woocommerce.android.cardreader.CardPaymentStatus.CardPaymentStatusErrorType.NO_NETWORK
import com.woocommerce.android.cardreader.CardPaymentStatus.CardPaymentStatusErrorType.PAYMENT_DECLINED
import com.woocommerce.android.cardreader.CardPaymentStatus.CollectingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.InitializingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.PaymentCompleted
import com.woocommerce.android.cardreader.CardPaymentStatus.PaymentFailed
import com.woocommerce.android.cardreader.CardPaymentStatus.ProcessingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.ShowAdditionalInfo
import com.woocommerce.android.cardreader.CardPaymentStatus.WaitingForInput
import com.woocommerce.android.cardreader.CardReaderStore
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.CAPTURE_ERROR
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.GENERIC_ERROR
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.MISSING_ORDER
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.NETWORK_ERROR
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.PAYMENT_ALREADY_CAPTURED
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.SERVER_ERROR
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.SUCCESS
import com.woocommerce.android.cardreader.PaymentData
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction.CollectPaymentStatus
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction.CollectPaymentStatus.DisplayMessageRequested
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction.CollectPaymentStatus.ReaderInputRequested
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction.CreatePaymentStatus.Failure
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction.CreatePaymentStatus.Success
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessPaymentAction
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessPaymentAction.ProcessPaymentStatus
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP

private const val USD_TO_CENTS_DECIMAL_PLACES = 2
private const val USD_CURRENCY = "usd"

internal class PaymentManager(
    private val terminalWrapper: TerminalWrapper,
    private val cardReaderStore: CardReaderStore,
    private val createPaymentAction: CreatePaymentAction,
    private val collectPaymentAction: CollectPaymentAction,
    private val processPaymentAction: ProcessPaymentAction,
    private val errorMapper: PaymentErrorMapper
) {
    suspend fun acceptPayment(orderId: Long, amount: BigDecimal, currency: String): Flow<CardPaymentStatus> = flow {
        if (!isSupportedCurrency(currency)) {
            emit(errorMapper.mapError(errorMessage = "Unsupported currency: $currency"))
            return@flow
        }
        var amountInSmallestCurrencyUnit = try {
            convertBigDecimalInDollarsToIntegerInCents(amount)
        } catch (e: ArithmeticException) {
            emit(errorMapper.mapError(errorMessage = "BigDecimal amount doesn't fit into an Integer: $amount"))
            return@flow
        }
        if (!terminalWrapper.isInitialized()) {
            emit(errorMapper.mapError(errorMessage = "Reader not connected"))
            return@flow
        }
        var paymentIntent = createPaymentIntent(amountInSmallestCurrencyUnit, currency)
        if (paymentIntent?.status != PaymentIntentStatus.REQUIRES_PAYMENT_METHOD) {
            return@flow
        }
        processPaymentIntent(orderId, paymentIntent).collect { emit(it) }
    }

    fun retryPayment(orderId: Long, paymentData: PaymentData) =
        processPaymentIntent(orderId, (paymentData as PaymentDataImpl).paymentIntent)

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
            capturePayment(orderId, cardReaderStore, paymentIntent)
        }
    }

    private suspend fun FlowCollector<CardPaymentStatus>.createPaymentIntent(
        amount: Int,
        currency: String
    ): PaymentIntent? {
        var paymentIntent: PaymentIntent? = null
        emit(InitializingPayment)
        createPaymentAction.createPaymentIntent(amount, currency).collect {
            when (it) {
                is Failure -> emit(errorMapper.mapError(paymentIntent, it.exception.errorMessage))
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
                is DisplayMessageRequested -> emit(ShowAdditionalInfo)
                is ReaderInputRequested -> emit(WaitingForInput)
                is CollectPaymentStatus.Failure -> emit(errorMapper.mapError(paymentIntent, it.exception))
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
                is ProcessPaymentStatus.Failure -> emit(errorMapper.mapError(paymentIntent, it.exception))
                is ProcessPaymentStatus.Success -> result = it.paymentIntent
            }
        }
        return result
    }

    private suspend fun FlowCollector<CardPaymentStatus>.capturePayment(
        orderId: Long,
        cardReaderStore: CardReaderStore,
        paymentIntent: PaymentIntent
    ) {
        emit(CapturingPayment)
        val captureResponse = cardReaderStore.capturePaymentIntent(orderId, paymentIntent.id)
        if (captureResponse == SUCCESS || captureResponse == PAYMENT_ALREADY_CAPTURED) {
            emit(PaymentCompleted)
        } else {
            emit(errorMapper.mapError(paymentIntent, captureResponse))
        }
    }

    // TODO cardreader Add support for other currencies
    private fun convertBigDecimalInDollarsToIntegerInCents(amount: BigDecimal): Int {
        return amount
            // round to USD_TO_CENTS_DECIMAL_PLACES decimal places
            .setScale(USD_TO_CENTS_DECIMAL_PLACES, HALF_UP)
            // convert dollars to cents
            .movePointRight(USD_TO_CENTS_DECIMAL_PLACES)
            .intValueExact()
    }

    // TODO Add Support for other currencies
    private fun isSupportedCurrency(currency: String): Boolean = currency.toLowerCase() == USD_CURRENCY
}

data class PaymentDataImpl(val paymentIntent: PaymentIntent) : PaymentData
