package com.woocommerce.android.ui.orders

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelPaperSizeSelectorDialog.ShippingLabelPaperSize
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event

sealed class OrderNavigationTarget : Event() {
    data class ViewOrderStatusSelector(
        val currentStatus: String,
        val orderStatusList: Array<Order.OrderStatus>
    ) : OrderNavigationTarget() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ViewOrderStatusSelector

            if (currentStatus != other.currentStatus) return false
            if (!orderStatusList.contentEquals(other.orderStatusList)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = currentStatus.hashCode()
            result = 31 * result + orderStatusList.contentHashCode()
            return result
        }
    }

    data class IssueOrderRefund(val remoteOrderId: Long) : OrderNavigationTarget()
    data class ViewRefundedProducts(val orderId: Long) : OrderNavigationTarget()
    data class ViewOrderFulfillInfo(val orderId: Long) : OrderNavigationTarget()
    data class AddOrderNote(val orderId: Long, val orderNumber: String) : OrderNavigationTarget()
    data class RefundShippingLabel(val remoteOrderId: Long, val shippingLabelId: Long) : OrderNavigationTarget()
    data class AddOrderShipmentTracking(
        val orderId: Long,
        val orderTrackingProvider: String,
        val isCustomProvider: Boolean
    ) : OrderNavigationTarget()

    data class ViewShipmentTrackingProviders(
        val orderId: Long,
        val selectedProvider: String
    ) : OrderNavigationTarget()
    data class PrintShippingLabel(val remoteOrderId: Long, val shippingLabelId: Long) : OrderNavigationTarget()
    data class ViewShippingLabelPaperSizes(val currentPaperSize: ShippingLabelPaperSize) : OrderNavigationTarget()
    object ViewCreateShippingLabelInfo : OrderNavigationTarget()
    object ViewPrintShippingLabelInfo : OrderNavigationTarget()
    object ViewShippingLabelFormatOptions : OrderNavigationTarget()
    data class ViewPrintCustomsForm(val invoices: List<String>, val isReprint: Boolean) : OrderNavigationTarget()
    data class StartShippingLabelCreationFlow(val orderId: Long) : OrderNavigationTarget()
    data class StartPaymentFlow(val orderId: Long) : OrderNavigationTarget()
    object ViewPrintingInstructions : OrderNavigationTarget()
    data class PreviewReceipt(val billingEmail: String, val receiptUrl: String, val orderId: Long) :
        OrderNavigationTarget()
    data class ViewOrderedAddons(val remoteOrderID: Long, val orderItemID: Long, val addonsProductID: Long) :
        OrderNavigationTarget()
    data class EditOrder(val orderId: Long) : OrderNavigationTarget()
    data class ShowOrder(val orderId: Long, val allOrderIds: LongArray) : OrderNavigationTarget()
    data class ViewCustomFields(val orderId: Long) : OrderNavigationTarget()
}
