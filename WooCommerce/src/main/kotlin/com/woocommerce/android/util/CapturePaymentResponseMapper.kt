package com.woocommerce.android.util

import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.Error.CaptureError
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.Error.GenericError
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.Error.MissingOrder
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.Error.NetworkError
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.Error.ServerError
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.Successful.PaymentAlreadyCaptured
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse.Successful.Success
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentErrorType.CAPTURE_ERROR
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentErrorType.MISSING_ORDER
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentErrorType.PAYMENT_ALREADY_CAPTURED
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentErrorType.SERVER_ERROR
import org.wordpress.android.fluxc.model.pay.WCCapturePaymentResponsePayload
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
