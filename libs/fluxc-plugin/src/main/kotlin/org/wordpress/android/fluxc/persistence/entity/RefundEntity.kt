package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

@Entity(
    tableName = "WCRefunds",
    primaryKeys = ["localSiteId", "orderId", "refundId"]
)
data class RefundEntity(
    val localSiteId: LocalId,
    val orderId: RemoteId,
    val refundId: RemoteId,
    val data: String
)
