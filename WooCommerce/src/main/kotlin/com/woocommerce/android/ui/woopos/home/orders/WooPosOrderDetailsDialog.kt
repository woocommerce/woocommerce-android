package com.woocommerce.android.ui.woopos.home.orders

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosDialogWrapper
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosInputField
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToolbar
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("CyclomaticComplexMethod")
@Composable
fun WooPosOrderDetailsScreen(
    orderId: Long,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
    viewModel: WooPosOrdersViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    // Find the specific order by ID
    val order = state.orders.find { it.order.id == orderId }?.order
    
    var refundState: RefundDialogState by remember { mutableStateOf(RefundDialogState.OrderDetails) }
    var refundAmount by remember { mutableStateOf(BigDecimal.ZERO) }
    var refundReason by remember { mutableStateOf("") }
    var refundMethod by remember { mutableStateOf(RefundMethod.CASH) }
    var isProcessingRefund by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        WooPosToolbar(
            titleText = when (refundState) {
                RefundDialogState.OrderDetails -> stringResource(R.string.woopos_order_details_title, order?.number ?: "")
                RefundDialogState.RefundAmount -> stringResource(R.string.woopos_refund_amount_title)
                RefundDialogState.RefundMethod -> stringResource(R.string.woopos_refund_method_title)
                RefundDialogState.RefundConfirmation -> stringResource(R.string.woopos_refund_confirmation_title)
                RefundDialogState.RefundSuccess -> stringResource(R.string.woopos_refund_success_title)
            },
            onBackClicked = {
                when (refundState) {
                    RefundDialogState.OrderDetails -> onNavigationEvent(WooPosNavigationEvent.GoBack)
                    RefundDialogState.RefundAmount -> {
                        refundState = RefundDialogState.OrderDetails
                        refundAmount = BigDecimal.ZERO
                    }
                    RefundDialogState.RefundMethod -> refundState = RefundDialogState.RefundAmount
                    RefundDialogState.RefundConfirmation -> refundState = RefundDialogState.RefundMethod
                    RefundDialogState.RefundSuccess -> onNavigationEvent(WooPosNavigationEvent.GoBack)
                }
            }
        )

        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            state.error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(WooPosSpacing.Large.value.toAdaptivePadding()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    WooPosText(
                        text = state.error ?: "Error loading order",
                        style = WooPosTypography.BodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
            order != null -> {
                AnimatedContent(
                    targetState = refundState,
                    transitionSpec = {
                        fadeIn(
                            animationSpec = tween(
                                durationMillis = 250,
                                delayMillis = 200,
                                easing = FastOutSlowInEasing
                            )
                        ) togetherWith fadeOut(
                            animationSpec = tween(
                                durationMillis = 200,
                                easing = FastOutSlowInEasing
                            )
                        )
                    },
                    label = "refund_state_transition",
                    modifier = Modifier.fillMaxSize()
                ) { state ->
                    when (state) {
                        RefundDialogState.OrderDetails -> {
                            OrderDetailsContent(
                                order = order,
                                currencyFormatter = viewModel.currencyFormatter,
                                onRefundClick = {
                                    refundState = RefundDialogState.RefundAmount
                                },
                                onReceiptClick = {
                                    onNavigationEvent(WooPosNavigationEvent.OpenEmailReceipt(order.id))
                                }
                            )
                        }
                        RefundDialogState.RefundAmount -> {
                            RefundAmountContent(
                                order = order,
                                currencyFormatter = viewModel.currencyFormatter,
                                refundAmount = refundAmount,
                                onAmountChange = { refundAmount = it },
                                onContinue = {
                                    refundState = RefundDialogState.RefundMethod
                                }
                            )
                        }
                        RefundDialogState.RefundMethod -> {
                            RefundMethodContent(
                                refundMethod = refundMethod,
                                onMethodSelected = { refundMethod = it },
                                onContinue = {
                                    refundState = RefundDialogState.RefundConfirmation
                                }
                            )
                        }
                        RefundDialogState.RefundConfirmation -> {
                            RefundConfirmationContent(
                                order = order,
                                currencyFormatter = viewModel.currencyFormatter,
                                refundAmount = refundAmount,
                                refundReason = refundReason,
                                refundMethod = refundMethod,
                                onReasonChange = { refundReason = it },
                                isProcessing = isProcessingRefund,
                                onConfirm = {
                                    isProcessingRefund = true
                                    viewModel.processRefund(
                                        order = order,
                                        amount = refundAmount,
                                        reason = refundReason,
                                        method = refundMethod
                                    ) { success ->
                                        isProcessingRefund = false
                                        if (success) {
                                            refundState = RefundDialogState.RefundSuccess
                                        }
                                    }
                                }
                            )
                        }
                        RefundDialogState.RefundSuccess -> {
                            RefundSuccessContent(
                                refundAmount = refundAmount,
                                currencyFormatter = viewModel.currencyFormatter,
                                currencyCode = order.currency,
                                onClose = { onNavigationEvent(WooPosNavigationEvent.GoBack) }
                            )
                        }
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(WooPosSpacing.Large.value.toAdaptivePadding()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    WooPosText(
                        text = "Order not found",
                        style = WooPosTypography.BodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
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
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(WooPosSpacing.Large.value.toAdaptivePadding()),
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Large.value.toAdaptivePadding())
    ) {
        // Order info section with larger, more readable layout
        OrderInfoSection(order = order, currencyFormatter = currencyFormatter)

        // Products section with enhanced readability
        if (order.items.isNotEmpty()) {
            OrderProductsSection(order = order, currencyFormatter = currencyFormatter)
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action buttons at bottom
        OrderDetailsButtonsRow(
            onRefundClick = onRefundClick,
            onReceiptClick = onReceiptClick
        )
    }
}

@Composable
private fun OrderStatusSection(status: Order.Status) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_order_details_status),
            style = WooPosTypography.BodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value.toAdaptivePadding()))
        
        OrderStatusChip(status = status, isLarge = true)
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
        // Total amount - prominent display
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WooPosText(
                text = stringResource(R.string.woopos_order_details_total),
                style = WooPosTypography.BodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(WooPosSpacing.Small.value.toAdaptivePadding()))
            
            WooPosText(
                text = currencyFormatter.formatCurrency(order.total, order.currency),
                style = WooPosTypography.Heading,
                fontWeight = FontWeight.Bold,
                color = WooPosTheme.colors.onSurfaceVariantHighest
            )
        }
        
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )

        // Order details in readable rows
        OrderInfoRow(
            label = stringResource(R.string.woopos_order_details_date),
            value = order.dateCreated?.let { formatDate(it) } ?: ""
        )
        
        OrderInfoRow(
            label = stringResource(R.string.woopos_order_details_payment_method),
            value = ""
        ) {
            PaymentMethodChip(order = order, isLarge = true)
        }
        
        OrderInfoRow(
            label = stringResource(R.string.woopos_order_details_status),
            value = ""
        ) {
            OrderStatusChip(status = order.status, isLarge = true)
        }
    }
}

