package com.woocommerce.android.util

import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.Error.CaptureError
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.Error.GenericError
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.Error.MissingOrder
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.Error.NetworkError
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.Error.ServerError
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.Successful.PaymentAlreadyCaptured
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.Successful.Success
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentErrorType.CAPTURE_ERROR
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentErrorType.MISSING_ORDER
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentErrorType.PAYMENT_ALREADY_CAPTURED
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentErrorType.SERVER_ERROR
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentResponsePayload
import javax.inject.Inject

class CapturePaymentResponseMapper @Inject constructor() {
    fun mapResponse(response: WCCapturePaymentResponsePayload) =
        when (response.error?.type) {
            null -> Success
            PAYMENT_ALREADY_CAPTURED -> PaymentAlreadyCaptured
            GENERIC_ERROR -> GenericError(response.error?.message.toErrorMessage())
            MISSING_ORDER -> MissingOrder(response.error?.message.toErrorMessage())
            CAPTURE_ERROR -> CaptureError(response.error?.message.toErrorMessage())
            SERVER_ERROR -> ServerError(response.error?.message.toErrorMessage())
            NETWORK_ERROR -> NetworkError(response.error?.message.toErrorMessage())
        }

    private fun String?.toErrorMessage() = this ?: "No error message provided"
}
