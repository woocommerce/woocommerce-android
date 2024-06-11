package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.AppConstants
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_FEEDBACK_ACTION
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_API_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_API_SUCCESS
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.model.OrderShipmentTracking
import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.model.ShippingLabelMapper
import com.woocommerce.android.model.WooPlugin
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.model.toOrderStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.ORDERS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.wordpress.android.fluxc.model.OrderAttributionInfo
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels.LabelItem
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCRefundStore
import org.wordpress.android.fluxc.store.WCShippingLabelStore
import org.wordpress.android.fluxc.store.WooCommerceStore
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
    private val shippingLabelMapper: ShippingLabelMapper
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
            if (result?.error?.type == WCOrderStore.OrderErrorType.PLUGIN_NOT_ACTIVE) {
                RequestResult.API_ERROR
            } else {
                RequestResult.ERROR
            }
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
            } else {
                VALUE_API_SUCCESS
            }
            AnalyticsTracker.track(AnalyticsEvent.SHIPPING_LABEL_API_REQUEST, mapOf(KEY_FEEDBACK_ACTION to action))
            result.model?.filter { it.status == LabelItem.STATUS_PURCHASED }
                ?.map { shippingLabelMapper.toAppModel(it) }
                ?: emptyList()
        }
    }

    suspend fun updateOrderStatus(
        orderId: Long,
        newStatus: String
    ): Flow<WCOrderStore.UpdateOrderResult> {
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
            if (it.isError) {
                Result.failure(WooException(it.error))
            } else {
                Result.success(Unit)
            }
        }
    }

    suspend fun addOrderShipmentTracking(
        orderId: Long,
        shipmentTrackingModel: OrderShipmentTracking
    ): OnOrderChanged {
        return orderStore.addOrderShipmentTracking(
            WCOrderStore.AddOrderShipmentTrackingPayload(
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
            WCOrderStore.DeleteOrderShipmentTrackingPayload(
                selectedSite.get(),
                orderId,
                shipmentTrackingModel
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
        } else {
            false
        }
    }

    fun getProductCountForOrder(remoteProductIds: List<Long>): Int {
        return if (remoteProductIds.isNotEmpty()) {
            productStore.getProductCountByRemoteIds(selectedSite.get(), remoteProductIds)
        } else {
            0
        }
    }

    suspend fun getUniqueProductTypes(remoteProductIds: List<Long>): String {
        return if (remoteProductIds.isNotEmpty()) {
            val products = productStore.getProductsByRemoteIds(selectedSite.get(), remoteProductIds)
            products.map { product -> product.type }.toSet().joinToString()
        } else {
            ""
        }
    }

    fun hasSubscriptionProducts(remoteProductIds: List<Long>): Boolean {
        return if (remoteProductIds.isNotEmpty()) {
            productStore.getProductsByRemoteIds(selectedSite.get(), remoteProductIds)
                .any { it.type == PRODUCT_SUBSCRIPTION_TYPE }
        } else {
            false
        }
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
        selectedSite.get(),
        orderId,
        trackingNumber
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

    fun getWooShippingPluginInfo(): WooPlugin {
        val info = wooCommerceStore.getSitePlugin(selectedSite.get(), WooCommerceStore.WooPlugin.WOO_SHIPPING)
        return WooPlugin(info != null, info?.isActive ?: false, info?.version)
    }

    suspend fun getOrderDetailsPluginsInfo(): Map<String, WooPlugin> {
        // Add WOO_CORE to the list to make sure if there is data in the plugins table
        val plugins = listOf(
            WooCommerceStore.WooPlugin.WOO_CORE,
            WooCommerceStore.WooPlugin.WOO_SERVICES,
            WooCommerceStore.WooPlugin.WOO_SHIPMENT_TRACKING,
            WooCommerceStore.WooPlugin.WOO_SUBSCRIPTIONS,
            WooCommerceStore.WooPlugin.WOO_GIFT_CARDS
        )

        val result = HashMap<String, WooPlugin>()
        val information = wooCommerceStore.getSitePlugins(selectedSite.get(), plugins).associateBy { it.name }

        if (information.isEmpty()) {
            AnalyticsTracker.track(AnalyticsEvent.PLUGINS_NOT_SYNCED_YET)
            // return earlier, no plugins info in the database
            return result
        }

        plugins.associateByTo(
            destination = result,
            keySelector = { plugin -> plugin.pluginName },
            valueTransform = { plugin ->
                val info = information[plugin.pluginName]
                WooPlugin(info != null, info?.isActive ?: false, info?.version)
            }
        )
        return result
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

    suspend fun orderHasMetadata(orderId: Long) = orderStore.hasDisplayableOrderMetadata(orderId, selectedSite.get())

    suspend fun getOrderMetadata(orderId: Long) = orderStore.getDisplayableOrderMetadata(orderId, selectedSite.get())

    suspend fun getOrderAttributionInfo(orderId: Long) = OrderAttributionInfo(
        orderStore.getOrderMetadata(orderId, selectedSite.get())
    )

    companion object {
        const val PRODUCT_SUBSCRIPTION_TYPE = "subscription"
    }
}
