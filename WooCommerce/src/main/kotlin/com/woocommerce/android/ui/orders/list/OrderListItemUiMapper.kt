package com.woocommerce.android.ui.orders.list

import androidx.annotation.ColorRes
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.list.OrderListItemUIType.LoadingItem
import com.woocommerce.android.ui.orders.list.OrderListItemUIType.OrderListItemUI
import com.woocommerce.android.ui.orders.list.OrderListItemUIType.SectionHeader
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import java.util.Locale

internal fun OrderListItemUIType.toUiModel(
    orderStatusOptions: Map<String, WCOrderStatusModel>,
    formatCurrency: (rawValue: String, currencyCode: String) -> String,
    resolveString: (resourceId: Int) -> String,
): OrderListItemUiModel = when (this) {
    is SectionHeader -> OrderListItemUiModel.DateSection(
        title = resolveString(title.labelRes),
    )
    is LoadingItem -> OrderListItemUiModel.Loading(orderId)
    is OrderListItemUI -> OrderListItemUiModel.Order(
        orderId = orderId,
        number = "#$orderNumber",
        customerName = orderName,
        dateCreated = dateCreated,
        total = formatCurrency(orderTotal, currencyCode),
        badges = buildList {
            add(
                OrderListBadgeUiModel(
                    text = orderStatusOptions[status]?.label ?: status,
                    containerColorRes = status.toOrderStatusBadgeContainerColor(),
                    contentColorRes = R.color.tagView_text,
                )
            )
            if (salesChannelLabel is OrderListItemUI.SalesChannelLabel.Visible) {
                add(
                    OrderListBadgeUiModel(
                        text = salesChannelLabel.text,
                        containerColorRes = R.color.tag_bg_pos,
                        contentColorRes = R.color.tag_text_pos,
                    )
                )
            }
        },
        isCompleted = status == Order.Status.Completed.value,
        showDivider = !isLastItemInSection,
    )
}

@ColorRes
private fun String.toOrderStatusBadgeContainerColor(): Int = when (trim().lowercase(Locale.US)) {
    CoreOrderStatus.PROCESSING.value -> R.color.tag_bg_processing
    CoreOrderStatus.FAILED.value -> R.color.tag_bg_failed
    CoreOrderStatus.COMPLETED.value -> R.color.tag_bg_completed
    CoreOrderStatus.ON_HOLD.value -> R.color.tag_bg_on_hold
    CoreOrderStatus.PENDING.value,
    CoreOrderStatus.CANCELLED.value,
    CoreOrderStatus.REFUNDED.value -> R.color.tag_bg_other
    else -> R.color.tagView_bg
}
