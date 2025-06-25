package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel

@Dao
abstract class ShippingLabelDao {

    @Query(
        """
        SELECT * FROM ShippingLabelEntity
        WHERE localSiteId = :localSiteId
        AND remoteOrderId = :orderId
        """
    )
    abstract suspend fun getShippingLabels(
        localSiteId: Int,
        orderId: Long
    ): List<WCShippingLabelModel>

    @Query(
        """
        SELECT * FROM ShippingLabelEntity
        WHERE localSiteId = :localSiteId
        AND remoteOrderId = :orderId
        AND remoteShippingLabelId = :remoteShippingLabelId
        LIMIT 1
        """
    )
    abstract suspend fun getShippingLabel(
        localSiteId: Int,
        orderId: Long,
        remoteShippingLabelId: Long
    ): WCShippingLabelModel?

    @Upsert
    abstract suspend fun upsertShippingLabels(shippingLabels: List<WCShippingLabelModel>)

    @Query(
        """
        DELETE FROM ShippingLabelEntity
        WHERE remoteOrderId = :orderId
        """
    )
    abstract suspend fun deleteShippingLabels(orderId: Long)
}
