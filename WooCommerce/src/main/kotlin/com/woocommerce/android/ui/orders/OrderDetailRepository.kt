package com.woocommerce.android.ui.orders

import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.LOGIN
import com.woocommerce.android.util.WooLog.T.ORDERS
import com.woocommerce.android.util.suspendCoroutineWithTimeout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.refunds.WCRefundModel
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderShipmentTrackingsPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCRefundStore
import org.wordpress.android.fluxc.store.WCShippingLabelStore
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class OrderDetailRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val orderStore: WCOrderStore,
    private val refundStore: WCRefundStore,
    private val shippingLabelStore: WCShippingLabelStore,
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val ACTION_TIMEOUT = 10L * 1000
    }

    private var continuationFetchShipmentTrackingList: Continuation<Boolean>? = null

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    fun getOrderDetailInfoFromDb(
        order: WCOrderModel
    ): OrderDetailUiItem {
        val refunds = refundStore
            .getAllRefunds(selectedSite.get(), order.remoteOrderId)
            .map { it.toAppModel() }
            .reversed()

        val shippingLabels = if (FeatureFlag.SHIPPING_LABELS_M1.isEnabled()) {
            shippingLabelStore
                .getShippingLabelsForOrder(selectedSite.get(), order.remoteOrderId)
                .map { it.toAppModel() }
        } else emptyList()

        val shipmentTrackingList = orderStore.getShipmentTrackingsForOrder(selectedSite.get(), order.id)

        return OrderDetailUiItem(
            orderModel = order,
            refunds = refunds,
            shippingLabels = shippingLabels,
            shipmentTrackingList = shipmentTrackingList,
            isShipmentTrackingAvailable = shipmentTrackingList.isNotEmpty()
        )
    }

    suspend fun fetchOrderDetailInfo(
        order: WCOrderModel
    ): OrderDetailUiItem {
        return coroutineScope {
            var fetchedRefunds: WooResult<List<WCRefundModel>>? = null
            var fetchedShipmentTrackingList = false

            val fetchRefunds = async {
                fetchedRefunds =
                    refundStore.fetchAllRefunds(selectedSite.get(), order.remoteOrderId)
            }
            val fetchShipmentTrackingList = async {
                fetchedShipmentTrackingList = fetchShipmentTrackingList(order)
            }
            fetchRefunds.await()
            fetchShipmentTrackingList.await()

            val shippingLabels = if (FeatureFlag.SHIPPING_LABELS_M1.isEnabled()) {
                var fetchedShippingLabels: WooResult<List<WCShippingLabelModel>>? = null
                val fetchShippingLabels = async {
                    fetchedShippingLabels =
                        shippingLabelStore.fetchShippingLabelsForOrder(selectedSite.get(), order.remoteOrderId)
                }
                fetchShippingLabels.await()
                fetchedShippingLabels?.model?.map { it.toAppModel() } ?: emptyList()
            } else emptyList()

            val refunds = fetchedRefunds?.model?.map { it.toAppModel() } ?: emptyList()
            val shipmentTrackingList = if (fetchedShipmentTrackingList) {
                orderStore.getShipmentTrackingsForOrder(selectedSite.get(), order.id)
            } else emptyList()

            OrderDetailUiItem(
                orderModel = order,
                refunds = refunds,
                shippingLabels = shippingLabels,
                shipmentTrackingList = shipmentTrackingList,
                isShipmentTrackingAvailable = fetchedShipmentTrackingList
            )
        }
    }

    /**
     * Fires the request to fetch shipment tracking list for an order
     *
     * @return the result of the action as a [Boolean]
     */
    private suspend fun fetchShipmentTrackingList(
        order: WCOrderModel
    ): Boolean {
        return try {
            suspendCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                continuationFetchShipmentTrackingList = it

                val payload = FetchOrderShipmentTrackingsPayload(order.id, order.remoteOrderId, selectedSite.get())
                dispatcher.dispatch(WCOrderActionBuilder.newFetchOrderShipmentTrackingsAction(payload))
            } ?: false // request timed out
        } catch (e: CancellationException) {
            WooLog.e(LOGIN, "Exception encountered while fetching shipment tracking info for order: " +
                "${order.remoteOrderId}", e)
            false
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        if (event.causeOfChange == WCOrderAction.FETCH_ORDER_SHIPMENT_TRACKINGS) {
            if (event.isError) {
                continuationFetchShipmentTrackingList?.resume(false)
                WooLog.e(ORDERS, "Error fetching order shipment tracking info: ${event.error.message}")
            } else {
                continuationFetchShipmentTrackingList?.resume(true)
            }
            continuationFetchShipmentTrackingList = null
        }
    }

    data class OrderDetailUiItem(
        val orderModel: WCOrderModel,
        val refunds: List<Refund>,
        val shippingLabels: List<ShippingLabel>,
        val shipmentTrackingList: List<WCOrderShipmentTrackingModel>,
        val isShipmentTrackingAvailable: Boolean
    )
}
