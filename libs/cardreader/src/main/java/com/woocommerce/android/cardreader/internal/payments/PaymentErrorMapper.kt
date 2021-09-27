package com.woocommerce.android.cardreader.internal.payments

import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.TerminalException
import com.stripe.stripeterminal.external.models.TerminalException.TerminalErrorCode
import com.woocommerce.android.cardreader.CardPaymentStatus.CardPaymentStatusErrorType
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
            TerminalErrorCode.CARD_READ_TIMED_OUT -> CardReadTimeOut
            TerminalErrorCode.DECLINED_BY_STRIPE_API -> mapStripeDeclinedError(exception)
            TerminalErrorCode.REQUEST_TIMED_OUT -> NoNetwork
            else -> GenericError
        }
        return PaymentFailed(type, paymentData, exception.errorMessage)
    }

    private fun mapStripeDeclinedError(exception: TerminalException): PaymentDeclined {
        return when (exception.apiError?.code) {
            DeclinedPayment.AMOUNT_TOO_SMALL.message -> PaymentDeclined.AmountTooSmall
            else -> PaymentDeclined.Declined
        }
    }

    fun mapCapturePaymentError(
        originalPaymentIntent: PaymentIntent,
        capturePaymentResponse: CapturePaymentResponse.Error
    ): PaymentFailed {
        val paymentData = PaymentDataImpl(originalPaymentIntent)
        val message = "Capturing payment failed: $capturePaymentResponse"
        val type = when (capturePaymentResponse) {
            NetworkError -> NoNetwork
            ServerError -> CardPaymentStatusErrorType.ServerError
            else -> GenericError
        }
        return PaymentFailed(type, paymentData, message)
    }

    fun mapError(
        originalPaymentIntent: PaymentIntent? = null,
        errorMessage: String
    ): PaymentFailed {
        val paymentData = originalPaymentIntent?.let { PaymentDataImpl(originalPaymentIntent) }
        return PaymentFailed(GenericError, paymentData, errorMessage)
    }

    enum class DeclinedPayment(val message: String) {
        AMOUNT_TOO_SMALL("amount_too_small")
    }
}
