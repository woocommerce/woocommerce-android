package com.woocommerce.android.ui.aiassistant

import androidx.annotation.ColorRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.woocommerce.android.R
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardAction
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardRenderer
import com.woocommerce.android.ciab.CIABOrderStatusMapper
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.compose.OrderSummaryRow
import com.woocommerce.android.ui.orders.compose.OrderSummaryRowModel
import com.woocommerce.android.util.CurrencyFormatter

class WooAssistantCardRenderer(
    private val currencyFormatter: CurrencyFormatter,
) : AssistantCardRenderer {
    @Composable
    override fun OrderCard(
        card: AssistantCard.Order,
        onAction: (AssistantCardAction) -> Unit,
        modifier: Modifier,
    ) {
        OrderSummaryRow(
            order = card.toOrderSummaryRowModel(currencyFormatter),
            onClick = { onAction(card.toOpenOrderAction()) },
            modifier = modifier,
        )
    }
}

internal fun AssistantCard.Order.toOpenOrderAction() = AssistantCardAction.OpenOrder(remoteOrderId)

internal fun AssistantCard.Order.toOrderSummaryRowModel(currencyFormatter: CurrencyFormatter) = OrderSummaryRowModel(
    number = number,
    date = date,
    customerName = customerName,
    status = status,
    statusColor = status.toOrderStatusColor(),
    totalPrice = formatTotalPrice(currencyFormatter),
    isPosOrder = false,
)

private fun AssistantCard.Order.formatTotalPrice(currencyFormatter: CurrencyFormatter): String =
    when {
        total.isBlank() -> ""
        currency.isBlank() -> total
        else -> currencyFormatter.formatCurrency(total, currency)
    }

@ColorRes
private fun String.toOrderStatusColor(): Int =
    when (Order.Status.fromValue(this)) {
        is Order.Status.Processing -> R.color.tag_bg_processing
        is Order.Status.Completed -> R.color.tag_bg_completed
        is Order.Status.Failed -> R.color.tag_bg_failed
        is Order.Status.OnHold -> R.color.tag_bg_on_hold
        is Order.Status.Custom -> if (this == CIABOrderStatusMapper.OPEN_KEY) {
            R.color.tag_bg_processing
        } else {
            R.color.tag_bg_other
        }
        else -> R.color.tag_bg_other
    }
