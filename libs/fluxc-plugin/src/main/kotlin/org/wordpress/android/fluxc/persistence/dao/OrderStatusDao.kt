package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.WCOrderStatusModel

@Dao
internal abstract class OrderStatusDao {
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

    @Transaction
    open suspend fun replaceAll(siteId: LocalId, statuses: List<WCOrderStatusModel>) {
        deleteOrderStatuses(siteId)
        upsertOrderStatuses(statuses)
    }

    @Upsert
    protected abstract suspend fun upsertOrderStatuses(statuses: List<WCOrderStatusModel>)

    @Query("DELETE FROM OrderStatusEntity WHERE siteId = :siteId")
    protected abstract suspend fun deleteOrderStatuses(siteId: LocalId)
}