@Composable
private fun OrderProductsSection(
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
            .padding(WooPosSpacing.Large.value.toAdaptivePadding())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WooPosText(
                text = stringResource(R.string.woopos_order_details_products_title),
                style = WooPosTypography.BodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            WooPosText(
                text = "${order.items.size} item${if (order.items.size > 1) "s" else ""}",
                style = WooPosTypography.BodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value.toAdaptivePadding()))

        order.items.forEachIndexed { index, item ->
            if (index > 0) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = WooPosSpacing.Small.value.toAdaptivePadding())
                )
            }
            OrderProductItem(item = item, currencyFormatter = currencyFormatter, currency = order.currency)
        }
    }
}

@Composable
private fun OrderInfoRow(
    label: String,
    value: String,
    customContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WooPosText(
            text = label,
            style = WooPosTypography.BodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        if (customContent != null) {
            customContent()
        } else {
            WooPosText(
                text = value,
                style = WooPosTypography.BodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun OrderProductItem(
    item: Order.Item,
    currencyFormatter: com.woocommerce.android.util.CurrencyFormatter,
    currency: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = WooPosSpacing.Small.value.toAdaptivePadding()),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            WooPosText(
                text = item.name,
                style = WooPosTypography.BodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )

            if (item.sku.isNotEmpty()) {
                WooPosText(
                    text = "SKU: ${item.sku}",
                    style = WooPosTypography.BodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = WooPosSpacing.XSmall.value.toAdaptivePadding())
                )
            }

            if (item.attributesDescription.isNotEmpty()) {
                WooPosText(
                    text = item.attributesDescription,
                    style = WooPosTypography.BodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = WooPosSpacing.XSmall.value.toAdaptivePadding())
                )
            }

            Row(
                modifier = Modifier.padding(top = WooPosSpacing.Small.value.toAdaptivePadding()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WooPosText(
                    text = "Qty: ${item.quantity.toInt()}",
                    style = WooPosTypography.BodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value.toAdaptivePadding()))

                WooPosText(
                    text = "@ ${currencyFormatter.formatCurrency(item.price, currency)}",
                    style = WooPosTypography.BodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(start = WooPosSpacing.Large.value.toAdaptivePadding())
        ) {
            WooPosText(
                text = currencyFormatter.formatCurrency(item.total, currency),
                style = WooPosTypography.BodyLarge,
                fontWeight = FontWeight.Bold,
                color = WooPosTheme.colors.onSurfaceVariantHighest
            )

            if (item.totalTax > BigDecimal.ZERO) {
                WooPosText(
                    text = "+ ${currencyFormatter.formatCurrency(item.totalTax, currency)} tax",
                    style = WooPosTypography.BodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = WooPosSpacing.XSmall.value.toAdaptivePadding())
                )
            }
        }
    }
}

