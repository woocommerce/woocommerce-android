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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WooPosOrderDetailsDialog(
    isVisible: Boolean,
    order: Order?,
    onDismissRequest: () -> Unit,
    onReceiptClick: (Order) -> Unit = {},
    viewModel: WooPosOrdersViewModel = hiltViewModel()
) {
    var refundState: RefundDialogState by remember { mutableStateOf(RefundDialogState.OrderDetails) }
    var refundAmount by remember { mutableStateOf(BigDecimal.ZERO) }
    var refundReason by remember { mutableStateOf("") }
    var refundMethod by remember { mutableStateOf(RefundMethod.CASH) }
    var isProcessingRefund by remember { mutableStateOf(false) }

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            refundState = RefundDialogState.OrderDetails
            refundAmount = BigDecimal.ZERO
            refundReason = ""
            refundMethod = RefundMethod.CASH
            isProcessingRefund = false
        }
    }

    WooPosDialogWrapper(
        isVisible = isVisible,
        onDismissRequest = {
            if (refundState == RefundDialogState.OrderDetails || !isProcessingRefund) {
                onDismissRequest()
            }
        },
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
                        onClick = {
                            if (refundState == RefundDialogState.OrderDetails || !isProcessingRefund) {
                                onDismissRequest()
                            }
                        },
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
                    label = "refund_state_transition"
                ) { state ->
                    when (state) {
                        RefundDialogState.OrderDetails -> {
                            OrderDetailsContent(
                                order = orderData,
                                currencyFormatter = viewModel.currencyFormatter,
                                onRefundClick = {
                                    refundState = RefundDialogState.RefundAmount
                                },
                                onReceiptClick = { onReceiptClick(orderData) }
                            )
                        }
                        RefundDialogState.RefundAmount -> {
                            RefundAmountContent(
                                order = orderData,
                                currencyFormatter = viewModel.currencyFormatter,
                                refundAmount = refundAmount,
                                onAmountChange = { refundAmount = it },
                                onContinue = {
                                    refundState = RefundDialogState.RefundMethod
                                },
                                onCancel = {
                                    refundState = RefundDialogState.OrderDetails
                                    refundAmount = BigDecimal.ZERO
                                }
                            )
                        }
                        RefundDialogState.RefundMethod -> {
                            RefundMethodContent(
                                refundMethod = refundMethod,
                                onMethodSelected = { refundMethod = it },
                                onContinue = {
                                    refundState = RefundDialogState.RefundConfirmation
                                },
                                onBack = {
                                    refundState = RefundDialogState.RefundAmount
                                }
                            )
                        }
                        RefundDialogState.RefundConfirmation -> {
                            RefundConfirmationContent(
                                order = orderData,
                                currencyFormatter = viewModel.currencyFormatter,
                                refundAmount = refundAmount,
                                refundReason = refundReason,
                                refundMethod = refundMethod,
                                onReasonChange = { refundReason = it },
                                isProcessing = isProcessingRefund,
                                onConfirm = {
                                    isProcessingRefund = true
                                    viewModel.processRefund(
                                        order = orderData,
                                        amount = refundAmount,
                                        reason = refundReason,
                                        method = refundMethod
                                    ) { success ->
                                        isProcessingRefund = false
                                        if (success) {
                                            refundState = RefundDialogState.RefundSuccess
                                        }
                                    }
                                },
                                onBack = {
                                    refundState = RefundDialogState.RefundMethod
                                }
                            )
                        }
                        RefundDialogState.RefundSuccess -> {
                            RefundSuccessContent(
                                refundAmount = refundAmount,
                                currencyFormatter = viewModel.currencyFormatter,
                                currencyCode = orderData.currency,
                                onClose = onDismissRequest
                            )
                        }
                    }
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
    onContinue: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_refund_amount_title),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = WooPosSpacing.Medium.value.toAdaptivePadding())
        )

        WooPosText(
            text = stringResource(R.string.woopos_refund_amount_description),
            style = WooPosTypography.BodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = WooPosSpacing.XLarge.value.toAdaptivePadding())
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(WooPosCornerRadius.Medium.value)
                )
                .padding(WooPosSpacing.Large.value.toAdaptivePadding())
        ) {
            WooPosText(
                text = stringResource(R.string.woopos_refund_original_amount),
                style = WooPosTypography.BodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = WooPosSpacing.XSmall.value.toAdaptivePadding())
            )
            WooPosText(
                text = currencyFormatter.formatCurrency(order.total, order.currency),
                style = WooPosTypography.BodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = WooPosSpacing.Medium.value.toAdaptivePadding())
            )

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
                textColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )

            if (refundAmount > BigDecimal.ZERO && refundAmount < order.total) {
                Spacer(modifier = Modifier.height(WooPosSpacing.Small.value.toAdaptivePadding()))
                WooPosText(
                    text = stringResource(R.string.woopos_refund_partial_refund_note),
                    style = WooPosTypography.BodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value.toAdaptivePadding()))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value.toAdaptivePadding())
        ) {
            WooPosOutlinedButton(
                onClick = onCancel,
                text = stringResource(R.string.cancel),
                modifier = Modifier.weight(1f)
            )

            WooPosButton(
                onClick = onContinue,
                text = stringResource(R.string.woopos_continue),
                modifier = Modifier.weight(1f),
                state = if (refundAmount > BigDecimal.ZERO) WooPosButtonState.ENABLED else WooPosButtonState.DISABLED
            )
        }
    }
}

