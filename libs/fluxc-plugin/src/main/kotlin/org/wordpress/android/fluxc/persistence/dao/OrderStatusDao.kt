package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.WCOrderStatusModel

@Dao
internal abstract class OrderStatusDao {
    @Upsert
    abstract suspend fun upsertOrderStatuses(statuses: List<WCOrderStatusModel>)

    @Query(
        """
        SELECT * FROM OrderStatusEntity
        WHERE siteId = :siteId
        """
    )
    abstract suspend fun getOrderStatusOptions(siteId: LocalId): List<WCOrderStatusModel>

    @Query(
        """
        SELECT * FROM OrderStatusEntity
        WHERE siteId = :siteId
        AND statusKey = :statusKey
        """
    )
    abstract suspend fun getOrderStatusOption(
        siteId: LocalId,
        statusKey: String
    ): WCOrderStatusModel?

    @Delete
    abstract suspend fun deleteOrderStatuses(statuses: List<WCOrderStatusModel>)
}
