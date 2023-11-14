package com.woocommerce.android.ui.orders.tracking

import com.woocommerce.android.model.OrderShipmentProvider
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.ORDERS
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderShipmentProvidersPayload
import javax.inject.Inject

class OrderShipmentProvidersRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val orderStore: WCOrderStore
) {
    @Suppress("ReturnCount")
    suspend fun fetchOrderShipmentProviders(orderId: Long): List<OrderShipmentProvider>? {
        // Check db first
        val providersInDb = getShipmentProvidersFromDB()
        if (providersInDb.isNotEmpty()) {
            return providersInDb
        }

        // Fetch from API
        val order = orderStore.getOrderByIdAndSite(orderId, selectedSite.get())
        if (order == null) {
            WooLog.e(
                ORDERS,
                "Can't find order with id $orderId while trying to fetch shipment providers list"
            )
            return null
        }
        val payload = FetchOrderShipmentProvidersPayload(selectedSite.get(), order)
        val result = orderStore.fetchOrderShipmentProviders(payload)
        return when {
            result.isError -> {
                WooLog.e(ORDERS, "Error fetching shipment providers: ${result.error.message}")
                null
            }
            result.rowsAffected == 0 -> {
                WooLog.i(ORDERS, "No shipment providers fetched")
                emptyList()
            }
            else -> getShipmentProvidersFromDB()
        }
    }

    private fun getShipmentProvidersFromDB(): List<OrderShipmentProvider> =
        orderStore.getShipmentProvidersForSite(selectedSite.get()).map { it.toAppModel() }
}
