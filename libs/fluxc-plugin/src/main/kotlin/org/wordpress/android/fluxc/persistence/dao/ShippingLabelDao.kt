package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel

@Dao
abstract class ShippingLabelDao {

    @Query(
        """
        SELECT * FROM ShippingLabelEntity
        WHERE localSiteId = :siteId
        AND remoteOrderId = :orderId
        """
    )
    abstract suspend fun getShippingLabels(
        siteId: LocalId,
        orderId: RemoteId
    ): List<WCShippingLabelModel>

    @Query(
        """
        SELECT * FROM ShippingLabelEntity
        WHERE localSiteId = :siteId
        AND remoteOrderId = :orderId
        AND remoteShippingLabelId = :shippingLabelId
        LIMIT 1
        """
    )
    abstract suspend fun getShippingLabel(
        siteId: LocalId,
        orderId: RemoteId,
        shippingLabelId: RemoteId
    ): WCShippingLabelModel?

    @Upsert
    abstract suspend fun upsertShippingLabels(shippingLabels: List<WCShippingLabelModel>)

    @Query(
        """
        DELETE FROM ShippingLabelEntity
        WHERE remoteOrderId = :orderId
        """
    )
    abstract suspend fun deleteShippingLabels(orderId: RemoteId)
}
