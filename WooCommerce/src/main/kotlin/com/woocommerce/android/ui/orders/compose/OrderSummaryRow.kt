package com.woocommerce.android.ui.orders.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme

@Composable
fun OrderSummaryRow(
    order: OrderSummaryRowModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .focusable(true)
            .clickable(onClick = onClick)
    ) {
        val contentModifier = Modifier
            .fillMaxWidth()
            .padding(ROW_PADDING)
        val contentWidth = maxWidth - ROW_PADDING * 2

        if (contentWidth < COMPACT_CONTENT_WIDTH_THRESHOLD) {
            CompactOrderSummaryRow(
                order = order,
                modifier = contentModifier,
            )
        } else {
            ExpandedOrderSummaryRow(
                order = order,
                modifier = contentModifier,
            )
        }
    }
}

@Composable
private fun ExpandedOrderSummaryRow(
    order: OrderSummaryRowModel,
    modifier: Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space5),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = order.number,
                    style = WooTheme.text.bodyLarge.regular,
                    color = WooTheme.colors.surface.onDefault,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = order.date,
                    style = WooTheme.text.bodyLarge.regular,
                    color = WooTheme.colors.surface.onDefault,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = WooTheme.padding.padding5),
                )
            }
            Text(
                text = order.customerName,
                style = WooTheme.text.bodyLarge.regular,
                color = WooTheme.colors.surface.onDefault,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = WooTheme.padding.padding3),
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3)) {
                OrderStatusTags(order)
            }
            Text(
                text = order.totalPrice,
                style = WooTheme.text.bodyLarge.regular,
                color = WooTheme.colors.surface.onDefault,
                modifier = Modifier.padding(top = WooTheme.padding.padding3),
            )
        }
    }
}

@Composable
private fun CompactOrderSummaryRow(
    order: OrderSummaryRowModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = order.number,
                style = WooTheme.text.bodyLarge.regular,
                color = WooTheme.colors.surface.onDefault,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3)) {
                OrderStatusTags(order)
            }
        }
        val metadata = listOf(order.customerName, order.date).filter { it.isNotBlank() }
        if (metadata.isNotEmpty()) {
            Text(
                text = metadata.joinToString("  "),
                style = WooTheme.text.bodyLarge.regular,
                color = WooTheme.colors.surface.onDefault,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = order.totalPrice,
            style = WooTheme.text.bodyLarge.regular,
            color = WooTheme.colors.surface.onDefault,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun OrderStatusTags(order: OrderSummaryRowModel) {
    OrderTag(
        text = order.status,
        textColor = colorResource(id = R.color.color_on_secondary),
        backgroundColor = colorResource(id = order.statusColor),
    )
    if (order.isPosOrder) {
        OrderTag(
            text = stringResource(id = R.string.pos_badge),
            textColor = colorResource(id = R.color.tag_text_pos),
            backgroundColor = colorResource(id = R.color.tag_bg_pos),
        )
    }
}

@Composable
private fun OrderTag(
    text: String,
    textColor: Color,
    backgroundColor: Color,
) {
    Text(
        text = text,
        color = textColor,
        style = WooTheme.text.labelSmall.regular,
        modifier = Modifier
            .clip(RoundedCornerShape(WooTheme.radius.small))
            .background(backgroundColor)
            .padding(
                horizontal = WooTheme.padding.padding3 + WooTheme.padding.padding1,
                vertical = WooTheme.padding.padding2,
            ),
    )
}

private val ROW_PADDING = 16.dp
private val COMPACT_CONTENT_WIDTH_THRESHOLD = 320.dp
