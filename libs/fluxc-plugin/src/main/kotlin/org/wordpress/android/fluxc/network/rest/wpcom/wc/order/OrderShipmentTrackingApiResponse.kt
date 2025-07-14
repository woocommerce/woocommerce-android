package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import org.wordpress.android.fluxc.network.Response

@Suppress("PropertyName", "VariableNaming")
class OrderShipmentTrackingApiResponse : Response {
    val tracking_id: String? = null
    val tracking_provider: String? = null
    val tracking_link: String? = null
    val tracking_number: String? = null
    val date_shipped: String? = null
}
