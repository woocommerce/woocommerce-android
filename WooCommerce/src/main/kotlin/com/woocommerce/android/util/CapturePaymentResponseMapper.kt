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
            GENERIC_ERROR -> GenericError
            MISSING_ORDER -> MissingOrder
            CAPTURE_ERROR -> CaptureError
            SERVER_ERROR -> ServerError
            NETWORK_ERROR -> NetworkError
        }
}
