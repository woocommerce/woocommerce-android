package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.AppConstants
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_FEEDBACK_ACTION
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_API_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_API_SUCCESS
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.model.*
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.ORDERS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.model.order.toIdSet
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.LabelItem
import org.wordpress.android.fluxc.store.*
import org.wordpress.android.fluxc.store.WCOrderStore.*
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType.GENERIC_ERROR
import javax.inject.Inject

class OrderDetailRepository @Inject constructor(
    private val orderStore: WCOrderStore,
    private val productStore: WCProductStore,
    private val refundStore: WCRefundStore,
    private val shippingLabelStore: WCShippingLabelStore,
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore
) {
    suspend fun fetchOrder(orderIdentifier: OrderIdentifier): Order? {
        val result = withTimeoutOrNull(AppConstants.REQUEST_TIMEOUT) {
            orderStore.fetchSingleOrder(
                selectedSite.get(),
                orderIdentifier.toIdSet().remoteOrderId
            )
        }

        return if (result?.isError == false) {
            getOrder(orderIdentifier)
        } else {
            null
        }
    }

    suspend fun fetchOrderNotes(
        localOrderId: Int,
        remoteOrderId: Long
    ): Boolean {
        val result = withTimeoutOrNull(AppConstants.REQUEST_TIMEOUT) {
            orderStore.fetchOrderNotes(localOrderId, remoteOrderId, selectedSite.get())
        }
        return result?.isError == false
    }

    suspend fun fetchOrderShipmentTrackingList(
        localOrderId: Int,
        remoteOrderId: Long
    ): RequestResult {
        val result = withTimeoutOrNull(AppConstants.REQUEST_TIMEOUT) {
            orderStore.fetchOrderShipmentTrackings(localOrderId, remoteOrderId, selectedSite.get())
        }

        return if (result?.isError == false) {
            RequestResult.SUCCESS
        } else {
            if (result?.error?.type == OrderErrorType.PLUGIN_NOT_ACTIVE) {
                RequestResult.API_ERROR
            } else RequestResult.ERROR
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

        return result.model?.filter { it.status == LabelItem.STATUS_PURCHASED }?.map { it.toAppModel() } ?: emptyList()
    }

    suspend fun updateOrderStatus(
        orderLocalId: LocalId,
        newStatus: String
    ): Flow<UpdateOrderResult> {
        return orderStore.updateOrderStatus(
            orderLocalId,
            selectedSite.get(),
            newStatus
        )
    }

    suspend fun addOrderNote(
        orderIdentifier: OrderIdentifier,
        remoteOrderId: Long,
        noteModel: OrderNote
    ): OnOrderChanged {
        val order = orderStore.getOrderByIdentifier(orderIdentifier)
        if (order == null) {
            WooLog.e(ORDERS, "Can't find order with identifier $orderIdentifier")
            return OnOrderChanged(0).also {
                it.error = OrderError(GENERIC_ERROR, "Can't find order with identifier $orderIdentifier")
            }
        }
        val dataModel = noteModel.toDataModel()
        val payload = PostOrderNotePayload(order.id, remoteOrderId, selectedSite.get(), dataModel)
        return orderStore.postOrderNote(payload)
    }

    suspend fun addOrderShipmentTracking(
        orderIdentifier: OrderIdentifier,
        shipmentTrackingModel: OrderShipmentTracking
    ): OnOrderChanged {
        val orderIdSet = orderIdentifier.toIdSet()
        return orderStore.addOrderShipmentTracking(
            AddOrderShipmentTrackingPayload(
                selectedSite.get(),
                orderIdSet.id,
                orderIdSet.remoteOrderId,
                shipmentTrackingModel.toDataModel(),
                shipmentTrackingModel.isCustomProvider
            )
        )
    }

    suspend fun deleteOrderShipmentTracking(
        localOrderId: Int,
        remoteOrderId: Long,
        shipmentTrackingModel: WCOrderShipmentTrackingModel
    ): OnOrderChanged {
        return orderStore.deleteOrderShipmentTracking(
            DeleteOrderShipmentTrackingPayload(
                selectedSite.get(), localOrderId, remoteOrderId, shipmentTrackingModel
            )
        )
    }

    fun getOrder(orderIdentifier: OrderIdentifier) = orderStore.getOrderByIdentifier(orderIdentifier)?.toAppModel()

    fun getOrderStatus(key: String): OrderStatus {
        return (
            orderStore.getOrderStatusForSiteAndKey(selectedSite.get(), key) ?: WCOrderStatusModel().apply {
                statusKey = key
                label = key
            }
            ).toOrderStatus()
    }

    fun getOrderStatusOptions() = orderStore.getOrderStatusOptionsForSite(selectedSite.get()).map { it.toOrderStatus() }

    fun getOrderNotes(localOrderId: Int) =
        orderStore.getOrderNotesForOrder(localOrderId).map { it.toAppModel() }

    suspend fun fetchProductsByRemoteIds(remoteIds: List<Long>) =
        productStore.fetchProductListSynced(selectedSite.get(), remoteIds)?.map { it.toAppModel() } ?: emptyList()

    fun hasVirtualProductsOnly(remoteProductIds: List<Long>): Boolean {
        return if (remoteProductIds.isNotEmpty()) {
            productStore.getVirtualProductCountByRemoteIds(
                selectedSite.get(), remoteProductIds
            ) == remoteProductIds.size
        } else false
    }

    fun getProductCountForOrder(remoteProductIds: List<Long>): Int {
        return if (remoteProductIds.isNotEmpty()) {
            productStore.getProductCountByRemoteIds(selectedSite.get(), remoteProductIds)
        } else 0
    }

    fun hasSubscriptionProducts(remoteProductIds: List<Long>): Boolean {
        return if (remoteProductIds.isNotEmpty()) {
            productStore.getProductsByRemoteIds(selectedSite.get(), remoteProductIds)
                .any { it.type == PRODUCT_SUBSCRIPTION_TYPE }
        } else false
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
        .getShippingLabelsForOrder(selectedSite.get(), remoteOrderId)
        .filter { it.status == LabelItem.STATUS_PURCHASED }
        .map { it.toAppModel() }

    fun getWooServicesPluginInfo(): WooPlugin {
        val info = wooCommerceStore.getSitePlugin(selectedSite.get(), WooCommerceStore.WooPlugin.WOO_SERVICES)
        return WooPlugin(info != null, info?.active ?: false, info?.version)
    }

    fun getStoreCountryCode(): String? {
        return wooCommerceStore.getStoreCountryCode(selectedSite.get())
    }

    suspend fun fetchSLCreationEligibility(orderId: Long) {
        val result = shippingLabelStore.fetchShippingLabelCreationEligibility(
            site = selectedSite.get(),
            orderId = orderId,
            canCreatePackage = true,
            canCreatePaymentMethod = true,
            canCreateCustomsForm = true
        )
        if (result.isError) {
            WooLog.e(
                tag = ORDERS,
                message = "Fetching shipping labels creation eligibility failed for $orderId, " +
                    "error: ${result.error.type} ${result.error.message}"
            )
        } else if (!result.model!!.isEligible) {
            WooLog.d(
                tag = ORDERS,
                message = "Order $orderId is not eligible for shipping labels creation, " +
                    "reason: ${result.model!!.reason}"
            )
        }
    }

    fun isOrderEligibleForSLCreation(orderId: Long): Boolean {
        return shippingLabelStore.isOrderEligibleForShippingLabelCreation(
            site = selectedSite.get(),
            orderId = orderId,
            canCreatePackage = true,
            canCreatePaymentMethod = true,
            canCreateCustomsForm = true
        )?.isEligible ?: false
    }

    companion object {
        const val PRODUCT_SUBSCRIPTION_TYPE = "subscription"
    }
}
