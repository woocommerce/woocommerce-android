package com.woocommerce.android.ui.orders.tracking

import com.woocommerce.android.AppConstants
import com.woocommerce.android.model.OrderShipmentProvider
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.tracking.OrderShipmentProvidersRepository.RequesResult.EMPTY
import com.woocommerce.android.ui.orders.tracking.OrderShipmentProvidersRepository.RequesResult.ERROR
import com.woocommerce.android.ui.orders.tracking.OrderShipmentProvidersRepository.RequesResult.SUCCESS
import com.woocommerce.android.util.ContinuationWrapper
import com.woocommerce.android.util.ContinuationWrapper.ContinuationResult.Cancellation
import com.woocommerce.android.util.ContinuationWrapper.ContinuationResult.Success
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.ORDERS
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.model.order.toIdSet
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderShipmentProvidersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderShipmentProvidersChanged
import javax.inject.Inject

class OrderShipmentProvidersRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val orderStore: WCOrderStore,
    private val dispatcher: Dispatcher
) {
    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    private var continuationFetchTrackingProviders = ContinuationWrapper<RequesResult>(ORDERS)

    suspend fun fetchOrderShipmentProviders(orderIdentifier: OrderIdentifier): List<OrderShipmentProvider>? {
        // Check db first
        val providersInDb = getShipmentProvidersFromDB()
        if (providersInDb.isNotEmpty()) {
            return providersInDb
        }

        // Fetch from API
        val order = orderStore.getOrderByIdentifier(orderIdentifier)
        if (order == null) {
            WooLog.e(
                    ORDERS, "Can't find order with id ${orderIdentifier.toIdSet().remoteOrderId} " +
                    "while trying to fetch shipment providers list"
            )
            return null
        }
        val result = continuationFetchTrackingProviders.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {
            val payload = FetchOrderShipmentProvidersPayload(selectedSite.get(), order)
            dispatcher.dispatch(WCOrderActionBuilder.newFetchOrderShipmentProvidersAction(payload))
        }
        return when (result) {
            is Cancellation -> null
            is Success -> when (result.value) {
                SUCCESS -> getShipmentProvidersFromDB()
                EMPTY -> emptyList()
                else -> null
            }
        }
    }

    private fun getShipmentProvidersFromDB(): List<OrderShipmentProvider> =
            orderStore.getShipmentProvidersForSite(selectedSite.get()).map { it.toAppModel() }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrderShipmentProviderChanged(event: OnOrderShipmentProvidersChanged) {
        when {
            event.isError -> {
                WooLog.e(ORDERS, "Error fetching shipment providers : ${event.error.message}")
                continuationFetchTrackingProviders.continueWith(ERROR)
            }
            event.rowsAffected == 0 -> {
                WooLog.e(ORDERS, "Error fetching shipment providers : empty list")
                continuationFetchTrackingProviders.continueWith(EMPTY)
            }
            else -> continuationFetchTrackingProviders.continueWith(SUCCESS)
        }
    }

    private enum class RequesResult {
        SUCCESS, ERROR, EMPTY
    }
}
