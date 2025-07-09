package org.wordpress.android.fluxc.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "WCRefunds",
    primaryKeys = ["localSiteId", "orderId", "refundId"]
)
data class RefundEntity(
    @ColumnInfo(name = "localSiteId")
    val localSiteId: Int,
    val orderId: Long,
    val refundId: Long,
    val data: String
)
