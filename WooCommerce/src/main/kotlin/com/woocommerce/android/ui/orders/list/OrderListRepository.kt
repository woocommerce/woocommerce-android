package com.woocommerce.android.ui.orders.list

import com.woocommerce.android.AppConstants
import com.woocommerce.android.WooException
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.ContinuationWrapper
import com.woocommerce.android.util.ContinuationWrapper.ContinuationResult.Cancellation
import com.woocommerce.android.util.ContinuationWrapper.ContinuationResult.Success
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.ORDERS
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.gateways.WCGatewayModel
import org.wordpress.android.fluxc.store.OrderUpdateStore
import org.wordpress.android.fluxc.store.WCGatewayStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderStatusOptionsPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderStatusOptionsChanged
import javax.inject.Inject

class OrderListRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val orderStore: WCOrderStore,
    private val orderUpdateStore: OrderUpdateStore,
    private val gatewayStore: WCGatewayStore,
    private val selectedSite: SelectedSite,
    private val orderMapper: OrderMapper,
) {
    companion object {
        private const val TAG = "OrderListRepository"
    }

    private var isFetchingOrderStatusOptions = false
    private var isFetchingPaymentGateways = false
    private var continuationOrderStatus = ContinuationWrapper<RequestResult>(ORDERS)

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    suspend fun fetchOrderStatusOptionsFromApi(): RequestResult {
        return if (!isFetchingOrderStatusOptions && selectedSite.exists()) {
            val result = continuationOrderStatus.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {
                isFetchingOrderStatusOptions = true

                dispatcher.dispatch(
                    WCOrderActionBuilder.newFetchOrderStatusOptionsAction(
                        FetchOrderStatusOptionsPayload(selectedSite.get())
                    )
                )
            }
            return when (result) {
                is Cancellation -> RequestResult.ERROR
                is Success -> result.value
            }
        } else {
            RequestResult.NO_ACTION_NEEDED
        }
    }

    suspend fun getCachedOrderStatusOptions(): Map<String, WCOrderStatusModel> {
        return withContext(coroutineDispatchers.io) {
            if (selectedSite.exists()) {
                val statusOptions = orderStore.getOrderStatusOptionsForSite(selectedSite.get())
                if (statusOptions.isNotEmpty()) {
                    statusOptions.map { it.statusKey to it }.toMap()
                } else {
                    emptyMap()
                }
            } else {
                WooLog.w(ORDERS, "No site selected - unable to load order status options")
                emptyMap()
            }
        }
    }

    suspend fun fetchPaymentGateways(): RequestResult {
        return withContext(coroutineDispatchers.io) {
            if (!isFetchingPaymentGateways && selectedSite.exists()) {
                isFetchingPaymentGateways = true
                val result = gatewayStore.fetchAllGateways(selectedSite.get())
                isFetchingPaymentGateways = false
                if (result.isError) {
                    WooLog.e(ORDERS, "${result.error.type.name}: ${result.error.message}")
                    RequestResult.ERROR
                } else {
                    RequestResult.SUCCESS
                }
            } else {
                RequestResult.NO_ACTION_NEEDED
            }
        }
    }

    fun getAllPaymentGateways(site: SiteModel): List<WCGatewayModel> {
        return gatewayStore.getAllGateways(site)
    }

    suspend fun trashOrder(orderId: Long): Result<Unit> {
        val result = orderUpdateStore.deleteOrder(
            orderId = orderId,
            site = selectedSite.get(),
            trash = true
        )

        return if (result.isError) {
            WooLog.e(ORDERS, "Error trashing order: ${result.error.message}")
            Result.failure(WooException(result.error))
        } else {
            Result.success(Unit)
        }
    }

    fun observeTopOrders(count: Int, isForced: Boolean, statusFilter: Order.Status? = null) = flow {
        if (!isForced) {
            orderStore.getOrdersForSite(selectedSite.get())
                .asSequence()
                .filter { statusFilter == null || it.status == statusFilter.value }
                .sortedByDescending { it.dateCreated }
                .take(count)
                .map { orderMapper.toAppModel(it) }
                .let { orders ->
                    emit(Result.success(orders.toList()))
                }
        }

        val result = orderStore.fetchOrders(
            site = selectedSite.get(),
            count = count,
            statusFilter = statusFilter?.value,
            deleteOldData = false
        )

        if (result.isError) {
            WooLog.e(ORDERS, "Error fetching top orders: ${result.error.message}")
            emit(Result.failure(WooException(result.error)))
        } else {
            val orderList = result.model?.map { orderMapper.toAppModel(it) } ?: emptyList()
            emit(Result.success(orderList))
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrderStatusOptionsChanged(event: OnOrderStatusOptionsChanged) {
        isFetchingOrderStatusOptions = false

        if (event.isError) {
            WooLog.e(
                ORDERS,
                "$TAG - Error fetching order status options from the api : ${event.error.message}"
            )
            continuationOrderStatus.continueWith(RequestResult.ERROR)
        } else {
            continuationOrderStatus.continueWith(RequestResult.SUCCESS)
        }
    }
}
