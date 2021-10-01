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
    data class ViewRefundedProducts(val remoteOrderId: Long) : OrderNavigationTarget()
    data class ViewOrderFulfillInfo(val orderIdentifier: String) : OrderNavigationTarget()
    data class AddOrderNote(val orderIdentifier: String, val orderNumber: String) : OrderNavigationTarget()
    data class RefundShippingLabel(val remoteOrderId: Long, val shippingLabelId: Long) : OrderNavigationTarget()
    data class AddOrderShipmentTracking(
        val orderIdentifier: String,
        val orderTrackingProvider: String,
        val isCustomProvider: Boolean
    ) : OrderNavigationTarget()

    data class ViewShipmentTrackingProviders(
        val orderIdentifier: String,
        val selectedProvider: String
    ) : OrderNavigationTarget()
    data class PrintShippingLabel(val remoteOrderId: Long, val shippingLabelId: Long) : OrderNavigationTarget()
    data class ViewShippingLabelPaperSizes(val currentPaperSize: ShippingLabelPaperSize) : OrderNavigationTarget()
    object ViewCreateShippingLabelInfo : OrderNavigationTarget()
    object ViewPrintShippingLabelInfo : OrderNavigationTarget()
    object ViewShippingLabelFormatOptions : OrderNavigationTarget()
    data class ViewPrintCustomsForm(val invoices: List<String>, val isReprint: Boolean) : OrderNavigationTarget()
    data class StartShippingLabelCreationFlow(val orderIdentifier: String) : OrderNavigationTarget()
    data class StartCardReaderConnectFlow(val skipOnboarding: Boolean) : OrderNavigationTarget()
    object ShowCardReaderWelcomeDialog : OrderNavigationTarget()
    data class StartCardReaderPaymentFlow(val orderIdentifier: String) : OrderNavigationTarget()
    object ViewPrintingInstructions : OrderNavigationTarget()
    data class PreviewReceipt(val billingEmail: String, val receiptUrl: String, val orderId: Long) :
        OrderNavigationTarget()
    data class ViewOrderedAddons(val remoteOrderID: Long, val orderItemID: Long, val addonsProductID: Long) :
        OrderNavigationTarget()
}
