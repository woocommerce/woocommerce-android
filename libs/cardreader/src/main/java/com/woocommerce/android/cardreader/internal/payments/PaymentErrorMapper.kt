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
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.GenericError
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
            else -> GenericError
        }
        return PaymentFailed(type, paymentData, exception.errorMessage)
    }

    private fun mapDeclinedByStripeApiError(exception: TerminalException): DeclinedByStripeApiError =
        if (exception.apiError?.code == DeclinedByStripeApiError.CardDeclined.ERROR_CODE) {
            DeclinedByStripeApiError.CardDeclined::class.sealedSubclasses
                .map { it.objectInstance }
                .mapNotNull { it as DeclinedByStripeApiError.CardDeclined }
                .firstOrNull {
                    it.declineErrorCodes.contains(exception.apiError?.declineCode)
                } ?: DeclinedByStripeApiError.CardDeclined.Unknown
        } else {
            DeclinedByStripeApiError::class.sealedSubclasses
                .map { it.objectInstance }
                .mapNotNull { it as DeclinedByStripeApiError }
                .firstOrNull {
                    it.errorCodes.contains(exception.apiError?.code)
                } ?: DeclinedByStripeApiError.Unknown
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
}
