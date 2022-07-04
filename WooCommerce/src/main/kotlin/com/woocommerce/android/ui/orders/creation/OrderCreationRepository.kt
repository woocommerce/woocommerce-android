package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.ShippingLine
import com.woocommerce.android.model.Order.Status.Companion.AUTO_DRAFT
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.order.LineItem
import org.wordpress.android.fluxc.model.order.UpdateOrderRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.OrderUpdateStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import javax.inject.Inject
import org.wordpress.android.fluxc.model.order.FeeLine as WCFeeLine
import org.wordpress.android.fluxc.model.order.ShippingLine as WCShippingLine

class OrderCreationRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val orderStore: WCOrderStore,
    private val orderUpdateStore: OrderUpdateStore,
    private val orderMapper: OrderMapper,
    private val dispatchers: CoroutineDispatchers,
    private val wooCommerceStore: WooCommerceStore,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    suspend fun placeOrder(order: Order): Result<Order> {
        val request = UpdateOrderRequest(
            status = order.status.toDataModel(),
            lineItems = order.items.map { item ->
                LineItem(
                    id = item.itemId.takeIf { it != 0L },
                    name = item.name,
                    productId = item.productId,
                    variationId = item.variationId,
                    quantity = item.quantity
                )
            },
            shippingAddress = order.shippingAddress.takeIf { it != Address.EMPTY }?.toShippingAddressModel(),
            billingAddress = order.billingAddress.takeIf { it != Address.EMPTY }?.toBillingAddressModel(),
            customerNote = order.customerNote,
            shippingLines = order.shippingLines.map { it.toDataModel() }
        )
        val result = if (order.id == 0L) {
            orderUpdateStore.createOrder(selectedSite.get(), request)
        } else {
            orderUpdateStore.updateOrder(selectedSite.get(), order.id, request)
        }

        return when {
            result.isError -> Result.failure(WooException(result.error))
            else -> Result.success(orderMapper.toAppModel(result.model!!))
        }
    }

    suspend fun createSimplePaymentOrder(currentPrice: BigDecimal): Result<Order> {
        val status = if (isAutoDraftSupported()) {
            WCOrderStatusModel(statusKey = AUTO_DRAFT)
        } else {
            null
        }

        val result = orderUpdateStore.createSimplePayment(
            site = selectedSite.get(),
            amount = currentPrice.toString(),
            isTaxable = true,
            status = status
        )

        return when {
            result.isError -> {
                WooLog.e(WooLog.T.ORDERS, "${result.error.type.name}: ${result.error.message}")
                analyticsTrackerWrapper.track(
                    AnalyticsEvent.SIMPLE_PAYMENTS_FLOW_FAILED,
                    mapOf(AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_SOURCE_AMOUNT)
                )
                Result.failure(WooException(result.error))
            }
            else -> Result.success(orderMapper.toAppModel(result.model!!))
        }
    }

    suspend fun createOrUpdateDraft(order: Order): Result<Order> {
        val request = UpdateOrderRequest(
            status = order.status.toDataModel(),
            lineItems = order.items.map { item ->
                LineItem(
                    id = item.itemId.takeIf { it != 0L },
                    name = item.name,
                    productId = item.productId,
                    variationId = item.variationId,
                    quantity = item.quantity,
                    subtotal = item.subtotal.takeIf { item.itemId != 0L }?.toPlainString(),
                    total = item.total.takeIf { item.itemId != 0L }?.toPlainString()
                )
            },
            shippingAddress = order.shippingAddress.takeIf { it != Address.EMPTY }?.toShippingAddressModel(),
            billingAddress = order.billingAddress.takeIf { it != Address.EMPTY }?.toBillingAddressModel(),
            customerNote = order.customerNote,
            shippingLines = order.shippingLines.map { it.toDataModel() },
            feeLines = order.feesLines.map { it.toDataModel() }
        )

        val result = if (order.id == 0L) {
            val status = if (isAutoDraftSupported()) {
                WCOrderStatusModel(statusKey = AUTO_DRAFT)
            } else {
                WCOrderStatusModel(statusKey = CoreOrderStatus.PENDING.value)
            }

            orderUpdateStore.createOrder(selectedSite.get(), request.copy(status = status))
        } else {
            orderUpdateStore.updateOrder(selectedSite.get(), order.id, request)
        }

        return when {
            result.isError -> {
                trackOrderSyncFailed(result.error.type.name, result.error.message)
                Result.failure(WooException(result.error))
            }
            else -> Result.success(orderMapper.toAppModel(result.model!!))
        }
    }

    private fun trackOrderSyncFailed(errorType: String?, errorDescription: String?) {
        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.ORDER_SYNC_FAILED,
            errorContext = this@OrderCreationRepository.javaClass.simpleName,
            errorType = errorType,
            errorDescription = errorDescription
        )
    }

    suspend fun deleteDraftOrder(order: Order) {
        // Make sure the request is not cancelled after leaving the screen
        withContext(NonCancellable) {
            WooLog.d(T.ORDERS, "Send a request to delete draft order")
            orderUpdateStore.deleteOrder(
                site = selectedSite.get(),
                orderId = order.id,
                trash = false
            ).let {
                when {
                    it.isError -> WooLog.w(T.ORDERS, "Deleting the order draft failed, error: ${it.error.message}")
                    else -> WooLog.d(T.ORDERS, "Draft order deleted successfully")
                }
            }
        }
    }

    private suspend fun isAutoDraftSupported(): Boolean {
        val version = withContext(dispatchers.io) {
            wooCommerceStore.getSitePlugin(selectedSite.get(), WooCommerceStore.WooPlugin.WOO_CORE)?.version
                ?: "0.0"
        }
        return version.semverCompareTo(AUTO_DRAFT_SUPPORTED_VERSION) >= 0
    }

    private suspend fun Order.Status.toDataModel(): WCOrderStatusModel {
        val key = this.value
        return withContext(dispatchers.io) {
            // Currently this query will run on the current thread, so forcing the usage of IO dispatcher
            orderStore.getOrderStatusForSiteAndKey(selectedSite.get(), key)
                ?: WCOrderStatusModel(statusKey = key).apply { label = key }
        }
    }

    private fun ShippingLine.toDataModel() = WCShippingLine(
        id = itemId.takeIf { it != 0L },
        methodId = methodId,
        methodTitle = methodTitle.takeIf { it.isNotEmpty() },
        total = total.toPlainString()
    )

    private fun Order.FeeLine.toDataModel() = WCFeeLine().also {
        it.id = id
        it.name = name
        it.total = total.toPlainString()
    }

    companion object {
        const val AUTO_DRAFT_SUPPORTED_VERSION = "6.3.0"
    }
}
