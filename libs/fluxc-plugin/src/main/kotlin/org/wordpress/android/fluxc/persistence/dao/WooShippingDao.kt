package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.persistence.entity.WooShippingLabelEntity
import org.wordpress.android.fluxc.persistence.entity.WooShippingShipmentEntity

@Dao
abstract class WooShippingDao {
    @Upsert
    abstract suspend fun insertLabel(label: WooShippingLabelEntity)

    @Upsert
    abstract suspend fun insertLabels(labels: List<WooShippingLabelEntity>)

    @Query("DELETE FROM WooShippingLabelEntity WHERE localSiteId = :localSiteId AND orderId = :orderId")
    protected abstract suspend fun deleteLabels(
        localSiteId: LocalOrRemoteId.LocalId,
        orderId: LocalOrRemoteId.RemoteId
    )

    @Query(
        """SELECT * FROM WooShippingLabelEntity WHERE localSiteId = :localSiteId AND orderId = :orderId
                ORDER BY createdDate DESC
                """
    )
    abstract suspend fun getLabels(
        localSiteId: LocalOrRemoteId.LocalId,
        orderId: LocalOrRemoteId.RemoteId
    ): List<WooShippingLabelEntity>

    @Query(
        """SELECT * FROM WooShippingLabelEntity WHERE localSiteId = :localSiteId AND orderId = :orderId
                AND status = 'PURCHASED' ORDER BY createdDate DESC"""
    )
    abstract suspend fun getPurchasedLabels(
        localSiteId: LocalOrRemoteId.LocalId,
        orderId: LocalOrRemoteId.RemoteId
    ): List<WooShippingLabelEntity>

    @Query(
        """SELECT * FROM WooShippingLabelEntity WHERE localSiteId = :localSiteId
              AND orderId = :orderId
              AND labelId = :labelId
              """
    )
    abstract suspend fun getLabel(
        localSiteId: LocalOrRemoteId.LocalId,
        orderId: LocalOrRemoteId.RemoteId,
        labelId: Long
    ): WooShippingLabelEntity?

    @Query(
        """SELECT * FROM WooShippingLabelEntity WHERE localSiteId = :localSiteId
              AND orderId = :orderId
              AND labelId = :labelId
              """
    )
    abstract fun observeLabel(
        localSiteId: LocalOrRemoteId.LocalId,
        orderId: LocalOrRemoteId.RemoteId,
        labelId: Long
    ): Flow<WooShippingLabelEntity?>

    @Transaction
    open suspend fun replaceLabels(
        labels: List<WooShippingLabelEntity>
    ) {
        val localSiteId = labels.firstOrNull()?.localSiteId ?: return
        val orderId = labels.firstOrNull()?.orderId ?: return
        require(labels.all { it.localSiteId == localSiteId && it.orderId == orderId }) {
            "All labels must belong to the same order"
        }
        deleteLabels(localSiteId, orderId)
        insertLabels(labels)
    }

    @Upsert
    protected abstract suspend fun insertShipments(shipment: List<WooShippingShipmentEntity>)

    @Query("DELETE FROM WooShippingShipmentEntity WHERE localSiteId = :localSiteId AND orderId = :orderId")
    protected abstract suspend fun deleteShipments(
        localSiteId: LocalOrRemoteId.LocalId,
        orderId: LocalOrRemoteId.RemoteId
    )

    @Query("SELECT * FROM WooShippingShipmentEntity WHERE localSiteId = :localSiteId AND orderId = :orderId")
    abstract suspend fun getShipments(
        localSiteId: LocalOrRemoteId.LocalId,
        orderId: LocalOrRemoteId.RemoteId
    ): List<WooShippingShipmentEntity>

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
