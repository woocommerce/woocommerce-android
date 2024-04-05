package com.woocommerce.android.ui.orders.details

import android.os.Parcelable
import com.woocommerce.android.model.Order
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus

@Parcelize
data class OrderDetailViewState(
    val orderInfo: OrderInfo? = null,
    val toolbarTitle: String? = null,
    val orderStatus: Order.OrderStatus? = null,
    val isOrderDetailSkeletonShown: Boolean? = null,
    val isRefreshing: Boolean? = null,
    val isShipmentTrackingAvailable: Boolean? = null,
    val refreshedProductId: Long? = null,
    val isCreateShippingLabelButtonVisible: Boolean? = null,
    val isProductListVisible: Boolean? = null,
    val areShippingLabelsVisible: Boolean? = null,
    val isProductListMenuVisible: Boolean? = null,
    val wcShippingBannerVisible: Boolean? = null,
    val isCustomFieldsButtonShown: Boolean? = null,
    val isAIThankYouNoteButtonShown: Boolean = false,
    val isOrderDetailEmpty: Boolean = false,
) : Parcelable {
    val isMarkOrderCompleteButtonVisible: Boolean?
        get() = if (orderStatus != null && (orderStatus.statusKey != CoreOrderStatus.COMPLETED.value)) {
            orderInfo?.order?.isOrderPaid
        } else {
            false
        }

    val isCreateShippingLabelBannerVisible: Boolean
        get() = isCreateShippingLabelButtonVisible == true && isProductListVisible == true

    @Parcelize
    data class OrderInfo(
        val order: Order? = null,
        val isPaymentCollectableWithCardReader: Boolean = false,
        val receiptButtonStatus: ReceiptButtonStatus = ReceiptButtonStatus.Hidden,
    ) : Parcelable

    enum class ReceiptButtonStatus {
        Loading, Hidden, Visible
    }
}
