package org.wordpress.android.fluxc.model.payments.inperson

import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.payments.inperson.WCCapturePaymentErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.Store.OnChangedError

class WCCapturePaymentResponsePayload(
    val site: SiteModel,
    val paymentId: String,
    val orderId: Long,
    val status: String?
) : Payload<WCCapturePaymentError?>() {
    constructor(
        error: WCCapturePaymentError,
        site: SiteModel,
        paymentId: String,
        orderId: Long
    ) : this(site, paymentId, orderId, null) {
        this.error = error
    }
}

class WCCapturePaymentError(
    val type: WCCapturePaymentErrorType = GENERIC_ERROR,
    val message: String = "",
    val extraData: Map<String, Any>? = null
) : OnChangedError

enum class WCCapturePaymentErrorType {
    GENERIC_ERROR,
    PAYMENT_ALREADY_CAPTURED,
    MISSING_ORDER,
    CAPTURE_ERROR,
    SERVER_ERROR,
    NETWORK_ERROR
}
