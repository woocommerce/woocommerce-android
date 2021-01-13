package com.woocommerce.android.ui.orders.list

import com.woocommerce.android.AppConstants
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.ORDERS
import com.woocommerce.android.util.suspendCoroutineWithTimeout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.store.WCGatewayStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderStatusOptionsPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderStatusOptionsChanged
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class OrderListRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val coroutineDispatchers: CoroutineDispatchers,
    private val orderStore: WCOrderStore,
    private val gatewayStore: WCGatewayStore,
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val TAG = "OrderListRepository"
    }

    private var isFetchingOrderStatusOptions = false
    private var isFetchingPaymentGateways = false
    private var continuationOrderStatus: Continuation<RequestResult>? = null

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    suspend fun fetchOrderStatusOptionsFromApi(): RequestResult {
        return if (!isFetchingOrderStatusOptions) {
            try {
                suspendCoroutineWithTimeout<RequestResult>(AppConstants.REQUEST_TIMEOUT) {
                    isFetchingOrderStatusOptions = true
                    continuationOrderStatus = it

                    dispatcher.dispatch(
                            WCOrderActionBuilder.newFetchOrderStatusOptionsAction(
                                    FetchOrderStatusOptionsPayload(selectedSite.get())
                            )
                    )
                } ?: RequestResult.ERROR // request timed out
            } catch (e: CancellationException) {
                WooLog.e(ORDERS, "TAG - Exception encountered while fetching order status options", e)
                RequestResult.ERROR
            }
        } else RequestResult.NO_ACTION_NEEDED
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
            if (!isFetchingPaymentGateways) {
                isFetchingPaymentGateways = true
                val result = gatewayStore.fetchAllGateways(selectedSite.get())
                isFetchingPaymentGateways = false
                if (result.isError) {
                    WooLog.e(ORDERS, "${result.error.type.name}: ${result.error.message}")
                    RequestResult.ERROR
                } else RequestResult.SUCCESS
            } else RequestResult.NO_ACTION_NEEDED
        }
    }

    /**
     * Checks to see if there are any orders in the db for the active store.
     *
     * @return True if there are orders in the db, else False
     */
    fun hasCachedOrdersForSite() = orderStore.hasCachedOrdersForSite(selectedSite.get())

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrderStatusOptionsChanged(event: OnOrderStatusOptionsChanged) {
        isFetchingOrderStatusOptions = false

        continuationOrderStatus?.let {
            if (event.isError) {
                WooLog.e(ORDERS,
                        "$TAG - Error fetching order status options from the api : ${event.error.message}")
                it.resume(RequestResult.ERROR)
            } else {
                it.resume(RequestResult.SUCCESS)
            }
            continuationOrderStatus = null
        }
    }
}
