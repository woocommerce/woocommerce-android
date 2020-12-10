package com.woocommerce.android.ui.orders.tracking

import com.woocommerce.android.model.OrderShipmentProvider
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.tracking.OrderShipmentProvidersRepository.RequesResult
import com.woocommerce.android.ui.orders.tracking.OrderShipmentProvidersRepository.RequesResult.EMPTY
import com.woocommerce.android.ui.orders.tracking.OrderShipmentProvidersRepository.RequesResult.ERROR
import com.woocommerce.android.ui.orders.tracking.OrderShipmentProvidersRepository.RequesResult.SUCCESS
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.ORDERS
import com.woocommerce.android.util.suspendCancellableCoroutineWithTimeout
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.model.order.toIdSet
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderShipmentProvidersPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderShipmentProvidersChanged
import kotlin.coroutines.resume

class OrderShipmentProvidersRepository(
    private val selectedSite: SelectedSite,
    private val orderStore: WCOrderStore,
    private val dispatcher: Dispatcher
) {
    companion object {
        private const val ACTION_TIMEOUT = 10_000L
    }

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    private var continuationFetchTrackingProviders: CancellableContinuation<RequesResult>? = null

    suspend fun fetchOrderShipmentProviders(orderIdentifier: OrderIdentifier): List<OrderShipmentProvider>? {
        continuationFetchTrackingProviders?.cancel()
        val order = orderStore.getOrderByIdentifier(orderIdentifier)
        if (order == null) {
            WooLog.e(
                ORDERS, "Can't find order with id ${orderIdentifier.toIdSet().remoteOrderId} " +
                "while trying to fetch shipment providers list"
            )
            return null
        }
        try {
            val result = suspendCancellableCoroutineWithTimeout<RequesResult>(ACTION_TIMEOUT) {
                continuationFetchTrackingProviders = it

                val payload = FetchOrderShipmentProvidersPayload(selectedSite.get(), order)
                dispatcher.dispatch(WCOrderActionBuilder.newFetchOrderShipmentProvidersAction(payload))
            }
            return when (result) {
                SUCCESS -> getShipmentProvidersFromDB()
                EMPTY -> emptyList()
                else -> null
            }
        } catch (e: CancellationException) {
            WooLog.e(
                ORDERS, "CancellationException while fetching shipment providers list for " +
                "order ${orderIdentifier.toIdSet().remoteOrderId}"
            )
            return null
        }
    }

    private fun getShipmentProvidersFromDB(): List<OrderShipmentProvider> =
        orderStore.getShipmentProvidersForSite(selectedSite.get()).map { it.toAppModel() }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOrderShipmentProviderChanged(event: OnOrderShipmentProvidersChanged) {
        if (event.isError) {
            WooLog.e(ORDERS, "Error fetching shipment providers : ${event.error.message}")
            continuationFetchTrackingProviders?.resume(ERROR)
        } else if (event.rowsAffected == 0) {
            WooLog.e(ORDERS, "Error fetching shipment providers : empty list")
            continuationFetchTrackingProviders?.resume(EMPTY)
        } else {
            continuationFetchTrackingProviders?.resume(SUCCESS)
        }
    }

    private enum class RequesResult {
        SUCCESS, ERROR, EMPTY
    }
}
