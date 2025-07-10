package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCOrderSummaryModel

@Dao
internal abstract class OrderSummaryDao {

    @Upsert
    abstract suspend fun upsertOrderSummaries(orderSummaries: List<WCOrderSummaryModel>)

    /**
     * Returns a list of [WCOrderSummaryModel]s that match the [remoteOrderIds] provided. This method uses
     * Kotlin's chunked functionality to ensure we don't crash with the "SQLiteException: too many SQL variables"
     * exception.
     */
    suspend fun getOrderSummariesChunked(siteId: LocalId, remoteOrderIds: List<RemoteId>): List<WCOrderSummaryModel> {
        return remoteOrderIds.chunked(CHUNK_SIZE)
            .flatMap { chunk ->
                getOrderSummariesForRemoteIds(siteId, chunk)
            }
    }

    @Query("SELECT * FROM OrderSummaryEntity WHERE siteId = :siteId AND orderId IN (:remoteOrderIds)")
    protected abstract suspend fun getOrderSummariesForRemoteIds(
        siteId: LocalId,
        remoteOrderIds: List<RemoteId>
    ): List<WCOrderSummaryModel>

    @Query("DELETE FROM OrderSummaryEntity WHERE siteId = :siteId AND orderId = :remoteOrderId")
    abstract suspend fun deleteOrderSummaryById(siteId: LocalId, remoteOrderId: RemoteId)

    private companion object {
        const val CHUNK_SIZE = 200
    }
}
