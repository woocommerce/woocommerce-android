package com.woocommerce.android.cardreader.internal.payments

import com.stripe.stripeterminal.model.external.PaymentIntent
import com.stripe.stripeterminal.model.external.TerminalException
import com.stripe.stripeterminal.model.external.TerminalException.TerminalErrorCode
import com.woocommerce.android.cardreader.CardPaymentStatus.CardPaymentStatusErrorType
import com.woocommerce.android.cardreader.CardPaymentStatus.CardPaymentStatusErrorType.CARD_READ_TIMED_OUT
import com.woocommerce.android.cardreader.CardPaymentStatus.CardPaymentStatusErrorType.GENERIC_ERROR
import com.woocommerce.android.cardreader.CardPaymentStatus.CardPaymentStatusErrorType.NO_NETWORK
import com.woocommerce.android.cardreader.CardPaymentStatus.CardPaymentStatusErrorType.PAYMENT_DECLINED
import com.woocommerce.android.cardreader.CardPaymentStatus.PaymentFailed
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse

class PaymentErrorMapper {
    fun mapError(
        originalPaymentIntent: PaymentIntent,
        exception: TerminalException
    ): PaymentFailed {
        val paymentData = PaymentDataImpl(exception.paymentIntent ?: originalPaymentIntent)
        val type = when (exception.errorCode) {
            TerminalErrorCode.CARD_READ_TIMED_OUT -> CARD_READ_TIMED_OUT
            TerminalErrorCode.PAYMENT_DECLINED_BY_STRIPE_API -> PAYMENT_DECLINED
            TerminalErrorCode.REQUEST_TIMED_OUT -> NO_NETWORK
            else -> GENERIC_ERROR
        }
        return PaymentFailed(type, paymentData, exception.errorMessage)
    }

    fun mapError(
        originalPaymentIntent: PaymentIntent,
        capturePaymentResponse: CapturePaymentResponse
    ): PaymentFailed {
        val paymentData = PaymentDataImpl(originalPaymentIntent)
        val message = "Capturing payment failed: $capturePaymentResponse"
        val type = when (capturePaymentResponse) {
            CapturePaymentResponse.NETWORK_ERROR -> NO_NETWORK
            CapturePaymentResponse.GENERIC_ERROR,
            CapturePaymentResponse.MISSING_ORDER,
            CapturePaymentResponse.CAPTURE_ERROR,
            CapturePaymentResponse.SERVER_ERROR -> GENERIC_ERROR
            CapturePaymentResponse.PAYMENT_ALREADY_CAPTURED,
            CapturePaymentResponse.SUCCESS ->
                throw IllegalStateException("mapError(..) should never be invoked with a successful response.")
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
