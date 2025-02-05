package org.wordpress.android.fluxc.persistence.dao

import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ListActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrdersDaoDecorator @Inject constructor(
    private val dispatcher: Dispatcher,
    private val ordersDao: OrdersDao,
) {
    suspend fun updateLocalOrder(
        orderId: Long,
        localSiteId: LocalOrRemoteId.LocalId,
        updateOrder: OrderEntity.() -> OrderEntity
    ) {
        getOrder(orderId, localSiteId)
            ?.let(updateOrder)
            ?.let { insertOrUpdateOrder(it) }
    }

    @Suppress("unused")
    suspend fun getAllOrders(): List<OrderEntity> = ordersDao.getAllOrders()

    suspend fun insertOrUpdateOrder(
        order: OrderEntity,
        listUpdateStrategy: ListUpdateStrategy = ListUpdateStrategy.DEFAULT
    ): UpdateOrderResult {
        val orderBeforeUpdate = ordersDao.getOrder(order.orderId, order.localSiteId)
        ordersDao.insertOrUpdateOrder(order)

        when (listUpdateStrategy) {
            ListUpdateStrategy.REFRESH -> {
                emitRefreshListEvent(order.localSiteId)
            }

            ListUpdateStrategy.INVALIDATE -> {
                emitInvalidateListEvent(order.localSiteId)
            }

            ListUpdateStrategy.DEFAULT -> {
                // Draft orders are not returned from the API. We need to re-fetch order list from the API
                // when the order is new or its status changed from draft to another status.
                val orderIsNewOrMovingFromDraft = orderBeforeUpdate == null
                        || (order.status != "auto-draft" && orderBeforeUpdate.status == "auto-draft")
                if (orderIsNewOrMovingFromDraft) {
                    // Re-fetch order list
                    emitRefreshListEvent(order.localSiteId)
                } else {
                    // Re-load order list from local db
                    emitInvalidateListEvent(order.localSiteId)
                }
            }

            ListUpdateStrategy.SUPPRESS -> {} // Don't update the list
        }
        return when {
            orderBeforeUpdate == null -> UpdateOrderResult.INSERTED
            // Ignore changes to the modifiedDate - it's updated even when there are no other changes
            // and results in an infinite loop.
            order.copy(dateModified = "") != orderBeforeUpdate.copy(dateModified = "") -> UpdateOrderResult.UPDATED
            else -> UpdateOrderResult.UNCHANGED
        }
    }

    suspend fun getOrder(orderId: Long, localSiteId: LocalOrRemoteId.LocalId): OrderEntity? =
        ordersDao.getOrder(orderId, localSiteId)

    @Suppress("unused")
    fun observeOrder(orderId: Long, localSiteId: LocalOrRemoteId.LocalId): Flow<OrderEntity?> =
        ordersDao.observeOrder(orderId, localSiteId)

    suspend fun getPaidOrdersForSiteDesc(
        localSiteId: LocalOrRemoteId.LocalId,
        status: List<String> = listOf(CoreOrderStatus.COMPLETED.value)
    ): List<OrderEntity> = ordersDao.getPaidOrdersForSiteDesc(localSiteId, status)

    suspend fun getOrdersForSite(
        localSiteId: LocalOrRemoteId.LocalId, status: List<String>
    ): List<OrderEntity> = ordersDao.getOrdersForSite(localSiteId, status)

    suspend fun getOrdersForSite(localSiteId: LocalOrRemoteId.LocalId): List<OrderEntity> =
        ordersDao.getOrdersForSite(localSiteId)

    fun observeOrdersForSite(localSiteId: LocalOrRemoteId.LocalId): Flow<List<OrderEntity>> =
        ordersDao.observeOrdersForSite(localSiteId)

    fun observeOrdersForSite(
        localSiteId: LocalOrRemoteId.LocalId, status: List<String>
    ): Flow<List<OrderEntity>> = ordersDao.observeOrdersForSite(localSiteId, status)

    fun getOrdersForSiteByRemoteIds(
        localSiteId: LocalOrRemoteId.LocalId, orderIds: List<Long>
    ): List<OrderEntity> = ordersDao.getOrdersForSiteByRemoteIds(localSiteId, orderIds)

    fun deleteOrdersForSite(localSiteId: LocalOrRemoteId.LocalId) {
        ordersDao.deleteOrdersForSite(localSiteId)
        emitInvalidateListEvent(localSiteId)
    }

    fun getOrderCountForSite(localSiteId: LocalOrRemoteId.LocalId): Int =
        ordersDao.getOrderCountForSite(localSiteId)

    fun observeOrderCountForSite(localSiteId: LocalOrRemoteId.LocalId, status: List<String>?): Flow<Int> {
        return if (status == null) {
            ordersDao.observeOrderCountForSite(localSiteId)
        } else {
            ordersDao.observeOrderCountForSite(localSiteId, status)
        }
    }

    suspend fun deleteOrder(localSiteId: LocalOrRemoteId.LocalId, orderId: Long) {
        ordersDao.deleteOrder(localSiteId, orderId)
        emitInvalidateListEvent(localSiteId)
    }

    /**
     * Emit DataInvalidated event - the ListStore component reloads the data from the DB.
     */
    private fun emitInvalidateListEvent(localSiteId: LocalOrRemoteId.LocalId) {
        val listTypeIdentifier = WCOrderListDescriptor.calculateTypeIdentifier(
            localSiteId = localSiteId.value
        )
        dispatcher.dispatch(ListActionBuilder.newListDataInvalidatedAction(listTypeIdentifier))
    }

    /**
     * Emit RefreshList event - the ListStore component refetches the list of order ids from remote.
     */
    private fun emitRefreshListEvent(localSiteId: LocalOrRemoteId.LocalId) {
        val listTypeIdentifier = WCOrderListDescriptor.calculateTypeIdentifier(
            localSiteId = localSiteId.value
        )
        dispatcher.dispatch(ListActionBuilder.newListRequiresRefreshAction(listTypeIdentifier))
    }

    enum class ListUpdateStrategy {
        DEFAULT,
        // Re-fetch the order list
        REFRESH,
        // Re-load the order list from the DB
        INVALIDATE,
        // Do not update the list
        SUPPRESS
    }

    enum class UpdateOrderResult {
        UPDATED, INSERTED, UNCHANGED
    }
}


