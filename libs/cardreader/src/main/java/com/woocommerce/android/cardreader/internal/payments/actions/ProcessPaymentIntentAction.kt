package com.woocommerce.android.cardreader.internal.payments.actions

import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.internal.LOG_TAG
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessPaymentIntentAction.ProcessPaymentIntentStatus.Failure
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessPaymentIntentAction.ProcessPaymentIntentStatus.Success
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper

internal class ProcessPaymentIntentAction(
    private val terminal: TerminalWrapper,
    private val logWrapper: LogWrapper
) {
    sealed class ProcessPaymentIntentStatus {
        data class Success(val paymentIntent: PaymentIntent) : ProcessPaymentIntentStatus()
        data class Failure(val exception: TerminalException) : ProcessPaymentIntentStatus()
    }

    suspend fun processPaymentIntent(paymentIntent: PaymentIntent): ProcessPaymentIntentStatus {
        logWrapper.d(LOG_TAG, "Processing payment intent")
        return try {
            val processedPaymentIntent = terminal.processPaymentIntent(paymentIntent)
            logWrapper.d(LOG_TAG, "Processing payment intent succeeded")
            Success(processedPaymentIntent)
        } catch (e: TerminalException) {
            logWrapper.e(
                LOG_TAG,
                "Processing payment intent failed. " +
                    "Message: ${e.errorMessage}, DeclineCode: ${e.apiError?.declineCode}"
            )
            Failure(e)
        }
    }

    suspend fun collectPaymentMethod(paymentIntent: PaymentIntent): ProcessPaymentIntentStatus {
        logWrapper.d(LOG_TAG, "Collecting payment method")
        return try {
            val collectedPaymentIntent = terminal.collectPaymentMethod(paymentIntent)
            logWrapper.d(LOG_TAG, "Collecting payment method succeeded")
            Success(collectedPaymentIntent)
        } catch (e: TerminalException) {
            logWrapper.e(
                LOG_TAG,
                "Collecting payment method failed. " +
                    "Message: ${e.errorMessage}, DeclineCode: ${e.apiError?.declineCode}"
            )
            Failure(e)
        }
    }

    suspend fun confirmPaymentIntent(paymentIntent: PaymentIntent): ProcessPaymentIntentStatus {
        logWrapper.d(LOG_TAG, "Confirming payment intent")
        return try {
            val confirmedPaymentIntent = terminal.confirmPaymentIntent(paymentIntent)
            logWrapper.d(LOG_TAG, "Confirming payment intent succeeded")
            Success(confirmedPaymentIntent)
        } catch (e: TerminalException) {
            logWrapper.e(
                LOG_TAG,
                "Confirming payment intent failed. " +
                    "Message: ${e.errorMessage}, DeclineCode: ${e.apiError?.declineCode}"
            )
            Failure(e)
        }
    }
}
