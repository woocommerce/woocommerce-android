package com.woocommerce.android.ui.orders.tracking

import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.tracking.OrderShipmentProvidersRepository.OrderShipmentProvidersFetchResult.CarriersFetched
import com.woocommerce.android.ui.orders.tracking.OrderShipmentProvidersRepository.OrderShipmentProvidersFetchResult.NoCarriersFound
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.ORDERS
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderShipmentProvidersPayload
import javax.inject.Inject

class OrderShipmentProvidersRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val orderStore: WCOrderStore
) {

    fun observeOrderShipmentProviders() =
        orderStore.observeOrderShipmentProviders(selectedSite.get()).map { entity ->
            entity.map(WCOrderShipmentProviderModel::toAppModel)
        }

    suspend fun fetchOrderShipmentProviders(orderId: Long): Result<OrderShipmentProvidersFetchResult> {
        // Fetch from API
        val order = orderStore.getOrderByIdAndSite(orderId, selectedSite.get())
        if (order == null) {
            val exception = OrderNotFoundException(orderId)
            WooLog.e(ORDERS, exception)
            return Result.failure(exception)
        }
        val payload = FetchOrderShipmentProvidersPayload(selectedSite.get(), order)
        val result = orderStore.fetchOrderShipmentProviders(payload)
        return when {
            result.isError -> {
                val exception = OrderShipmentProvidersFetchException(result.error.message)
                WooLog.e(ORDERS, exception)
                Result.failure(exception)
            }

            result.shipmentProvidersFetchedCount == 0 -> {
                WooLog.i(ORDERS, "No shipment providers fetched")
                Result.success(NoCarriersFound)
            }

            else -> Result.success(CarriersFetched)
        }
    }

    class OrderNotFoundException(orderId: Long) :
        Exception("Can't find order with id $orderId while trying to fetch shipment providers list")

    class OrderShipmentProvidersFetchException(
        errorMessage: String
    ) : Exception("Error fetching order shipment providers: $errorMessage")

    sealed class OrderShipmentProvidersFetchResult {
        data object CarriersFetched : OrderShipmentProvidersFetchResult()
        data object NoCarriersFound : OrderShipmentProvidersFetchResult()
    }
}
