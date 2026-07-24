package com.woocommerce.android.ui.orders.list

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.compose.designsystem.component.WooBadgeTone
import com.woocommerce.android.ui.orders.list.OrderListItemUIType.LoadingItem
import com.woocommerce.android.ui.orders.list.OrderListItemUIType.OrderListItemUI
import com.woocommerce.android.ui.orders.list.OrderListItemUIType.SectionHeader
import org.wordpress.android.fluxc.model.WCOrderStatusModel

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
                    tone = status.toOrderStatusBadgeTone(),
                )
            )
            if (salesChannelLabel is OrderListItemUI.SalesChannelLabel.Visible) {
                add(
                    OrderListBadgeUiModel(
                        text = salesChannelLabel.text,
                        tone = WooBadgeTone.NeutralOutlined,
                    )
                )
            }
        },
        isCompleted = status == Order.Status.Completed.value,
        showDivider = !isLastItemInSection,
    )
}

private fun String.toOrderStatusBadgeTone(): WooBadgeTone = when (Order.Status.fromValue(this)) {
    Order.Status.Processing -> WooBadgeTone.Success
    Order.Status.Completed -> WooBadgeTone.Info
    Order.Status.OnHold -> WooBadgeTone.Caution
    Order.Status.Failed -> WooBadgeTone.Error
    Order.Status.Pending,
    Order.Status.Cancelled,
    Order.Status.Refunded,
    is Order.Status.Custom -> WooBadgeTone.Neutral
}
