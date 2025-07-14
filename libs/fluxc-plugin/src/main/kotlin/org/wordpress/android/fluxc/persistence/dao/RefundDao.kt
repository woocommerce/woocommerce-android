package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.persistence.entity.RefundEntity

@Dao
internal abstract class RefundDao {

    @Upsert
    abstract suspend fun upsertRefund(refund: RefundEntity)

    @Upsert
    abstract suspend fun upsertRefunds(refunds: List<RefundEntity>)

    @Query("SELECT * FROM RefundEntity WHERE siteId = :siteId AND orderId = :orderId")
    abstract suspend fun getRefundsForOrder(siteId: LocalId, orderId: RemoteId): List<RefundEntity>

    @Query("SELECT * FROM RefundEntity WHERE siteId = :siteId AND orderId = :orderId AND refundId = :refundId")
    abstract suspend fun getRefund(siteId: LocalId, orderId: RemoteId, refundId: RemoteId): RefundEntity?

    @Transaction
    open suspend fun replaceRefundsForOrder(siteId: LocalId, orderId: RemoteId, refunds: List<RefundEntity>) {
        deleteRefundsForOrder(siteId, orderId)
        upsertRefunds(refunds)
    }

    @Query("DELETE FROM RefundEntity WHERE siteId = :siteId AND orderId = :orderId")
    protected abstract suspend fun deleteRefundsForOrder(siteId: LocalId, orderId: RemoteId)
}
