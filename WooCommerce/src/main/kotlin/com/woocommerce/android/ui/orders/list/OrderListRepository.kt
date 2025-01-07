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
        private const val ORDER_STATUS_TRASH = "trash"
        private const val BULK_UPDATE_ORDER_STATUS_ALL_FAILED = "Unable to update any orders."
        private const val BULK_UPDATE_ORDER_NOTHING_UPDATED = "No orders were updated."
        private const val BULK_UPDATE_ORDER_NO_RESPONSE = "No response received."
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
                    statusOptions.associateBy { it.statusKey }
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

    suspend fun hasOrdersLocally(statusFilter: Order.Status? = null) =
        orderStore.getOrdersForSite(selectedSite.get())
            .any { statusFilter == null || it.status == statusFilter.value }

    fun observeTopOrders(count: Int, isForced: Boolean, statusFilter: Order.Status? = null) = flow {
        if (!isForced) {
            orderStore.getOrdersForSite(selectedSite.get())
                .asSequence()
                .filter { it.status != ORDER_STATUS_TRASH && (statusFilter == null || it.status == statusFilter.value) }
                .sortedByDescending { it.dateCreated }
                .take(count)
                .map { orderMapper.toAppModel(it) }
                .toList()
                .takeIf { it.isNotEmpty() }
                ?.let { orders ->
                    emit(Result.success(orders))
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

    suspend fun bulkUpdateOrderStatus(orderIds: List<Long>, newStatus: Order.Status): Result<Unit> {
        val result = orderStore.batchUpdateOrdersStatus(
            site = selectedSite.get(),
            orderIds = orderIds,
            newStatus = WCOrderStatusModel(statusKey = newStatus.value)
        )

        return if (result.isError) {
            WooLog.e(ORDERS, "Error bulk updating order status: ${result.error.message}")
            Result.failure(WooException(result.error))
        } else {
            result.model?.let {
                logBulkOrderUpdateResults(it)

                // We want to return success if at least one order was updated.
                // However, if:
                // - there's no updated orders but there are failed orders, return failure
                // - there's no updated orders and no failed orders, return failure
                when {
                    it.failedOrders.isNotEmpty() && it.updatedOrders.isEmpty() ->
                        Result.failure(Exception(BULK_UPDATE_ORDER_STATUS_ALL_FAILED))

                    it.failedOrders.isEmpty() && it.updatedOrders.isEmpty() ->
                        Result.failure(Exception(BULK_UPDATE_ORDER_NOTHING_UPDATED))

                    else -> Result.success(Unit)
                }
            } ?: Result.failure(Exception(BULK_UPDATE_ORDER_NO_RESPONSE))
        }
    }

    private fun logBulkOrderUpdateResults(model: WCOrderStore.UpdateOrdersStatusResult) {
        if (model.updatedOrders.isNotEmpty()) {
            WooLog.i(ORDERS, "Successfully updated ${model.updatedOrders.size} orders")
        }
        if (model.failedOrders.isNotEmpty()) {
            model.failedOrders.forEach { failed ->
                WooLog.e(
                    ORDERS,
                    "Failed to update order ${failed.id}: " +
                        "[Code: ${failed.errorCode}, Status: ${failed.errorStatus}] ${failed.errorMessage}"
                )
            }
        }
    }
}
