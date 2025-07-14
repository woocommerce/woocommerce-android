package org.wordpress.android.fluxc.persistence.entity

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId

@Entity(
    tableName = "RefundEntity",
    primaryKeys = ["siteId", "orderId", "refundId"]
)
data class RefundEntity(
    val siteId: LocalId,
    val orderId: RemoteId,
    val refundId: RemoteId,
    val data: String
)
