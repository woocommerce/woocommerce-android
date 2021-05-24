package com.woocommerce.android.ui.orders.fulfill

import com.woocommerce.android.AppConstants
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderShipmentTracking
import com.woocommerce.android.model.getNonRefundedProducts
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.ContinuationWrapper
import com.woocommerce.android.util.ContinuationWrapper.ContinuationResult.Cancellation
import com.woocommerce.android.util.ContinuationWrapper.ContinuationResult.Success
import com.woocommerce.android.util.WooLog.T.ORDERS
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.model.order.toIdSet
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.AddOrderShipmentTrackingPayload
import org.wordpress.android.fluxc.store.WCOrderStore.DeleteOrderShipmentTrackingPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCRefundStore
import javax.inject.Inject

@OpenClassOnDebug
class OrderFulfillRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val orderStore: WCOrderStore,
    private val refundStore: WCRefundStore,
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite
) {
    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    private var continuationAddShipmentTracking = ContinuationWrapper<Boolean>(ORDERS)
    private var continuationDeleteShipmentTracking = ContinuationWrapper<Boolean>(ORDERS)

    fun getOrder(orderIdentifier: OrderIdentifier) = orderStore.getOrderByIdentifier(orderIdentifier)?.toAppModel()

    fun getNonRefundedProducts(
        remoteOrderId: Long,
        items: List<Order.Item>
    ) = getOrderRefunds(remoteOrderId).getNonRefundedProducts(items)

    fun hasVirtualProductsOnly(remoteProductIds: List<Long>): Boolean {
        return if (remoteProductIds.isNotEmpty()) {
            productStore.getVirtualProductCountByRemoteIds(
                selectedSite.get(), remoteProductIds
            ) == remoteProductIds.size
        } else false
    }

    fun getOrderShipmentTrackings(localOrderId: Int) =
        orderStore.getShipmentTrackingsForOrder(selectedSite.get(), localOrderId).map { it.toAppModel() }

    private fun getOrderRefunds(remoteOrderId: Long) = refundStore
        .getAllRefunds(selectedSite.get(), remoteOrderId)
        .map { it.toAppModel() }
        .reversed()
        .sortedBy { it.id }

    suspend fun addOrderShipmentTracking(
        orderIdentifier: OrderIdentifier,
        shipmentTrackingModel: OrderShipmentTracking
    ): Boolean {
        val orderIdSet = orderIdentifier.toIdSet()
        val result = continuationAddShipmentTracking.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {
            val payload = AddOrderShipmentTrackingPayload(
                selectedSite.get(),
                orderIdSet.id,
                orderIdSet.remoteOrderId,
                shipmentTrackingModel.toDataModel(),
                shipmentTrackingModel.isCustomProvider
            )
            dispatcher.dispatch(WCOrderActionBuilder.newAddOrderShipmentTrackingAction(payload))
        }
        return when (result) {
            is Cancellation -> false
            is Success -> result.value
        }
    }

    suspend fun deleteOrderShipmentTracking(
        localOrderId: Int,
        remoteOrderId: Long,
        shipmentTrackingModel: WCOrderShipmentTrackingModel
    ): Boolean {
        val result = continuationDeleteShipmentTracking.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {
            val payload = DeleteOrderShipmentTrackingPayload(
                selectedSite.get(), localOrderId, remoteOrderId, shipmentTrackingModel
            )
            dispatcher.dispatch(WCOrderActionBuilder.newDeleteOrderShipmentTrackingAction(payload))
        }
        return when (result) {
            is Cancellation -> false
            is Success -> result.value
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        when (event.causeOfChange) {
            WCOrderAction.ADD_ORDER_SHIPMENT_TRACKING -> {
                if (event.isError) {
                    AnalyticsTracker.track(
                        Stat.ORDER_TRACKING_ADD_FAILED, mapOf(
                        AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                        AnalyticsTracker.KEY_ERROR_TYPE to event.error.type.toString(),
                        AnalyticsTracker.KEY_ERROR_DESC to event.error.message)
                    )
                    continuationAddShipmentTracking.continueWith(false)
                } else {
                    AnalyticsTracker.track(Stat.ORDER_TRACKING_ADD_SUCCESS)
                    continuationAddShipmentTracking.continueWith(true)
                }
            }
            WCOrderAction.DELETE_ORDER_SHIPMENT_TRACKING -> {
                if (event.isError) {
                    AnalyticsTracker.track(
                        Stat.ORDER_TRACKING_DELETE_FAILED, mapOf(
                        AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                        AnalyticsTracker.KEY_ERROR_TYPE to event.error.type.toString(),
                        AnalyticsTracker.KEY_ERROR_DESC to event.error.message
                    ))
                    continuationDeleteShipmentTracking.continueWith(false)
                } else {
                    AnalyticsTracker.track(Stat.ORDER_TRACKING_DELETE_SUCCESS)
                    continuationDeleteShipmentTracking.continueWith(true)
                }
            }
            else -> {
            }
        }
    }
}
