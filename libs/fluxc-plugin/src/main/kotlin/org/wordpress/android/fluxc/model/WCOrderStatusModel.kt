package org.wordpress.android.fluxc.model

import androidx.room.Entity
import org.wordpress.android.fluxc.model.LocalOrRemoteId.*

@Entity(
    tableName = "OrderStatusEntity",
    primaryKeys = ["localSiteId", "statusKey"]
)
data class WCOrderStatusModel(
    val localSiteId: LocalId = LocalId(0),
    val statusKey: String = "",
    val label: String = "",
    val statusCount: Int = 0
)
