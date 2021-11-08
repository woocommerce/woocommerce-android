package com.woocommerce.android.ui.orders.tracking

import com.woocommerce.android.model.OrderShipmentProvider
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.ORDERS
import javax.inject.Inject
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.model.order.toIdSet
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderShipmentProvidersPayload

class OrderShipmentProvidersRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val orderStore: WCOrderStore
) {
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
                ORDERS,
                "Can't find order with id ${orderIdentifier.toIdSet().remoteOrderId} " +
                    "while trying to fetch shipment providers list"
            )
            return null
        }
        val payload = FetchOrderShipmentProvidersPayload(selectedSite.get(), order)
        val result = orderStore.fetchOrderShipmentProviders(payload)
        return when {
            result.rowsAffected == 0 -> {
                WooLog.e(ORDERS, "Error fetching shipment providers : empty list")
                emptyList()
            }
            result.isError -> {
                WooLog.e(ORDERS, "Error fetching shipment providers : ${result.error.message}")
                null
            }
            else -> getShipmentProvidersFromDB()
        }
    }

    private fun getShipmentProvidersFromDB(): List<OrderShipmentProvider> =
        orderStore.getShipmentProvidersForSite(selectedSite.get()).map { it.toAppModel() }
}
