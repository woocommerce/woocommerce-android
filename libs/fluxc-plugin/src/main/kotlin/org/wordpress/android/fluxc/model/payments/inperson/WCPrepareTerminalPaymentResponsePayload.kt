package org.wordpress.android.fluxc.model.payments.inperson

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError

data class WCPrepareTerminalPaymentResponsePayload(
    val site: SiteModel,
    val paymentId: String,
    val orderId: Long,
    val error: WooError? = null,
)
