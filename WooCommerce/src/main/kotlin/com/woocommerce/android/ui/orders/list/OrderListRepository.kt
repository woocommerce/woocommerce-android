package com.woocommerce.android.ui.orders.list

import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.ORDERS
import com.woocommerce.android.util.suspendCoroutineWithTimeout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderStatusOptionsPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderStatusOptionsChanged
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class OrderListRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val TAG = "OrderListRepository"
        private const val ACTION_TIMEOUT = 10L * 1000
    }

    private var isFetchingOrderStatusOptions = false
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
                suspendCoroutineWithTimeout<RequestResult>(ACTION_TIMEOUT) {
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
        return withContext(Dispatchers.IO) {
            val statusOptions = orderStore.getOrderStatusOptionsForSite(selectedSite.get())
            if (statusOptions.isNotEmpty()) {
                statusOptions.map { it.statusKey to it }.toMap()
            } else {
                emptyMap()
            }
        }
    }

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
