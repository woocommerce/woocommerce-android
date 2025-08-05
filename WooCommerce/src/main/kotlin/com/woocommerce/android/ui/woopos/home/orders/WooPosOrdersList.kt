package com.woocommerce.android.ui.woopos.home.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.ShadowType
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosLazyColumn
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WooPosOrdersList(
    modifier: Modifier = Modifier,
    viewModel: WooPosOrdersViewModel = hiltViewModel(),
    onOrderClick: (Order) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            state.error != null -> {
                WooPosText(
                    text = state.error ?: "Error loading orders",
                    style = WooPosTypography.BodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(WooPosSpacing.Large.value.toAdaptivePadding())
                )
            }
            state.orders.isEmpty() -> {
                WooPosText(
                    text = stringResource(R.string.woopos_orders_empty_list),
                    style = WooPosTypography.BodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(WooPosSpacing.Large.value.toAdaptivePadding())
                )
            }
            else -> {
                WooPosLazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = WooPosSpacing.Medium.value.toAdaptivePadding(),
                        vertical = WooPosSpacing.Medium.value.toAdaptivePadding()
                    ),
                    verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value)
                ) {
                    items(state.orders.size) { index ->
                        OrderItem(
                            order = state.orders[index],
                            onClick = { onOrderClick(state.orders[index]) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderItem(
    order: Order,
    onClick: () -> Unit
) {
    WooPosCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
            .clickable { onClick() },
        elevation = WooPosElevation.Medium,
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowType = ShadowType.Soft,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WooPosSpacing.Medium.value.toAdaptivePadding())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WooPosText(
                    text = "#${order.number}",
                    style = WooPosTypography.BodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                OrderStatusChip(status = order.status)
            }

            Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value.toAdaptivePadding()))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WooPosText(
                        text = formatDate(order.dateCreated),
                        style = WooPosTypography.Caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.width(WooPosSpacing.Small.value.toAdaptivePadding()))

                    PaymentMethodChip(order = order)
                }

                WooPosText(
                    text = state.formatPrice(order, viewModel.currencyFormatter),
                    style = WooPosTypography.BodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = WooPosTheme.colors.onSurfaceVariantHighest,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun PaymentMethodChip(order: Order) {
    val isCashPayment = order.isCashPayment
    val icon = if (isCashPayment) Icons.Default.Payments else Icons.Default.CreditCard
    val text = if (isCashPayment) "CASH" else "CARD"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(WooPosCornerRadius.Small.value)
            )
            .padding(
                horizontal = WooPosSpacing.XSmall.value.toAdaptivePadding(),
                vertical = 2.dp
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(12.dp)
        )

        Spacer(modifier = Modifier.width(2.dp))

        WooPosText(
            text = text,
            style = WooPosTypography.Caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun OrderStatusChip(status: Order.Status) {
    val backgroundColor = when (status) {
        Order.Status.Completed -> WooPosTheme.colors.success.copy(alpha = 0.1f)
        Order.Status.Processing -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        Order.Status.Cancelled, Order.Status.Failed -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
    }

    val textColor = when (status) {
        Order.Status.Completed -> WooPosTheme.colors.success
        Order.Status.Processing -> MaterialTheme.colorScheme.primary
        Order.Status.Cancelled, Order.Status.Failed -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(WooPosCornerRadius.Small.value)
            )
            .padding(
                horizontal = WooPosSpacing.Small.value.toAdaptivePadding(),
                vertical = WooPosSpacing.XSmall.value.toAdaptivePadding()
            )
    ) {
        WooPosText(
            text = status.value.uppercase(),
            style = WooPosTypography.Caption,
            fontWeight = FontWeight.Medium,
            color = textColor,
            maxLines = 1
        )
    }
}

private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return formatter.format(date)
}

@Composable
@WooPosPreview
fun WooPosOrdersListPreview() {
    WooPosTheme {
        WooPosOrdersList()
    }
}