@Composable
private fun OrderDetailsButtonsRow(
    onRefundClick: () -> Unit,
    onReceiptClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value.toAdaptivePadding())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value.toAdaptivePadding())
        ) {
            WooPosButton(
                onClick = onRefundClick,
                text = stringResource(R.string.woopos_order_details_refund_button),
                modifier = Modifier.weight(1f)
            )

            WooPosButton(
                onClick = onReceiptClick,
                text = stringResource(R.string.woopos_order_details_receipt_button),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return formatter.format(date)
}

private fun formatCompactDate(date: Date): String {
    val formatter = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return formatter.format(date)
}

@Composable
private fun PaymentMethodChip(order: Order, isLarge: Boolean = false) {
    val isCashPayment = order.isCashPayment
    val icon = if (isCashPayment) Icons.Default.Payments else Icons.Default.CreditCard
    val text = if (isCashPayment) "Cash" else "Card"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                shape = RoundedCornerShape(WooPosCornerRadius.Small.value)
            )
            .padding(
                horizontal = if (isLarge) WooPosSpacing.Medium.value.toAdaptivePadding() 
                           else WooPosSpacing.XSmall.value.toAdaptivePadding(),
                vertical = if (isLarge) WooPosSpacing.Small.value.toAdaptivePadding()
                          else WooPosSpacing.XSmall.value.toAdaptivePadding()
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(if (isLarge) 20.dp else 12.dp)
        )

        Spacer(modifier = Modifier.width(
            if (isLarge) WooPosSpacing.Small.value.toAdaptivePadding()
            else WooPosSpacing.XSmall.value.toAdaptivePadding()
        ))

        WooPosText(
            text = text,
            style = if (isLarge) WooPosTypography.BodyMedium else WooPosTypography.Caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun OrderStatusChip(status: Order.Status, isLarge: Boolean = false) {
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
                horizontal = if (isLarge) WooPosSpacing.Medium.value.toAdaptivePadding()
                           else WooPosSpacing.XSmall.value.toAdaptivePadding(),
                vertical = if (isLarge) WooPosSpacing.Small.value.toAdaptivePadding()
                          else WooPosSpacing.XSmall.value.toAdaptivePadding()
            )
    ) {
        WooPosText(
            text = status.value.replaceFirstChar { it.uppercase() },
            style = if (isLarge) WooPosTypography.BodyMedium else WooPosTypography.Caption,
            fontWeight = FontWeight.Medium,
            color = textColor,
            maxLines = 1
        )
    }
}

