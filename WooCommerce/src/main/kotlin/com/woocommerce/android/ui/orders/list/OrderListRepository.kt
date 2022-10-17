package com.woocommerce.android.ui.orders.list

import com.woocommerce.android.AppConstants
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.ContinuationWrapper
import com.woocommerce.android.util.ContinuationWrapper.ContinuationResult.Cancellation
import com.woocommerce.android.util.ContinuationWrapper.ContinuationResult.Success
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.ORDERS
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
            if (!isFetchingPaymentGateways && selectedSite.exists()) {
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
