package com.woocommerce.android.ui.orders

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus

sealed class OrderStatusUpdateSource(open val oldStatus: String, open val newStatus: String) : Parcelable {
    @Parcelize
    data class SwipeToCompleteGesture(
        val orderId: Long,
        val orderPosition: Int,
        override val oldStatus: String
    ) : OrderStatusUpdateSource(
        oldStatus = oldStatus,
        newStatus = CoreOrderStatus.COMPLETED.value
    )

    @Parcelize
    data class FullFillScreen(override val oldStatus: String) : OrderStatusUpdateSource(
        oldStatus = oldStatus,
        newStatus = CoreOrderStatus.COMPLETED.value
    )

    @Parcelize
    data class Dialog(
        override val oldStatus: String,
        override val newStatus: String
    ) : OrderStatusUpdateSource(oldStatus, newStatus)
}
