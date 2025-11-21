package org.wordpress.android.fluxc.model.shippinglabels

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

@Entity(
    tableName = "ShippingLabelCreationEligibilityEntity",
    primaryKeys = ["localSiteId", "remoteOrderId"]
)
data class WCShippingLabelCreationEligibility(
    val localSiteId: LocalId,
    val remoteOrderId: RemoteId,
    val canCreatePackage: Boolean,
    val canCreatePaymentMethod: Boolean,
    val canCreateCustomsForm: Boolean,
    val isEligible: Boolean,
    val reason: String?
)
