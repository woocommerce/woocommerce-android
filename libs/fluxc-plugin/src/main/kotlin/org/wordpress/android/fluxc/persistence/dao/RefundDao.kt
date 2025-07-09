package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import org.wordpress.android.fluxc.persistence.entity.RefundEntity

@Dao
internal abstract class RefundDao {

    @Upsert
    abstract suspend fun upsertRefund(refund: RefundEntity)

    @Upsert
    abstract suspend fun upsertRefunds(refunds: List<RefundEntity>)

    @Query("DELETE FROM WCRefunds WHERE localSiteId = :localSiteId AND orderId = :orderId")
    abstract suspend fun deleteRefundsForOrder(localSiteId: Int, orderId: Long)

    @Query("DELETE FROM WCRefunds WHERE localSiteId = :localSiteId AND orderId = :orderId AND refundId = :refundId")
    abstract suspend fun deleteRefund(localSiteId: Int, orderId: Long, refundId: Long)

    @Query("SELECT * FROM WCRefunds WHERE localSiteId = :localSiteId AND orderId = :orderId")
    abstract suspend fun getRefundsForOrder(localSiteId: Int, orderId: Long): List<RefundEntity>

    @Query("SELECT * FROM WCRefunds WHERE localSiteId = :localSiteId AND orderId = :orderId AND refundId = :refundId")
    abstract suspend fun getRefund(localSiteId: Int, orderId: Long, refundId: Long): RefundEntity?

}
