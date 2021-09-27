package com.woocommerce.android.cardreader.internal.payments

import com.stripe.stripeterminal.model.external.PaymentIntent
import com.stripe.stripeterminal.model.external.TerminalException
import com.stripe.stripeterminal.model.external.TerminalException.TerminalErrorCode
import com.woocommerce.android.cardreader.CardPaymentStatus
import com.woocommerce.android.cardreader.CardPaymentStatus.CardPaymentStatusErrorType.*
import com.woocommerce.android.cardreader.CardPaymentStatus.PaymentFailed
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.Error.NetworkError
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.Error.ServerError

internal class PaymentErrorMapper {
    fun mapTerminalError(
        originalPaymentIntent: PaymentIntent?,
        exception: TerminalException
    ): PaymentFailed {
        val paymentData =
            originalPaymentIntent?.let {
                PaymentDataImpl(exception.paymentIntent ?: originalPaymentIntent)
            }
        val type = when (exception.errorCode) {
            TerminalErrorCode.CARD_READ_TIMED_OUT -> CARD_READ_TIMED_OUT
            TerminalErrorCode.PAYMENT_DECLINED_BY_STRIPE_API -> PAYMENT_DECLINED
            TerminalErrorCode.REQUEST_TIMED_OUT -> NO_NETWORK
            TerminalErrorCode.STRIPE_API_ERROR -> mapStripeAPIError(exception)
            else -> GENERIC_ERROR
        }
        return PaymentFailed(type, paymentData, exception.errorMessage)
    }

    private fun mapStripeAPIError(exception: TerminalException): CardPaymentStatus.CardPaymentStatusErrorType {
        return when (exception.apiError?.code) {
            StripeApiError.AMOUNT_TOO_SMALL.message -> AMOUNT_TOO_SMALL
            else -> GENERIC_ERROR
        }
    }

    fun mapCapturePaymentError(
        originalPaymentIntent: PaymentIntent,
        capturePaymentResponse: CapturePaymentResponse.Error
    ): PaymentFailed {
        val paymentData = PaymentDataImpl(originalPaymentIntent)
        val message = "Capturing payment failed: $capturePaymentResponse"
        val type = when (capturePaymentResponse) {
            NetworkError -> NO_NETWORK
            ServerError -> SERVER_ERROR
            else -> GENERIC_ERROR
        }
        return PaymentFailed(type, paymentData, message)
    }

    fun mapError(
        originalPaymentIntent: PaymentIntent? = null,
        errorMessage: String
    ): PaymentFailed {
        val paymentData = originalPaymentIntent?.let { PaymentDataImpl(originalPaymentIntent) }
        return PaymentFailed(GENERIC_ERROR, paymentData, errorMessage)
    }

    enum class StripeApiError(val message: String) {
        AMOUNT_TOO_SMALL("amount_too_small")
    }
}
