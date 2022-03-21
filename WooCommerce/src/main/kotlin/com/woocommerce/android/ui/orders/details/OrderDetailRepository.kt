package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.AppConstants
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_FEEDBACK_ACTION
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_API_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_API_SUCCESS
import com.woocommerce.android.model.*
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.ORDERS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.LabelItem
import org.wordpress.android.fluxc.store.*
import org.wordpress.android.fluxc.store.WCOrderStore.*
import javax.inject.Inject

class OrderDetailRepository @Inject constructor(
    private val orderStore: WCOrderStore,
    private val productStore: WCProductStore,
    private val refundStore: WCRefundStore,
    private val shippingLabelStore: WCShippingLabelStore,
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val dispatchers: CoroutineDispatchers,
    private val orderMapper: OrderMapper,
    private val shippingLabelMapper: ShippingLabelMapper,
) {
    suspend fun fetchOrderById(orderId: Long): Order? {
        val result = withTimeoutOrNull(AppConstants.REQUEST_TIMEOUT) {
            orderStore.fetchSingleOrder(
                selectedSite.get(),
                orderId
            )
        }

        return if (result?.isError == false) {
            getOrderById(orderId)
        } else {
            null
        }
    }

    suspend fun fetchOrderNotes(
        orderId: Long,
    ): Boolean = withContext(dispatchers.io) {
        val result = withTimeoutOrNull(AppConstants.REQUEST_TIMEOUT) {
            orderStore.fetchOrderNotes(selectedSite.get(), orderId)
        }
        result?.isError == false
    }

    suspend fun fetchOrderShipmentTrackingList(orderId: Long): RequestResult {
        val result = withTimeoutOrNull(AppConstants.REQUEST_TIMEOUT) {
            orderStore.fetchOrderShipmentTrackings(orderId, selectedSite.get())
        }

        return if (result?.isError == false) {
            RequestResult.SUCCESS
        } else {
            if (result?.error?.type == OrderErrorType.PLUGIN_NOT_ACTIVE) {
                RequestResult.API_ERROR
            } else RequestResult.ERROR
        }
    }

    suspend fun fetchOrderRefunds(orderId: Long): List<Refund> {
        return withContext(dispatchers.io) {
            refundStore.fetchAllRefunds(selectedSite.get(), orderId)
                .model?.map { it.toAppModel() } ?: emptyList()
        }
    }

    suspend fun fetchOrderShippingLabels(remoteOrderId: Long): List<ShippingLabel> {
        return withContext(dispatchers.io) {
            val result = shippingLabelStore.fetchShippingLabelsForOrder(selectedSite.get(), remoteOrderId)

            val action = if (result.isError) {
                VALUE_API_FAILED
            } else VALUE_API_SUCCESS
            AnalyticsTracker.track(AnalyticsEvent.SHIPPING_LABEL_API_REQUEST, mapOf(KEY_FEEDBACK_ACTION to action))
            result.model?.filter { it.status == LabelItem.STATUS_PURCHASED }
                ?.map { shippingLabelMapper.toAppModel(it) }
                ?: emptyList()
        }
    }

    suspend fun updateOrderStatus(
        orderId: Long,
        newStatus: String
    ): Flow<UpdateOrderResult> {
        val status = withContext(dispatchers.io) {
            orderStore.getOrderStatusForSiteAndKey(selectedSite.get(), newStatus)
                ?: WCOrderStatusModel(statusKey = newStatus)
        }
        return orderStore.updateOrderStatus(
            orderId,
            selectedSite.get(),
            status
        )
    }

    suspend fun addOrderNote(
        orderId: Long,
        noteModel: OrderNote
    ): Result<Unit> {
        return orderStore.postOrderNote(
            site = selectedSite.get(),
            orderId = orderId,
            note = noteModel.note,
            isCustomerNote = noteModel.isCustomerNote
        ).let {
            if (it.isError) Result.failure(WooException(it.error))
            else Result.success(Unit)
        }
    }

    suspend fun addOrderShipmentTracking(
        orderId: Long,
        shipmentTrackingModel: OrderShipmentTracking
    ): OnOrderChanged {
        return orderStore.addOrderShipmentTracking(
            AddOrderShipmentTrackingPayload(
                site = selectedSite.get(),
                orderId = orderId,
                tracking = shipmentTrackingModel.toDataModel(),
                isCustomProvider = shipmentTrackingModel.isCustomProvider
            )
        )
    }

    suspend fun deleteOrderShipmentTracking(
        orderId: Long,
        shipmentTrackingModel: WCOrderShipmentTrackingModel
    ): OnOrderChanged {
        return orderStore.deleteOrderShipmentTracking(
            DeleteOrderShipmentTrackingPayload(
                selectedSite.get(), orderId, shipmentTrackingModel
            )
        )
    }

    suspend fun getOrderById(orderId: Long) = withContext(dispatchers.io) {
        orderStore.getOrderByIdAndSite(orderId, selectedSite.get())?.let {
            orderMapper.toAppModel(it)
        }
    }

    fun getOrderStatus(key: String): OrderStatus {
        return (
            orderStore.getOrderStatusForSiteAndKey(selectedSite.get(), key) ?: WCOrderStatusModel().apply {
                statusKey = key
                label = key
            }
            ).toOrderStatus()
    }

    fun getOrderStatusOptions() = orderStore.getOrderStatusOptionsForSite(selectedSite.get()).map { it.toOrderStatus() }

    suspend fun getOrderNotes(orderId: Long) =
        orderStore.getOrderNotesForOrder(site = selectedSite.get(), orderId = orderId)
            .map { it.toAppModel() }

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

    fun getOrderRefunds(orderId: Long) = refundStore
        .getAllRefunds(selectedSite.get(), orderId)
        .map { it.toAppModel() }
        .reversed()
        .sortedBy { it.id }

    fun getOrderShipmentTrackingByTrackingNumber(
        orderId: Long,
        trackingNumber: String
    ): OrderShipmentTracking? = orderStore.getShipmentTrackingByTrackingNumber(
        selectedSite.get(), orderId, trackingNumber
    )?.toAppModel()

    fun getOrderShipmentTrackings(orderId: Long) =
        orderStore.getShipmentTrackingsForOrder(selectedSite.get(), orderId).map { it.toAppModel() }

    fun getOrderShippingLabels(remoteOrderId: Long) = shippingLabelStore
        .getShippingLabelsForOrder(selectedSite.get(), remoteOrderId)
        .filter { it.status == LabelItem.STATUS_PURCHASED }
        .map { shippingLabelMapper.toAppModel(it) }

    fun getWooServicesPluginInfo(): WooPlugin {
        val info = wooCommerceStore.getSitePlugin(selectedSite.get(), WooCommerceStore.WooPlugin.WOO_SERVICES)
        return WooPlugin(info != null, info?.isActive ?: false, info?.version)
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
