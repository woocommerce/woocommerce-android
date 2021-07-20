package com.woocommerce.android.cardreader.internal.payments

import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.TerminalException
import com.stripe.stripeterminal.external.models.TerminalException.TerminalErrorCode
import com.woocommerce.android.cardreader.CardPaymentStatus.CardPaymentStatusErrorType.CARD_READ_TIMED_OUT
import com.woocommerce.android.cardreader.CardPaymentStatus.CardPaymentStatusErrorType.GENERIC_ERROR
import com.woocommerce.android.cardreader.CardPaymentStatus.CardPaymentStatusErrorType.NO_NETWORK
import com.woocommerce.android.cardreader.CardPaymentStatus.CardPaymentStatusErrorType.SERVER_ERROR
import com.woocommerce.android.cardreader.CardPaymentStatus.CardPaymentStatusErrorType.PAYMENT_DECLINED
import com.woocommerce.android.cardreader.CardPaymentStatus.PaymentFailed
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.Error.NetworkError
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.Error.ServerError

class PaymentErrorMapper {
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
            else -> GENERIC_ERROR
        }
        return PaymentFailed(type, paymentData, exception.errorMessage)
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
}
