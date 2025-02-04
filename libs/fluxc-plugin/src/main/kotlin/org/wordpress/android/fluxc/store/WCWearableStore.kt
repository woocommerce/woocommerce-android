package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.persistence.dao.OrdersDaoDecorator
import org.wordpress.android.fluxc.persistence.dao.OrdersDaoDecorator.ListUpdateStrategy.SUPPRESS
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T.API
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This store serves specifically for the wearable app.
 *
 * It should not be used in the main Woo mobile app and relies on the scenario of
 * the Watch receiving data from the Phone instead of the Woo Store API.
 */
@Singleton
class WCWearableStore @Inject constructor(
    private val coroutineEngine: CoroutineEngine,
    private val wcOrderRestClient: OrderRestClient,
    private val ordersDaoDecorator: OrdersDaoDecorator,
    private val accessToken: AccessToken
) {
    fun updateToken(token: String) { accessToken.set(token) }

    @Suppress("SpreadOperator")
    suspend fun fetchOrders(
        site: SiteModel,
        offset: Int = 0,
        filterByStatus: String? = null,
        shouldStoreData: Boolean = false
    ): OrdersForWearablesResult {
        return coroutineEngine.withDefaultContext(API, this, "fetchOrders") {
            val result = wcOrderRestClient.fetchOrdersSync(site, offset, filterByStatus)
            when {
                result.isError -> {
                    ordersDaoDecorator.getAllOrders()
                        .takeIf { it.isNotEmpty() }
                        ?.let { OrdersForWearablesResult.Success(result.orders) }
                        ?: OrdersForWearablesResult.Failure(OrderError())
                }
                else -> {
                    if (shouldStoreData) {
                        ordersDaoDecorator.deleteOrdersForSite(site.localId())
                        insertOrders(result.orders)
                    }
                    OrdersForWearablesResult.Success(result.orders)
                }
            }
        }
    }

    suspend fun insertOrders(orders: List<OrderEntity>) {
        orders.forEach {
            ordersDaoDecorator.insertOrUpdateOrder(it, SUPPRESS)
        }
    }

    sealed class OrdersForWearablesResult {
        data class Success(val orders: List<OrderEntity>) : OrdersForWearablesResult()
        data class Failure(val error: OrderError) : OrdersForWearablesResult()
    }
}
