package com.woocommerce.android.model

import org.wordpress.android.fluxc.model.WCOrderFulfillmentModel

data class OrderFulfillment(
    val localSiteId: Int,
    val orderId: Long,
    val fulfillmentId: Long,
    val status: String?,
    val isFulfilled: Boolean,
    val dateUpdated: String?,
    val dateFulfilled: String?,
    val trackingNumber: String?,
    val shipmentProvider: String?,
    val trackingUrl: String?
)

fun WCOrderFulfillmentModel.toAppModel() = OrderFulfillment(
    localSiteId = localSiteId.value,
    orderId = orderId.value,
    fulfillmentId = fulfillmentId,
    status = status,
    isFulfilled = isFulfilled,
    dateUpdated = dateUpdated,
    dateFulfilled = dateFulfilled,
    trackingNumber = trackingNumber,
    shipmentProvider = shipmentProvider,
    trackingUrl = trackingUrl
)