sealed class RefundDialogState {
    object OrderDetails : RefundDialogState()
    object RefundAmount : RefundDialogState()
    object RefundMethod : RefundDialogState()
    object RefundConfirmation : RefundDialogState()
    object RefundSuccess : RefundDialogState()
}

@Composable
private fun RefundAmountContent(
    order: Order,
    currencyFormatter: com.woocommerce.android.util.CurrencyFormatter,
    refundAmount: BigDecimal,
    onAmountChange: (BigDecimal) -> Unit,
    onContinue: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WooPosSpacing.Large.value.toAdaptivePadding()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WooPosText(
                text = stringResource(R.string.woopos_refund_amount_description),
                style = WooPosTypography.BodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = WooPosSpacing.Large.value.toAdaptivePadding())
            )

            // Original amount display
            WooPosText(
                text = currencyFormatter.formatCurrency(order.total, order.currency),
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Refund amount input centered
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WooPosInputField(
                value = if (refundAmount == BigDecimal.ZERO) "" else refundAmount.toPlainString(),
                onValueChange = { value ->
                    val amount = value.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    if (amount <= order.total) {
                        onAmountChange(amount)
                    }
                },
                label = stringResource(R.string.woopos_refund_amount_label),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = WooPosTypography.Heading,
                textColor = MaterialTheme.colorScheme.onSurface,
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .padding(horizontal = WooPosSpacing.Medium.value.toAdaptivePadding())
            )

            if (refundAmount > BigDecimal.ZERO && refundAmount < order.total) {
                Spacer(modifier = Modifier.height(WooPosSpacing.Small.value.toAdaptivePadding()))
                WooPosText(
                    text = stringResource(R.string.woopos_refund_partial_refund_note),
                    style = WooPosTypography.BodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = WooPosSpacing.Medium.value.toAdaptivePadding())
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Continue button at bottom
        WooPosButton(
            onClick = onContinue,
            text = stringResource(R.string.woopos_continue),
            state = if (refundAmount > BigDecimal.ZERO) WooPosButtonState.ENABLED else WooPosButtonState.DISABLED,
            modifier = Modifier
                .fillMaxWidth()
                .padding(WooPosSpacing.Medium.value.toAdaptivePadding())
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value.toAdaptivePadding()))
    }
}

@Composable
private fun RefundMethodContent(
    refundMethod: RefundMethod,
    onMethodSelected: (RefundMethod) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WooPosSpacing.Large.value.toAdaptivePadding()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WooPosText(
                text = stringResource(R.string.woopos_refund_method_description),
                style = WooPosTypography.BodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Method selection cards
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WooPosSpacing.Large.value.toAdaptivePadding()),
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Large.value.toAdaptivePadding())
        ) {
            RefundMethodCard(
                title = "Cash",
                description = "Issue cash refund",
                isSelected = refundMethod == RefundMethod.CASH,
                onClick = { onMethodSelected(RefundMethod.CASH) }
            )

            RefundMethodCard(
                title = "Card",
                description = "Process card refund",
                isSelected = refundMethod == RefundMethod.CARD,
                onClick = { onMethodSelected(RefundMethod.CARD) }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Continue button at bottom
        WooPosButton(
            onClick = onContinue,
            text = stringResource(R.string.woopos_continue),
            modifier = Modifier
                .fillMaxWidth()
                .padding(WooPosSpacing.Medium.value.toAdaptivePadding())
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value.toAdaptivePadding()))
    }
}

