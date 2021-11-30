package com.woocommerce.android.cardreader.internal.payments

import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.TerminalException
import com.stripe.stripeterminal.external.models.TerminalException.TerminalErrorCode
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.Error.NetworkError
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.Error.ServerError
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.CardReadTimeOut
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByStripeApiError
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.Generic
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.NoNetwork
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.PaymentFailed

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
            TerminalErrorCode.DECLINED_BY_STRIPE_API -> mapDeclinedByStripeApiError(exception)
            TerminalErrorCode.REQUEST_TIMED_OUT -> NoNetwork
            else -> Generic
        }
        return PaymentFailed(type, paymentData, exception.errorMessage)
    }

    private fun mapDeclinedByStripeApiError(exception: TerminalException): DeclinedByStripeApiError =
        // https://stripe.com/docs/error-codes#card-declined
        // Apparently the implementation is contradict with the doc
        // Some declines code comes without "card_declined" in "exception.apiError?.code"
        DeclinedByStripeApiError.CardDeclined::class.sealedSubclasses
            .mapNotNull { it.objectInstance }
            .firstOrNull {
                it.declineErrorCodes.contains(exception.apiError?.declineCode)
            } ?: DeclinedByStripeApiError::class.sealedSubclasses
            .mapNotNull { it.objectInstance }
            .firstOrNull {
                it.errorCodes.contains(exception.apiError?.code)
            } ?: DeclinedByStripeApiError.Unknown

    fun mapCapturePaymentError(
        originalPaymentIntent: PaymentIntent,
        capturePaymentResponse: CapturePaymentResponse.Error
    ): PaymentFailed {
        val paymentData = PaymentDataImpl(originalPaymentIntent)
        val message = "Capturing payment failed: $capturePaymentResponse"
        val type = when (capturePaymentResponse) {
            NetworkError -> NoNetwork
            ServerError -> CardPaymentStatusErrorType.Server
            else -> Generic
        }
        return PaymentFailed(type, paymentData, message)
    }

    fun mapError(
        originalPaymentIntent: PaymentIntent? = null,
        errorMessage: String
    ): PaymentFailed {
        val paymentData = originalPaymentIntent?.let { PaymentDataImpl(originalPaymentIntent) }
        return PaymentFailed(Generic, paymentData, errorMessage)
    }
}