@Composable
private fun RefundMethodContent(
    refundMethod: RefundMethod,
    onMethodSelected: (RefundMethod) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_refund_method_title),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = WooPosSpacing.Medium.value.toAdaptivePadding())
        )

        WooPosText(
            text = stringResource(R.string.woopos_refund_method_description),
            style = WooPosTypography.BodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = WooPosSpacing.XLarge.value.toAdaptivePadding())
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value.toAdaptivePadding())
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

        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value.toAdaptivePadding()))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value.toAdaptivePadding())
        ) {
            WooPosOutlinedButton(
                onClick = onBack,
                text = stringResource(R.string.back),
                modifier = Modifier.weight(1f)
            )

            WooPosButton(
                onClick = onContinue,
                text = stringResource(R.string.woopos_continue),
                modifier = Modifier.weight(1f)
            )
        }
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
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_refund_confirmation_title),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = WooPosSpacing.Medium.value.toAdaptivePadding())
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(WooPosCornerRadius.Medium.value)
                )
                .padding(WooPosSpacing.Large.value.toAdaptivePadding()),
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value.toAdaptivePadding())
        ) {
            RefundSummaryRow(
                label = stringResource(R.string.woopos_refund_amount_label),
                value = currencyFormatter.formatCurrency(refundAmount, order.currency)
            )

            RefundSummaryRow(
                label = stringResource(R.string.woopos_refund_method_label),
                value = when (refundMethod) {
                    RefundMethod.CASH -> stringResource(R.string.woopos_refund_method_cash)
                    RefundMethod.CARD -> stringResource(R.string.woopos_refund_method_card)
                }
            )

            RefundSummaryRow(
                label = stringResource(R.string.woopos_order_number_label),
                value = order.number
            )
        }

        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value.toAdaptivePadding()))

        WooPosInputField(
            value = refundReason,
            onValueChange = onReasonChange,
            label = stringResource(R.string.woopos_refund_reason_label),
            textColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value.toAdaptivePadding()))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value.toAdaptivePadding())
        ) {
            WooPosOutlinedButton(
                onClick = onBack,
                text = stringResource(R.string.back),
                modifier = Modifier.weight(1f),
                state = if (isProcessing) WooPosButtonState.DISABLED else WooPosButtonState.ENABLED
            )

            WooPosButton(
                onClick = onConfirm,
                text = stringResource(R.string.woopos_refund_confirm),
                modifier = Modifier.weight(1f),
                state = if (isProcessing) WooPosButtonState.LOADING else WooPosButtonState.ENABLED
            )
        }
    }
}

@Composable
private fun RefundSummaryRow(
    label: String,
    value: String
) {
    Column {
        WooPosText(
            text = label,
            style = WooPosTypography.BodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        WooPosText(
            text = value,
            style = WooPosTypography.BodyMedium,
            fontWeight = FontWeight.Medium
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
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = WooPosTheme.colors.success
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value.toAdaptivePadding()))

        WooPosText(
            text = stringResource(R.string.woopos_refund_success_title),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = WooPosSpacing.Medium.value.toAdaptivePadding())
        )

        WooPosText(
            text = stringResource(
                R.string.woopos_refund_success_message,
                currencyFormatter.formatCurrency(refundAmount, currencyCode)
            ),
            style = WooPosTypography.BodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = WooPosSpacing.XLarge.value.toAdaptivePadding())
        )

        WooPosButton(
            onClick = onClose,
            text = stringResource(R.string.done),
            modifier = Modifier.fillMaxWidth()
        )
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
            .padding(WooPosSpacing.Large.value.toAdaptivePadding())
    ) {
        WooPosText(
            text = title,
            style = WooPosTypography.BodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
        WooPosText(
            text = description,
            style = WooPosTypography.BodyMedium,
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
fun WooPosOrderDetailsDialogPreview() {
    WooPosTheme {
        WooPosOrderDetailsDialog(
            isVisible = true,
            order = null,
            onDismissRequest = {}
        )
    }
}
