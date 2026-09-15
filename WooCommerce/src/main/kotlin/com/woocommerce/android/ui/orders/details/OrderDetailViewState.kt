package com.woocommerce.android.ui.orders.details

import android.os.Parcelable
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Refund
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus

@Parcelize
data class OrderDetailViewState(
    val orderInfo: OrderInfo? = null,
    val toolbarTitle: String = "",
    val orderStatus: Order.OrderStatus? = null,
    val isOrderDetailSkeletonShown: Boolean? = null,
    val isRefreshing: Boolean? = null,
    val isShipmentTrackingAvailable: Boolean? = null,
    val isCreateShippingLabelButtonVisible: Boolean? = null,
    val isProductListVisible: Boolean? = null,
    val isProductListMenuVisible: Boolean? = null,
    val wcShippingBannerVisible: Boolean? = null,
    val isWcShippingBannerEnabled: Boolean = false,
    val isAIThankYouNoteButtonShown: Boolean = false,
    val isOrderDetailEmpty: Boolean = false,
) : Parcelable {
    val isMarkOrderCompleteButtonVisible: Boolean?
        get() = if (orderStatus != null && (orderStatus.statusKey != CoreOrderStatus.COMPLETED.value)) {
            orderInfo?.order?.isOrderPaid
        } else {
            false
        }

    @Parcelize
    data class OrderInfo(
        val order: Order? = null,
        val isVirtualOrder: Boolean = false,
        val isPaymentCollectableWithCardReader: Boolean = false,
        val receiptButtonStatus: ReceiptButtonStatus = ReceiptButtonStatus.Hidden,
    ) : Parcelable

    data class RefundsState(
        val refunds: List<Refund>,
        val refundedProductsCount: Int,
        val shippingLines: List<Refund.ShippingLine>
    ) {
        val isVisible: Boolean
            get() = refundedProductsCount > 0 || shippingLines.isNotEmpty()
    }

    enum class ReceiptButtonStatus {
        Loading, Hidden, Visible
    }
}
