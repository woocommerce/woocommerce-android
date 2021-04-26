package com.woocommerce.android.cardreader.internal.payments

import com.stripe.stripeterminal.model.external.PaymentIntent
import com.stripe.stripeterminal.model.external.PaymentIntentStatus
import com.woocommerce.android.cardreader.CardPaymentStatus
import com.woocommerce.android.cardreader.CardPaymentStatus.CapturingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.CapturingPaymentFailed
import com.woocommerce.android.cardreader.CardPaymentStatus.CollectingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.CollectingPaymentFailed
import com.woocommerce.android.cardreader.CardPaymentStatus.InitializingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.InitializingPaymentFailed
import com.woocommerce.android.cardreader.CardPaymentStatus.PaymentCompleted
import com.woocommerce.android.cardreader.CardPaymentStatus.ProcessingPayment
import com.woocommerce.android.cardreader.CardPaymentStatus.ProcessingPaymentFailed
import com.woocommerce.android.cardreader.CardPaymentStatus.ShowAdditionalInfo
import com.woocommerce.android.cardreader.CardPaymentStatus.UnexpectedError
import com.woocommerce.android.cardreader.CardPaymentStatus.WaitingForInput
import com.woocommerce.android.cardreader.CardReaderStore
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction.CollectPaymentStatus
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction.CollectPaymentStatus.DisplayMessageRequested
import com.woocommerce.android.cardreader.internal.payments.actions.CollectPaymentAction.CollectPaymentStatus.ReaderInputRequested
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction.CreatePaymentStatus.Failure
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction.CreatePaymentStatus.Success
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessPaymentAction
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessPaymentAction.ProcessPaymentStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import java.lang.ArithmeticException
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP

private const val USD_TO_CENTS_DECIMAL_PLACES = 2
private const val USD_CURRENCY = "usd"

internal class PaymentManager(
    private val cardReaderStore: CardReaderStore,
    private val createPaymentAction: CreatePaymentAction,
    private val collectPaymentAction: CollectPaymentAction,
    private val processPaymentAction: ProcessPaymentAction
) {
    suspend fun acceptPayment(amount: BigDecimal, currency: String): Flow<CardPaymentStatus> = flow {
        if (!isSupportedCurrency(currency)) {
            emit(UnexpectedError("Unsupported currency: $currency"))
            return@flow
        }
        val amountInSmallestCurrencyUnit = try {
            convertBigDecimalInDollarsToIntegerInCents(amount)
        } catch (e: ArithmeticException) {
            emit(UnexpectedError("BigDecimal amount doesn't fit into an Integer: $amount"))
            return@flow
        }
        var paymentIntent = createPaymentIntent(amountInSmallestCurrencyUnit, currency)
        if (paymentIntent?.status != PaymentIntentStatus.REQUIRES_PAYMENT_METHOD) {
            return@flow
        }

        paymentIntent = collectPayment(paymentIntent)
        if (paymentIntent.status != PaymentIntentStatus.REQUIRES_CONFIRMATION) {
            return@flow
        }

        paymentIntent = processPayment(paymentIntent)
        if (paymentIntent.status != PaymentIntentStatus.REQUIRES_CAPTURE) {
            return@flow
        }

        capturePayment(cardReaderStore, paymentIntent)
    }

    private suspend fun FlowCollector<CardPaymentStatus>.createPaymentIntent(
        amount: Int,
        currency: String
    ): PaymentIntent? {
        var paymentIntent: PaymentIntent? = null
        emit(InitializingPayment)
        createPaymentAction.createPaymentIntent(amount, currency).collect {
            when (it) {
                is Failure -> emit(InitializingPaymentFailed)
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
                is CollectPaymentStatus.Failure -> emit(CollectingPaymentFailed)
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
                is ProcessPaymentStatus.Failure -> emit(ProcessingPaymentFailed)
                is ProcessPaymentStatus.Success -> result = it.paymentIntent
            }
        }
        return result
    }

    private suspend fun FlowCollector<CardPaymentStatus>.capturePayment(
        cardReaderStore: CardReaderStore,
        paymentIntent: PaymentIntent
    ) {
        emit(CapturingPayment)
        val success = cardReaderStore.capturePaymentIntent(paymentIntent.id)
        if (success) {
            emit(PaymentCompleted)
        } else {
            emit(CapturingPaymentFailed)
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