@Composable
private fun RefundConfirmationContent(
    order: Order,
    currencyFormatter: com.woocommerce.android.util.CurrencyFormatter,
    refundAmount: BigDecimal,
    refundReason: String,
    refundMethod: RefundMethod,
    onReasonChange: (String) -> Unit,
    isProcessing: Boolean,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WooPosSpacing.Large.value.toAdaptivePadding())
        ) {
            WooPosText(
                text = stringResource(R.string.woopos_refund_confirmation_title),
                style = WooPosTypography.BodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = WooPosSpacing.Large.value.toAdaptivePadding())
            )

            // Refund summary card
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
                RefundSummaryRow(
                    label = stringResource(R.string.woopos_refund_amount_label),
                    value = currencyFormatter.formatCurrency(refundAmount, order.currency),
                    isLarge = true
                )

                RefundSummaryRow(
                    label = stringResource(R.string.woopos_refund_method_label),
                    value = when (refundMethod) {
                        RefundMethod.CASH -> stringResource(R.string.woopos_refund_method_cash)
                        RefundMethod.CARD -> stringResource(R.string.woopos_refund_method_card)
                    },
                    isLarge = true
                )

                RefundSummaryRow(
                    label = stringResource(R.string.woopos_order_number_label),
                    value = order.number,
                    isLarge = true
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Reason input
        WooPosInputField(
            value = refundReason,
            onValueChange = onReasonChange,
            label = stringResource(R.string.woopos_refund_reason_label),
            textColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WooPosSpacing.Medium.value.toAdaptivePadding())
        )

        Spacer(modifier = Modifier.weight(1f))

        // Confirm button at bottom
        WooPosButton(
            onClick = onConfirm,
            text = stringResource(R.string.woopos_refund_confirm),
            state = if (isProcessing) WooPosButtonState.LOADING else WooPosButtonState.ENABLED,
            modifier = Modifier
                .fillMaxWidth()
                .padding(WooPosSpacing.Medium.value.toAdaptivePadding())
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value.toAdaptivePadding()))
    }
}

@Composable
private fun RefundSummaryRow(
    label: String,
    value: String,
    isLarge: Boolean = false
) {
    Column {
        WooPosText(
            text = label,
            style = if (isLarge) WooPosTypography.BodyMedium else WooPosTypography.BodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value.toAdaptivePadding()))
        
        WooPosText(
            text = value,
            style = if (isLarge) WooPosTypography.BodyLarge else WooPosTypography.BodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun RefundSuccessContent(
    refundAmount: BigDecimal,
    currencyFormatter: com.woocommerce.android.util.CurrencyFormatter,
    currencyCode: String,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Success content centered
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = WooPosTheme.colors.success
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value.toAdaptivePadding()))

            WooPosText(
                text = stringResource(R.string.woopos_refund_success_title),
                style = WooPosTypography.Heading,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = WooPosSpacing.Large.value.toAdaptivePadding())
            )

            WooPosText(
                text = currencyFormatter.formatCurrency(refundAmount, currencyCode),
                style = WooPosTypography.Heading,
                fontWeight = FontWeight.Bold,
                color = WooPosTheme.colors.success,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = WooPosSpacing.Medium.value.toAdaptivePadding())
            )

            WooPosText(
                text = stringResource(R.string.woopos_refund_success_message),
                style = WooPosTypography.BodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = WooPosSpacing.Large.value.toAdaptivePadding())
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Done button at bottom
        WooPosButton(
            onClick = onClose,
            text = stringResource(R.string.done),
            modifier = Modifier
                .fillMaxWidth()
                .padding(WooPosSpacing.Medium.value.toAdaptivePadding())
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value.toAdaptivePadding()))
    }
}

@Composable
private fun RefundMethodCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                shape = RoundedCornerShape(WooPosCornerRadius.Medium.value)
            )
            .clickable { onClick() }
            .padding(WooPosSpacing.XLarge.value.toAdaptivePadding()),
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value.toAdaptivePadding())
    ) {
        WooPosText(
            text = title,
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
        WooPosText(
            text = description,
            style = WooPosTypography.BodyLarge,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
@WooPosPreview
fun WooPosOrderDetailsScreenPreview() {
    WooPosTheme {
        WooPosOrderDetailsScreen(
            orderId = 123L,
            onNavigationEvent = {}
        )
    }
}
