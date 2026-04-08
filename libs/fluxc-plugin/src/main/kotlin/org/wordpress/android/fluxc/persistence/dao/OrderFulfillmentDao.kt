package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCOrderFulfillmentModel

@Dao
abstract class OrderFulfillmentDao {

    @Upsert
    abstract suspend fun upsertOrderFulfillment(fulfillment: WCOrderFulfillmentModel)

    @Query(
        """
        SELECT * FROM OrderFulfillmentEntity
        WHERE localSiteId = :siteId
        AND orderId = :orderId
        """
    )
    abstract suspend fun getOrderFulfillments(
        siteId: LocalId,
        orderId: RemoteId
    ): List<WCOrderFulfillmentModel>

    @Delete
    abstract suspend fun deleteOrderFulfillment(fulfillment: WCOrderFulfillmentModel)
}
