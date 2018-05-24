package com.woocommerce.android.ui.orders

import android.content.Context
import com.woocommerce.android.tools.SelectedSite
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDERS
import org.wordpress.android.fluxc.action.WCOrderAction.UPDATE_ORDER_STATUS
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.util.NetworkUtils
import javax.inject.Inject

class OrderListPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite
) : OrderListContract.Presenter {
    private var orderView: OrderListContract.View? = null

    override fun takeView(view: OrderListContract.View) {
        orderView = view
        dispatcher.register(this)
    }

    override fun dropView() {
        orderView = null
        dispatcher.unregister(this)
    }

    override fun loadOrders(context: Context?, forceRefresh: Boolean) {
        if (context == null) return

        orderView?.setLoadingIndicator(true)
        if (forceRefresh) {
            if (NetworkUtils.isNetworkAvailable(context)) {
                val payload = FetchOrdersPayload(selectedSite.get())
                dispatcher.dispatch(WCOrderActionBuilder.newFetchOrdersAction(payload))
            } else {
                // Display any orders we have in the database and notify user of
                // no connectivity.
                fetchAndLoadOrdersFromDb(isForceRefresh = false)
                orderView?.showNetworkErrorFetchOrders()
            }
        } else {
            fetchAndLoadOrdersFromDb(isForceRefresh = false)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        if (event.isError) {
            // Basic error messaging is handled by the hosting activity
            orderView?.setLoadingIndicator(false)
            return
        }

        when (event.causeOfChange) {
            FETCH_ORDERS -> fetchAndLoadOrdersFromDb(true)
            // A child fragment made a change that requires a data refresh.
            UPDATE_ORDER_STATUS -> orderView?.refreshFragmentState()
            else -> {}
        }
    }

    override fun openOrderDetail(order: WCOrderModel) {
        orderView?.openOrderDetail(order, null)
    }

    /**
     * Fetch orders from the local database.
     *
     * @param isForceRefresh True if orders were refreshed from the API, else false.
     */
    private fun fetchAndLoadOrdersFromDb(isForceRefresh: Boolean) {
        val orders = orderStore.getOrdersForSite(selectedSite.get())
        if (orders.count() > 0) {
            orderView?.showOrders(orders, isForceRefresh)
        } else {
            orderView?.showNoOrders()
        }
        orderView?.setLoadingIndicator(false)
    }
}
