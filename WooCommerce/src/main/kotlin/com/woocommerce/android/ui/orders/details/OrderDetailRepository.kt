package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.model.ShippingLabel
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
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.WCOrderNoteModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.model.order.toIdSet
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.AddOrderShipmentTrackingPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderNotesPayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderShipmentTrackingsPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType
import org.wordpress.android.fluxc.store.WCOrderStore.PostOrderNotePayload
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderStatusPayload
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCRefundStore
import org.wordpress.android.fluxc.store.WCShippingLabelStore
import javax.inject.Inject
import kotlin.coroutines.resume

@OpenClassOnDebug
class OrderDetailRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val orderStore: WCOrderStore,
    private val productStore: WCProductStore,
    private val refundStore: WCRefundStore,
    private val shippingLabelStore: WCShippingLabelStore,
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val ACTION_TIMEOUT = 10L * 1000
    }

    private var continuationFetchOrder: CancellableContinuation<Boolean>? = null
    private var continuationFetchOrderNotes: CancellableContinuation<Boolean>? = null
    private var continuationFetchOrderShipmentTrackingList: CancellableContinuation<RequestResult>? = null
    private var continuationUpdateOrderStatus: CancellableContinuation<Boolean>? = null
    private var continuationAddOrderNote: CancellableContinuation<Boolean>? = null
    private var continuationAddShipmentTracking: CancellableContinuation<Boolean>? = null

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
            suspendCancellableCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
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
            suspendCancellableCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
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
            suspendCancellableCoroutineWithTimeout<RequestResult>(ACTION_TIMEOUT) {
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
        return withContext(Dispatchers.IO) {
            shippingLabelStore.fetchShippingLabelsForOrder(selectedSite.get(), remoteOrderId)
        }.model?.map { it.toAppModel() } ?: emptyList()
    }

    suspend fun updateOrderStatus(
        localOrderId: Int,
        remoteOrderId: Long,
        newStatus: String
    ): Boolean {
        return try {
            continuationUpdateOrderStatus?.cancel()
            suspendCancellableCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
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
        localOrderId: Int,
        remoteOrderId: Long,
        noteModel: WCOrderNoteModel
    ): Boolean {
        return try {
            continuationAddOrderNote?.cancel()
            suspendCancellableCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                continuationAddOrderNote = it

                val payload = PostOrderNotePayload(localOrderId, remoteOrderId, selectedSite.get(), noteModel)
                dispatcher.dispatch(WCOrderActionBuilder.newPostOrderNoteAction(payload))
            } ?: false
        } catch (e: CancellationException) {
            WooLog.e(ORDERS, "CancellationException while adding order note $remoteOrderId")
            false
        }
    }

    suspend fun addOrderShipmentTracking(
        localOrderId: Int,
        remoteOrderId: Long,
        shipmentTrackingModel: WCOrderShipmentTrackingModel,
        isCustomProvider: Boolean
    ): Boolean {
        return try {
            continuationAddShipmentTracking?.cancel()
            suspendCancellableCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                continuationAddShipmentTracking = it

                val payload = AddOrderShipmentTrackingPayload(
                    selectedSite.get(),
                    localOrderId,
                    remoteOrderId,
                    shipmentTrackingModel,
                    isCustomProvider
                )
                dispatcher.dispatch(WCOrderActionBuilder.newAddOrderShipmentTrackingAction(payload))
            } ?: false
        } catch (e: CancellationException) {
            WooLog.e(ORDERS, "CancellationException while adding shipment tracking $remoteOrderId")
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

    fun getProductsByRemoteIds(remoteIds: List<Long>) =
        productStore.getProductsByRemoteIds(selectedSite.get(), remoteIds)

    fun getOrderRefunds(remoteOrderId: Long) = refundStore
        .getAllRefunds(selectedSite.get(), remoteOrderId)
        .map { it.toAppModel() }
        .reversed()
        .sortedBy { it.id }

    fun getOrderShipmentTrackings(localOrderId: Int) =
        orderStore.getShipmentTrackingsForOrder(selectedSite.get(), localOrderId).map { it.toAppModel() }

    fun getOrderShippingLabels(remoteOrderId: Long) = shippingLabelStore
        .getShippingLabelsForOrder(selectedSite.get(), remoteOrderId).map { it.toAppModel() }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onOrderChanged(event: OnOrderChanged) {
        when (event.causeOfChange) {
            WCOrderAction.FETCH_SINGLE_ORDER -> {
                if (event.isError) {
                    continuationFetchOrder?.resume(false)
                } else {
                    continuationFetchOrder?.resume(true)
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
            else -> { }
        }
    }
}
