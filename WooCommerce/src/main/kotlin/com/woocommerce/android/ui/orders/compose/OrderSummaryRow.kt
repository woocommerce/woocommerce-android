package com.woocommerce.android.ui.orders.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCTag

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
            DashboardOrderSummaryRow(
                order = order,
                modifier = contentModifier,
            )
        }
    }
}

@Composable
private fun DashboardOrderSummaryRow(
    order: OrderSummaryRowModel,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Column(modifier = Modifier.align(Alignment.TopStart)) {
            Row {
                Text(
                    text = order.number,
                    style = MaterialTheme.typography.body1,
                    color = colorResource(id = R.color.color_on_surface_medium),
                )

                Text(
                    text = order.date,
                    style = MaterialTheme.typography.body1,
                    color = colorResource(id = R.color.color_on_surface_medium),
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            Text(
                text = order.customerName,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Column(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalAlignment = Alignment.End,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OrderStatusTags(order = order)
            }

            Text(
                text = order.totalPrice,
                style = MaterialTheme.typography.body1,
                modifier = Modifier.padding(top = 8.dp)
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = order.number,
                style = MaterialTheme.typography.body1,
                color = colorResource(id = R.color.color_on_surface_medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OrderStatusTags(order = order)
            }
        }

        val metadata = listOf(order.customerName, order.date).filter { it.isNotBlank() }
        if (metadata.isNotEmpty()) {
            Text(
                text = metadata.joinToString("  "),
                style = MaterialTheme.typography.body1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(
            text = order.totalPrice,
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun OrderStatusTags(order: OrderSummaryRowModel) {
    WCTag(
        text = order.status,
        textColor = colorResource(id = R.color.color_on_secondary),
        backgroundColor = colorResource(id = order.statusColor),
        fontWeight = FontWeight.Normal,
    )

    if (order.isPosOrder) {
        WCTag(
            text = stringResource(id = R.string.pos_badge),
            textColor = colorResource(id = R.color.tag_text_pos),
            backgroundColor = colorResource(id = R.color.tag_bg_pos),
            fontWeight = FontWeight.Normal,
        )
    }
}

private val ROW_PADDING = 16.dp
private val COMPACT_CONTENT_WIDTH_THRESHOLD = 320.dp
