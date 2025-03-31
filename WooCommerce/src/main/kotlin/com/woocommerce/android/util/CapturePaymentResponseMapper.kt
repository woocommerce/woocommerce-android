package com.woocommerce.android.util

import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.Error.CaptureError
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.Error.GenericError
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.Error.MissingOrder
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.Error.NetworkError
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.Error.ServerError
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.Successful.PaymentAlreadyCaptured
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.Successful.Success
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentError
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentErrorType.AMOUNT_TOO_SMALL
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentErrorType.CAPTURE_ERROR
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentErrorType.MISSING_ORDER
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentErrorType.PAYMENT_ALREADY_CAPTURED
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentErrorType.SERVER_ERROR
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentResponsePayload
import javax.inject.Inject

class CapturePaymentResponseMapper @Inject constructor() {
    fun mapResponse(response: WCCapturePaymentResponsePayload) = when (response.error?.type) {
        null -> Success
        PAYMENT_ALREADY_CAPTURED -> PaymentAlreadyCaptured
        GENERIC_ERROR -> GenericError(response.error.messageOrDefault())
        MISSING_ORDER -> MissingOrder(response.error.messageOrDefault())
        CAPTURE_ERROR -> CaptureError.Generic(response.error.messageOrDefault())
        SERVER_ERROR -> ServerError(response.error.messageOrDefault())
        NETWORK_ERROR -> NetworkError(response.error.messageOrDefault())
        AMOUNT_TOO_SMALL -> mapAmountTooSmallError(response)
    }

    private fun mapAmountTooSmallError(response: WCCapturePaymentResponsePayload): CaptureError {
        val extraData = response.error?.extraData ?: return CaptureError.Generic(response.error.messageOrDefault())
        return if (extraData[MINIMUM_AMOUNT_KEY] == null || extraData[MINIMUM_AMOUNT_CURRENCY_KEY] == null) {
            CaptureError.Generic(response.error.messageOrDefault())
        } else {
            val minAmount = when (val amount = extraData[MINIMUM_AMOUNT_KEY]) {
                is Double -> amount.toLong()
                is Long -> amount
                is Int -> amount.toLong()
                else -> (amount.toString().toDoubleOrNull() ?: 0.0).toLong()
            }

            CaptureError.AmountTooSmall(
                message = "Amount too small",
                minAmountInMicroUnits = minAmount,
                currency = extraData[MINIMUM_AMOUNT_CURRENCY_KEY] as String
            )
        }
    }

    private fun WCCapturePaymentError?.messageOrDefault() = this?.message ?: "No error message provided"

    companion object {
        private const val MINIMUM_AMOUNT_CURRENCY_KEY = "minimum_amount_currency"
        private const val MINIMUM_AMOUNT_KEY = "minimum_amount"
    }
}
