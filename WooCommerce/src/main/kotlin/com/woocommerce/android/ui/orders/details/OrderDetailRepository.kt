package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.AppConstants
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_FEEDBACK_ACTION
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_API_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_API_SUCCESS
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.model.OrderShipmentTracking
import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.model.WooPlugin
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.model.toOrderStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.ORDERS
import com.woocommerce.android.util.suspendCancellableCoroutineWithTimeout
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_SINGLE_PRODUCT
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.model.order.toIdSet
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.AddOrderShipmentTrackingPayload
import org.wordpress.android.fluxc.store.WCOrderStore.DeleteOrderShipmentTrackingPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderNotesPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderShipmentTrackingsPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType
import org.wordpress.android.fluxc.store.WCOrderStore.PostOrderNotePayload
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderStatusPayload
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.OnProductChanged
import org.wordpress.android.fluxc.store.WCRefundStore
import org.wordpress.android.fluxc.store.WCShippingLabelStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject
import kotlin.coroutines.resume

@OpenClassOnDebug
class OrderDetailRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val orderStore: WCOrderStore,
    private val productStore: WCProductStore,
    private val refundStore: WCRefundStore,
    private val shippingLabelStore: WCShippingLabelStore,
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore
) {
    private var continuationFetchOrder: CancellableContinuation<Boolean>? = null
    private var continuationFetchOrderNotes: CancellableContinuation<Boolean>? = null
    private var continuationFetchOrderShipmentTrackingList: CancellableContinuation<RequestResult>? = null
    private var continuationUpdateOrderStatus: CancellableContinuation<Boolean>? = null
    private var continuationAddOrderNote: CancellableContinuation<Boolean>? = null
    private var continuationAddShipmentTracking: CancellableContinuation<Boolean>? = null
    private var continuationDeleteShipmentTracking: CancellableContinuation<Boolean>? = null

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    suspend fun fetchOrder(orderIdentifier: OrderIdentifier): Order? {
        val remoteOrderId = orderIdentifier.toIdSet().remoteOrderId
        try {
            continuationFetchOrder?.cancel()
            suspendCancellableCoroutineWithTimeout<Boolean>(AppConstants.REQUEST_TIMEOUT) {
                continuationFetchOrder = it

                val payload = WCOrderStore.FetchSingleOrderPayload(selectedSite.get(), remoteOrderId)
                dispatcher.dispatch(WCOrderActionBuilder.newFetchSingleOrderAction(payload))
            }
        } catch (e: CancellationException) {
            WooLog.e(ORDERS, "CancellationException while fetching single order $remoteOrderId")
        }

        continuationFetchOrder = null
        return getOrder(orderIdentifier)
    }

    suspend fun fetchOrderNotes(
        localOrderId: Int,
        remoteOrderId: Long
    ): Boolean {
        return try {
            continuationFetchOrderNotes?.cancel()
            suspendCancellableCoroutineWithTimeout<Boolean>(AppConstants.REQUEST_TIMEOUT) {
                continuationFetchOrderNotes = it

                val payload = FetchOrderNotesPayload(localOrderId, remoteOrderId, selectedSite.get())
                dispatcher.dispatch(WCOrderActionBuilder.newFetchOrderNotesAction(payload))
            } ?: false
        } catch (e: CancellationException) {
            WooLog.e(ORDERS, "CancellationException while fetching order notes $remoteOrderId")
            false
        }
    }

    suspend fun fetchOrderShipmentTrackingList(
        localOrderId: Int,
        remoteOrderId: Long
    ): RequestResult {
        return try {
            continuationFetchOrderShipmentTrackingList?.cancel()
            suspendCancellableCoroutineWithTimeout<RequestResult>(AppConstants.REQUEST_TIMEOUT) {
                continuationFetchOrderShipmentTrackingList = it

                val payload = FetchOrderShipmentTrackingsPayload(localOrderId, remoteOrderId, selectedSite.get())
                dispatcher.dispatch(WCOrderActionBuilder.newFetchOrderShipmentTrackingsAction(payload))
            } ?: RequestResult.ERROR
        } catch (e: CancellationException) {
            WooLog.e(ORDERS, "CancellationException while fetching shipment trackings $remoteOrderId")
            RequestResult.ERROR
        }
    }

    suspend fun fetchOrderRefunds(remoteOrderId: Long): List<Refund> {
        return withContext(Dispatchers.IO) {
            refundStore.fetchAllRefunds(selectedSite.get(), remoteOrderId)
        }.model?.map { it.toAppModel() } ?: emptyList()
    }

    suspend fun fetchOrderShippingLabels(remoteOrderId: Long): List<ShippingLabel> {
        val result = withContext(Dispatchers.IO) {
            shippingLabelStore.fetchShippingLabelsForOrder(selectedSite.get(), remoteOrderId)
        }

        val action = if (result.isError) {
            VALUE_API_FAILED
        } else VALUE_API_SUCCESS
        AnalyticsTracker.track(Stat.SHIPPING_LABEL_API_REQUEST, mapOf(KEY_FEEDBACK_ACTION to action))

        return result.model?.map { it.toAppModel() } ?: emptyList()
    }

    suspend fun updateOrderStatus(
        localOrderId: Int,
        remoteOrderId: Long,
        newStatus: String
    ): Boolean {
        return try {
            continuationUpdateOrderStatus?.cancel()
            suspendCancellableCoroutineWithTimeout<Boolean>(AppConstants.REQUEST_TIMEOUT) {
                continuationUpdateOrderStatus = it

                val payload = UpdateOrderStatusPayload(
                    localOrderId, remoteOrderId, selectedSite.get(), newStatus
                )
                dispatcher.dispatch(WCOrderActionBuilder.newUpdateOrderStatusAction(payload))
            } ?: false
        } catch (e: CancellationException) {
            WooLog.e(ORDERS, "CancellationException while updating order status $remoteOrderId")
            false
        }
    }

    suspend fun addOrderNote(
        orderIdentifier: OrderIdentifier,
        remoteOrderId: Long,
        noteModel: OrderNote
    ): Boolean {
        return try {
            continuationAddOrderNote?.cancel()
            val order = orderStore.getOrderByIdentifier(orderIdentifier)
            if (order == null) {
                WooLog.e(ORDERS, "Can't find order with identifier $orderIdentifier")
                return false
            }
            suspendCancellableCoroutineWithTimeout<Boolean>(AppConstants.REQUEST_TIMEOUT) {
                continuationAddOrderNote = it

                val dataModel = noteModel.toDataModel()
                val payload = PostOrderNotePayload(order.id, remoteOrderId, selectedSite.get(), dataModel)
                dispatcher.dispatch(WCOrderActionBuilder.newPostOrderNoteAction(payload))
            } ?: false
        } catch (e: CancellationException) {
            WooLog.e(ORDERS, "CancellationException while adding order note $remoteOrderId")
            false
        }
    }

    suspend fun addOrderShipmentTracking(
        orderIdentifier: OrderIdentifier,
        shipmentTrackingModel: OrderShipmentTracking
    ): Boolean {
        val orderIdSet = orderIdentifier.toIdSet()
        return try {
            continuationAddShipmentTracking?.cancel()
            suspendCancellableCoroutineWithTimeout<Boolean>(AppConstants.REQUEST_TIMEOUT) {
                continuationAddShipmentTracking = it

                val payload = AddOrderShipmentTrackingPayload(
                    selectedSite.get(),
                    orderIdSet.id,
                    orderIdSet.remoteOrderId,
                    shipmentTrackingModel.toDataModel(),
                    shipmentTrackingModel.isCustomProvider
                )
                dispatcher.dispatch(WCOrderActionBuilder.newAddOrderShipmentTrackingAction(payload))
            } ?: false
        } catch (e: CancellationException) {
            WooLog.e(ORDERS, "CancellationException while adding shipment tracking ${orderIdSet.remoteOrderId}")
            false
        }
    }

    suspend fun deleteOrderShipmentTracking(
        localOrderId: Int,
        remoteOrderId: Long,
        shipmentTrackingModel: WCOrderShipmentTrackingModel
    ): Boolean {
        return try {
            continuationDeleteShipmentTracking?.cancel()
            suspendCancellableCoroutineWithTimeout<Boolean>(AppConstants.REQUEST_TIMEOUT) {
                continuationDeleteShipmentTracking = it

                val payload = DeleteOrderShipmentTrackingPayload(
                    selectedSite.get(), localOrderId, remoteOrderId, shipmentTrackingModel
                )
                dispatcher.dispatch(WCOrderActionBuilder.newDeleteOrderShipmentTrackingAction(payload))
            } ?: false
        } catch (e: CancellationException) {
            WooLog.e(ORDERS, "CancellationException while deleting shipment tracking $remoteOrderId")
            false
        }
    }

    fun getOrder(orderIdentifier: OrderIdentifier) = orderStore.getOrderByIdentifier(orderIdentifier)?.toAppModel()

    fun getOrderStatus(key: String): OrderStatus {
        return (orderStore.getOrderStatusForSiteAndKey(selectedSite.get(), key) ?: WCOrderStatusModel().apply {
            statusKey = key
            label = key
        }).toOrderStatus()
    }

    fun getOrderStatusOptions() = orderStore.getOrderStatusOptionsForSite(selectedSite.get()).map { it.toOrderStatus() }

    fun getOrderNotes(localOrderId: Int) =
        orderStore.getOrderNotesForOrder(localOrderId).map { it.toAppModel() }

    suspend fun fetchProductsByRemoteIds(remoteIds: List<Long>) =
        productStore.fetchProductListSynced(selectedSite.get(), remoteIds)?.map { it.toAppModel() } ?: emptyList()

    fun hasVirtualProductsOnly(remoteProductIds: List<Long>): Boolean {
        return if (remoteProductIds.isNotEmpty()) {
            productStore.hasVirtualProductsOnly(selectedSite.get(), remoteProductIds) == remoteProductIds.size
        } else false
    }

    fun getProductCountForOrder(remoteProductIds: List<Long>): Int {
        return if (remoteProductIds.isNotEmpty()) {
            productStore.getProductCountByRemoteIds(selectedSite.get(), remoteProductIds)
        } else 0
    }

    fun getOrderRefunds(remoteOrderId: Long) = refundStore
        .getAllRefunds(selectedSite.get(), remoteOrderId)
        .map { it.toAppModel() }
        .reversed()
        .sortedBy { it.id }

    fun getOrderShipmentTrackingByTrackingNumber(
        localOrderId: Int,
        trackingNumber: String
    ): OrderShipmentTracking? = orderStore.getShipmentTrackingByTrackingNumber(
        selectedSite.get(), localOrderId, trackingNumber
    )?.toAppModel()

    fun getOrderShipmentTrackings(localOrderId: Int) =
        orderStore.getShipmentTrackingsForOrder(selectedSite.get(), localOrderId).map { it.toAppModel() }

    fun getOrderShippingLabels(remoteOrderId: Long) = shippingLabelStore
        .getShippingLabelsForOrder(selectedSite.get(), remoteOrderId).map { it.toAppModel() }

    fun getWooServicesPluginInfo(): WooPlugin {
        val info = wooCommerceStore.getWooCommerceServicesPluginInfo(selectedSite.get())
        return WooPlugin(info != null, info?.active ?: false)
    }

    fun getStoreCountryCode(): String? {
        return wooCommerceStore.getStoreCountryCode(selectedSite.get())
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        when (event.causeOfChange) {
            WCOrderAction.FETCH_SINGLE_ORDER -> {
                if (continuationFetchOrder?.isActive == true) {
                    if (event.isError) {
                        continuationFetchOrder?.resume(false)
                    } else {
                        continuationFetchOrder?.resume(true)
                    }
                }
            }
            WCOrderAction.FETCH_ORDER_NOTES -> {
                if (event.isError) {
                    continuationFetchOrderNotes?.resume(false)
                } else {
                    continuationFetchOrderNotes?.resume(true)
                }
                continuationFetchOrderNotes = null
            }
            WCOrderAction.FETCH_ORDER_SHIPMENT_TRACKINGS -> {
                if (event.isError) {
                    val error = if (event.error.type == OrderErrorType.PLUGIN_NOT_ACTIVE) {
                        RequestResult.API_ERROR
                    } else RequestResult.ERROR
                    continuationFetchOrderShipmentTrackingList?.resume(error)
                } else {
                    continuationFetchOrderShipmentTrackingList?.resume(RequestResult.SUCCESS)
                }
                continuationFetchOrderShipmentTrackingList = null
            }
            WCOrderAction.UPDATE_ORDER_STATUS -> {
                if (event.isError) {
                    AnalyticsTracker.track(
                        Stat.ORDER_STATUS_CHANGE_FAILED, mapOf(
                        AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                        AnalyticsTracker.KEY_ERROR_TYPE to event.error.type.toString(),
                        AnalyticsTracker.KEY_ERROR_DESC to event.error.message)
                    )
                    continuationUpdateOrderStatus?.resume(false)
                } else {
                    AnalyticsTracker.track(Stat.ORDER_STATUS_CHANGE_SUCCESS)
                    continuationUpdateOrderStatus?.resume(true)
                }
                continuationUpdateOrderStatus = null
            }
            WCOrderAction.POST_ORDER_NOTE -> {
                if (event.isError) {
                    AnalyticsTracker.track(
                        Stat.ORDER_NOTE_ADD_FAILED, mapOf(
                        AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                        AnalyticsTracker.KEY_ERROR_TYPE to event.error.type.toString(),
                        AnalyticsTracker.KEY_ERROR_DESC to event.error.message)
                    )
                    continuationAddOrderNote?.resume(false)
                } else {
                    AnalyticsTracker.track(Stat.ORDER_NOTE_ADD_SUCCESS)
                    continuationAddOrderNote?.resume(true)
                }
                continuationAddOrderNote = null
            }
            WCOrderAction.ADD_ORDER_SHIPMENT_TRACKING -> {
                if (event.isError) {
                    AnalyticsTracker.track(
                        Stat.ORDER_TRACKING_ADD_FAILED, mapOf(
                        AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                        AnalyticsTracker.KEY_ERROR_TYPE to event.error.type.toString(),
                        AnalyticsTracker.KEY_ERROR_DESC to event.error.message)
                    )
                    continuationAddShipmentTracking?.resume(false)
                } else {
                    AnalyticsTracker.track(Stat.ORDER_TRACKING_ADD_SUCCESS)
                    continuationAddShipmentTracking?.resume(true)
                }
                continuationAddShipmentTracking = null
            }
            WCOrderAction.DELETE_ORDER_SHIPMENT_TRACKING -> {
                if (event.isError) {
                    AnalyticsTracker.track(
                        Stat.ORDER_TRACKING_DELETE_FAILED, mapOf(
                        AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                        AnalyticsTracker.KEY_ERROR_TYPE to event.error.type.toString(),
                        AnalyticsTracker.KEY_ERROR_DESC to event.error.message
                    ))
                    continuationDeleteShipmentTracking?.resume(false)
                } else {
                    AnalyticsTracker.track(Stat.ORDER_TRACKING_DELETE_SUCCESS)
                    continuationDeleteShipmentTracking?.resume(true)
                }
                continuationDeleteShipmentTracking = null
            }
            else -> { }
        }
    }

    class OnProductImageChanged(val remoteProductId: Long)

    /**
     * This will be triggered if we fetched a product via ProduictImageMap so we could get its image.
     * Here we fire an event that tells the fragment to update that product in the order product list.
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onProductChanged(event: OnProductChanged) {
        if (event.causeOfChange == FETCH_SINGLE_PRODUCT && !event.isError) {
            EventBus.getDefault().post(OnProductImageChanged(event.remoteProductId))
        }
    }
}
