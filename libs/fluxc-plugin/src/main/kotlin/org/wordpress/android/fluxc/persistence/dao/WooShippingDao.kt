package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.persistence.entity.WooShippingShipmentEntity

@Dao
abstract class WooShippingDao {
    @Upsert
    protected abstract suspend fun insertShipments(shipment: List<WooShippingShipmentEntity>)

    @Query("DELETE FROM WooShippingShipmentEntity WHERE localSiteId = :localSiteId AND orderId = :orderId")
    protected abstract suspend fun deleteShipments(
        localSiteId: LocalOrRemoteId.LocalId,
        orderId: LocalOrRemoteId.RemoteId
    )

    @Query("SELECT * FROM WooShippingShipmentEntity WHERE localSiteId = :localSiteId AND orderId = :orderId")
    abstract suspend fun getShipments(localSiteId: LocalOrRemoteId.LocalId, orderId: LocalOrRemoteId.RemoteId): List<WooShippingShipmentEntity>

    @Transaction
    open suspend fun replaceShipments(
        shipments: List<WooShippingShipmentEntity>
    ) {
        val localSiteId = shipments.firstOrNull()?.localSiteId ?: return
        val orderId = shipments.firstOrNull()?.orderId ?: return
        require(shipments.all { it.localSiteId == localSiteId && it.orderId == orderId }) {
            "All shipments must belong to the same order"
        }
        deleteShipments(localSiteId, orderId)
        insertShipments(shipments)
    }
}
