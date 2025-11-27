package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel

@Dao
abstract class OrderShipmentTrackingDao {

    @Upsert
    abstract suspend fun upsertShipmentTracking(tracking: WCOrderShipmentTrackingModel)

    @Query(
        """
        SELECT * FROM OrderShipmentTrackingEntity
        WHERE localSiteId = :siteId
        AND orderId = :orderId
        ORDER BY dateShipped DESC
        """
    )
    abstract suspend fun getShipmentTrackings(
        siteId: LocalId,
        orderId: RemoteId
    ): List<WCOrderShipmentTrackingModel>

    @Query(
        """
        SELECT * FROM OrderShipmentTrackingEntity
        WHERE localSiteId = :siteId
        AND orderId = :orderId
        AND trackingNumber = :trackingNumber
        LIMIT 1
        """
    )
    abstract suspend fun getShipmentTrackingByNumber(
        siteId: LocalId,
        orderId: RemoteId,
        trackingNumber: String
    ): WCOrderShipmentTrackingModel?

    @Delete
    abstract suspend fun deleteShipmentTracking(tracking: WCOrderShipmentTrackingModel)

    @Query(
        """
        DELETE FROM OrderShipmentTrackingEntity
        WHERE localSiteId = :siteId
        """
    )
    abstract suspend fun deleteShipmentTrackingsForSite(siteId: LocalId)
}
