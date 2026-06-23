package org.wordpress.android.fluxc.model.shippinglabels

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

@Entity(
    tableName = "ShippingLabelEntity",
    primaryKeys = ["localSiteId", "remoteOrderId", "remoteShippingLabelId"],
)
data class WCShippingLabelModel(
    val localSiteId: LocalId,
    val remoteOrderId: RemoteId,
    val remoteShippingLabelId: RemoteId,
    val trackingNumber: String,
    val carrierId: String,
    val dateCreated: Long?,
    val expiryDate: Long?,
    val serviceName: String,
    val status: String,
    val packageName: String,
    val rate: Float,
    val refundableAmount: Float,
    val currency: String,
    val productNames: String,
    val productIds: String,
    val formData: String,
    val refund: String,
    val commercialInvoiceUrl: String?
)
