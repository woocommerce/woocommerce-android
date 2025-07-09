package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
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

    @Query("DELETE FROM WCRefunds WHERE localSiteId = :siteId AND orderId = :orderId")
    abstract suspend fun deleteRefundsForOrder(siteId: LocalId, orderId: RemoteId)

    @Query("DELETE FROM WCRefunds WHERE localSiteId = :siteId AND orderId = :orderId AND refundId = :refundId")
    abstract suspend fun deleteRefund(siteId: LocalId, orderId: RemoteId, refundId: RemoteId)

    @Query("SELECT * FROM WCRefunds WHERE localSiteId = :siteId AND orderId = :orderId")
    abstract suspend fun getRefundsForOrder(siteId: LocalId, orderId: RemoteId): List<RefundEntity>

    @Query("SELECT * FROM WCRefunds WHERE localSiteId = :siteId AND orderId = :orderId AND refundId = :refundId")
    abstract suspend fun getRefund(siteId: LocalId, orderId: RemoteId, refundId: RemoteId): RefundEntity?
}
