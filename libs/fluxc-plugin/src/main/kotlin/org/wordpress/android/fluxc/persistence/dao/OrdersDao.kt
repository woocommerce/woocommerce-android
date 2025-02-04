package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus

/**
 * ⚠️AVOID USING THIS CLASS DIRECTLY -
 * Use [OrdersDaoDecorator] to ensure the [org.wordpress.android.fluxc.store.ListStore] component
 * keeps receiving events.
 */
@Dao
abstract class OrdersDao {
    @Query("SELECT * FROM OrderEntity")
    abstract suspend fun getAllOrders(): List<OrderEntity>

    @Insert(onConflict = REPLACE)
    abstract fun insertOrUpdateOrder(order: OrderEntity)

    @Query("SELECT * FROM OrderEntity WHERE orderId = :orderId AND localSiteId = :localSiteId")
    abstract suspend fun getOrder(orderId: Long, localSiteId: LocalId): OrderEntity?

    @Query("SELECT * FROM OrderEntity WHERE orderId = :orderId AND localSiteId = :localSiteId")
    abstract fun observeOrder(orderId: Long, localSiteId: LocalId): Flow<OrderEntity?>

    @Transaction
    open suspend fun updateLocalOrder(
        orderId: Long,
        localSiteId: LocalId,
        updateOrder: OrderEntity.() -> OrderEntity
    ) {
        getOrder(orderId, localSiteId)
            ?.let(updateOrder)
            ?.let { insertOrUpdateOrder(it) }
    }

    @Query("SELECT * FROM OrderEntity WHERE localSiteId = :localSiteId AND status IN (:status) ORDER BY datePaid DESC")
    abstract suspend fun getPaidOrdersForSiteDesc(
        localSiteId: LocalId,
        status: List<String> = listOf(CoreOrderStatus.COMPLETED.value)
    ): List<OrderEntity>

    @Query("SELECT * FROM OrderEntity WHERE localSiteId = :localSiteId AND status IN (:status)")
    abstract suspend fun getOrdersForSite(localSiteId: LocalId, status: List<String>): List<OrderEntity>

    @Query("SELECT * FROM OrderEntity WHERE localSiteId = :localSiteId")
    abstract suspend fun getOrdersForSite(localSiteId: LocalId): List<OrderEntity>

    @Query("SELECT * FROM OrderEntity WHERE localSiteId = :localSiteId")
    abstract fun observeOrdersForSite(localSiteId: LocalId): Flow<List<OrderEntity>>

    @Query("SELECT * FROM OrderEntity WHERE localSiteId = :localSiteId AND status IN (:status)")
    abstract fun observeOrdersForSite(localSiteId: LocalId, status: List<String>): Flow<List<OrderEntity>>

    @Query("SELECT * FROM OrderEntity WHERE localSiteId = :localSiteId AND orderId IN (:orderIds)")
    abstract fun getOrdersForSiteByRemoteIds(
        localSiteId: LocalId,
        orderIds: List<Long>
    ): List<OrderEntity>

    @Query("DELETE FROM OrderEntity WHERE localSiteId = :localSiteId")
    abstract fun deleteOrdersForSite(localSiteId: LocalId)

    @Query("SELECT COUNT(*) FROM OrderEntity WHERE localSiteId = :localSiteId")
    abstract fun getOrderCountForSite(localSiteId: LocalId): Int

    @Query("SELECT COUNT(*) FROM OrderEntity WHERE localSiteId = :localSiteId AND status IN (:status)")
    abstract fun observeOrderCountForSite(localSiteId: LocalId, status: List<String>): Flow<Int>

    @Query("SELECT COUNT(*) FROM OrderEntity WHERE localSiteId = :localSiteId")
    abstract fun observeOrderCountForSite(localSiteId: LocalId): Flow<Int>

    @Query("DELETE FROM OrderEntity WHERE localSiteId = :localSiteId AND orderId = :orderId")
    abstract suspend fun deleteOrder(localSiteId: LocalId, orderId: Long)
}
