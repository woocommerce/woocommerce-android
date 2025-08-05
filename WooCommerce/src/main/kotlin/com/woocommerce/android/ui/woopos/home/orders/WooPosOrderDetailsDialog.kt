package com.woocommerce.android.ui.woopos.home.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosDialogWrapper
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WooPosOrderDetailsDialog(
    isVisible: Boolean,
    order: Order?,
    onDismissRequest: () -> Unit,
    onRefundClick: (Order) -> Unit = {},
    onReceiptClick: (Order) -> Unit = {},
    viewModel: WooPosOrdersViewModel = hiltViewModel()
) {
    WooPosDialogWrapper(
        isVisible = isVisible,
        onDismissRequest = onDismissRequest,
        dialogBackgroundContentDescription = stringResource(R.string.woopos_order_details_dialog_content_description)
    ) {
        order?.let { orderData ->
            Column(
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.surfaceBright)
                    .padding(WooPosSpacing.XLarge.value.toAdaptivePadding())
            ) {
                Row {
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(
                                id = R.string.woopos_exit_dialog_confirmation_close_content_description
                            ),
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(WooPosSpacing.Large.value.toAdaptivePadding()))

                OrderDetailsContent(
                    order = orderData,
                    currencyFormatter = viewModel.currencyFormatter,
                    onRefundClick = { onRefundClick(orderData) },
                    onReceiptClick = { onReceiptClick(orderData) }
                )
            }
        }
    }
}

@Composable
private fun OrderDetailsContent(
    order: Order,
    currencyFormatter: com.woocommerce.android.util.CurrencyFormatter,
    onRefundClick: () -> Unit,
    onReceiptClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_order_details_title, order.number),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = WooPosSpacing.Medium.value.toAdaptivePadding())
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value.toAdaptivePadding()))

        OrderInfoSection(order = order, currencyFormatter = currencyFormatter)

        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value.toAdaptivePadding()))

        OrderDetailsButtonsRow(
            onRefundClick = onRefundClick,
            onReceiptClick = onReceiptClick
        )
    }
}

@Composable
private fun OrderInfoSection(
    order: Order,
    currencyFormatter: com.woocommerce.android.util.CurrencyFormatter
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(WooPosCornerRadius.Medium.value)
            )
            .padding(WooPosSpacing.Large.value.toAdaptivePadding()),
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Large.value.toAdaptivePadding())
    ) {
        OrderInfoRow(
            label = stringResource(R.string.woopos_order_details_total),
            value = currencyFormatter.formatCurrency(order.total, order.currency),
            isAmount = true
        )

        OrderInfoRow(
            label = stringResource(R.string.woopos_order_details_date),
            value = order.dateCreated?.let { formatDate(it) } ?: ""
        )

        OrderInfoRow(
            label = stringResource(R.string.woopos_order_details_status),
            value = order.status.value.uppercase(),
            isStatus = true
        )

        OrderInfoRow(
            label = stringResource(R.string.woopos_order_details_payment_method),
            value = if (order.isCashPayment) "CASH" else "CARD",
            isPaymentMethod = true
        )
    }
}

@Composable
private fun OrderInfoRow(
    label: String,
    value: String,
    isAmount: Boolean = false,
    isStatus: Boolean = false,
    isPaymentMethod: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        WooPosText(
            text = label,
            style = WooPosTypography.BodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = WooPosSpacing.XSmall.value.toAdaptivePadding())
        )

        WooPosText(
            text = value,
            style = when {
                isAmount -> WooPosTypography.Heading
                else -> WooPosTypography.BodyMedium
            },
            fontWeight = when {
                isAmount -> FontWeight.Bold
                isStatus || isPaymentMethod -> FontWeight.Medium
                else -> FontWeight.Normal
            },
            color = when {
                isAmount -> WooPosTheme.colors.onSurfaceVariantHighest
                isStatus -> when (value.lowercase()) {
                    "completed", "processing" -> MaterialTheme.colorScheme.primary
                    "pending" -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurface
                }
                isPaymentMethod -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

@Composable
private fun OrderDetailsButtonsRow(
    onRefundClick: () -> Unit,
    onReceiptClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value.toAdaptivePadding())
    ) {
        WooPosOutlinedButton(
            onClick = onRefundClick,
            text = stringResource(R.string.woopos_order_details_refund_button),
            modifier = Modifier.weight(1f)
        )

        WooPosOutlinedButton(
            onClick = onReceiptClick,
            text = stringResource(R.string.woopos_order_details_receipt_button),
            modifier = Modifier.weight(1f)
        )
    }
}

private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return formatter.format(date)
}

@Composable
@WooPosPreview
fun WooPosOrderDetailsDialogPreview() {
    WooPosTheme {
        WooPosOrderDetailsDialog(
            isVisible = true,
            order = null,
            onDismissRequest = {}
        )
    }
}
