package org.wordpress.android.fluxc.model

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId

@Entity(
    tableName = "OrderStatusEntity",
    primaryKeys = ["siteId", "statusKey"]
)
data class WCOrderStatusModel(
    val siteId: LocalId = LocalId(0),
    val statusKey: String = "",
    val label: String = "",
    val statusCount: Int = 0,
    val position: Int = 0,
)
