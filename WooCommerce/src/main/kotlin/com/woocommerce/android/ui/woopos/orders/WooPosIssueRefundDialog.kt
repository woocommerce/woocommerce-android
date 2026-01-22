package com.woocommerce.android.ui.woopos.orders

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosDialogWrapper
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosItemImage
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import java.math.BigDecimal

@Composable
fun WooPosIssueRefundDialog(
    orderId: Long,
    onDismissRequest: () -> Unit,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
    refundReasonUpdate: String? = null
) {
    val viewModel: WooPosRefundViewModel =
        hiltViewModel<WooPosRefundViewModel, WooPosRefundViewModel.Factory>(key = "refund_$orderId") { factory ->
            factory.create(orderId)
        }

    refundReasonUpdate?.let { reason ->
        LaunchedEffect(reason) {
            viewModel.onUIEvent(WooPosRefundUIEvent.OnRefundReasonChanged(reason))
        }
    }

    val handleDismiss = {
        if (viewModel.onDismissRequest()) {
            viewModel.onUIEvent(WooPosRefundUIEvent.DialogDismissed)
            onDismissRequest()
        }
    }

    BackHandler {
        handleDismiss()
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    WooPosDialogWrapper(
        isVisible = true,
        dialogBackgroundContentDescription = stringResource(
            R.string.woopos_orders_issue_refund_content_description
        ),
        onDismissRequest = handleDismiss
    ) {
        when (val currentState = state) {
            is WooPosRefundState.Loading -> LoadingContent()
            is WooPosRefundState.Content -> ContentStateHandler(
                state = currentState,
                orderId = orderId,
                viewModel = viewModel,
                onDismissRequest = handleDismiss,
                onNavigationEvent = onNavigationEvent
            )
            is WooPosRefundState.Error -> ErrorContent(currentState.message, handleDismiss)
            is WooPosRefundState.NoRefundableItems -> NoItemsContent(handleDismiss)
            is WooPosRefundState.RefundSuccess -> RefundSuccessContent(currentState, handleDismiss)
            is WooPosRefundState.RefundError -> ErrorContent(currentState.message, handleDismiss)
        }
    }
}

@Composable
private fun ContentStateHandler(
    state: WooPosRefundState.Content,
    orderId: Long,
    viewModel: WooPosRefundViewModel,
    onDismissRequest: () -> Unit,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    when (state.step) {
        WooPosRefundState.Content.RefundStep.SelectItems -> {
            SelectItemsContent(
                state = state,
                onDismissRequest = onDismissRequest,
                onContinue = {
                    viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToReviewClicked)
                }
            )
        }

        WooPosRefundState.Content.RefundStep.ReviewRefund -> {
            ReviewRefundContent(
                state = state,
                onDismissRequest = onDismissRequest,
                onContinue = {
                    viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToConfirmRefundClicked)
                },
                onEditRefund = {
                    viewModel.onUIEvent(WooPosRefundUIEvent.BackToSelectItemsClicked)
                },
                onEditReason = {
                    onNavigationEvent(
                        WooPosNavigationEvent.OpenRefundReason(
                            orderId = orderId,
                            initialReason = state.refundReason
                        )
                    )
                }
            )
        }

        WooPosRefundState.Content.RefundStep.ConfirmRefund -> {
            ConfirmRefundContent(
                state = state,
                isProcessing = false,
                onDismissRequest = onDismissRequest,
                onConfirm = {
                    viewModel.onUIEvent(WooPosRefundUIEvent.OnRefundConfirmed)
                },
                onBack = {
                    viewModel.onUIEvent(WooPosRefundUIEvent.BackToReviewClicked)
                }
            )
        }
        WooPosRefundState.Content.RefundStep.Processing -> {
            ConfirmRefundContent(
                state = state,
                isProcessing = true,
                onDismissRequest = {},
                onConfirm = {},
                onBack = {}
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    val loadingDescription = stringResource(R.string.woopos_orders_loading_refund_items)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(WooPosSpacing.XLarge.value),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.semantics {
                contentDescription = loadingDescription
            }
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onDismissRequest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WooPosSpacing.XLarge.value),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WooPosText(
            text = message,
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.size(WooPosSpacing.Large.value))
        WooPosButton(
            text = stringResource(R.string.close),
            onClick = onDismissRequest
        )
    }
}

@Composable
private fun NoItemsContent(onDismissRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WooPosSpacing.XLarge.value),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_orders_no_items_available_for_refund),
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.size(WooPosSpacing.Large.value))
        WooPosButton(
            text = stringResource(R.string.close),
            onClick = onDismissRequest
        )
    }
}

@Composable
private fun RefundSuccessContent(
    state: WooPosRefundState.RefundSuccess,
    onDismissRequest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WooPosSpacing.XLarge.value),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WooPosText(
            text = stringResource(R.string.order_refunds_amount_refund_successful),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.size(WooPosSpacing.Medium.value))
        WooPosText(
            text = "${state.refundedAmount} refunded for ${state.orderNumber}",
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.size(WooPosSpacing.Large.value))
        WooPosButton(
            text = stringResource(R.string.close),
            onClick = onDismissRequest
        )
    }
}

@Composable
private fun SelectItemsContent(
    state: WooPosRefundState.Content,
    onDismissRequest: () -> Unit,
    onContinue: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        RefundDialogHeader(onDismissRequest = onDismissRequest)

        ItemsHeaderRow(itemsCount = state.itemsCount)

        Divider(modifier = Modifier.padding(horizontal = WooPosSpacing.XLarge.value))

        LazyColumn(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth()
                .padding(horizontal = WooPosSpacing.XLarge.value)
                .padding(vertical = WooPosSpacing.Medium.value),
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)
        ) {
            itemsIndexed(state.refundableItems) { index, item ->
                RefundableItemRow(item = item)
                if (index < state.refundableItems.lastIndex) {
                    Divider()
                }
            }
        }

        Divider(modifier = Modifier.padding(horizontal = WooPosSpacing.XLarge.value))

        WooPosButton(
            text = stringResource(R.string.continue_button),
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(WooPosSpacing.XLarge.value)
        )
    }
}

@Composable
private fun RefundDialogHeader(onDismissRequest: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WooPosSpacing.XLarge.value),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_orders_select_items_to_refund),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(
            modifier = Modifier.size(48.dp),
            onClick = onDismissRequest,
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = ImageVector.vectorResource(R.drawable.ic_close_24dp),
                contentDescription = stringResource(R.string.close),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ItemsHeaderRow(itemsCount: Int) {
    val selectAllContentDescription = stringResource(R.string.order_refunds_items_select_all)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WooPosSpacing.XLarge.value)
            .padding(bottom = WooPosSpacing.Medium.value),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = true,
            onCheckedChange = null,
            modifier = Modifier
                .size(32.dp)
                .semantics {
                    contentDescription = selectAllContentDescription
                },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                disabledCheckedColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.width(WooPosSpacing.Large.value))
        Row {
            WooPosText(
                text = stringResource(R.string.woopos_orders_select_all_items),
                style = WooPosTypography.Caption,
                fontWeight = FontWeight.Bold,
                color = WooPosTheme.colors.onSurfaceVariantHighest
            )
            Spacer(modifier = Modifier.width(WooPosSpacing.XSmall.value))
            WooPosText(
                text = stringResource(R.string.woopos_orders_items_selected_count, itemsCount),
                style = WooPosTypography.Caption,
                fontWeight = FontWeight.Normal,
                color = WooPosTheme.colors.onSurfaceVariantLowest
            )
        }
    }
}

@Composable
private fun RefundableItemRow(item: WooPosRefundableItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = WooPosSpacing.XSmall.value),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = true,
            onCheckedChange = null,
            modifier = Modifier
                .size(32.dp)
                .semantics {
                    contentDescription = item.name
                },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                disabledCheckedColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.size(WooPosSpacing.Large.value))

        WooPosItemImage(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(WooPosCornerRadius.Small.value)),
            imageUrl = null,
            placeholderIcon = ImageVector.vectorResource(R.drawable.ic_inventory_2_24dp),
            placeholderIconSize = 24.dp
        )
        Spacer(modifier = Modifier.size(WooPosSpacing.Medium.value))

        Column(
            modifier = Modifier.weight(1f),
        ) {
            WooPosText(
                text = item.name,
                style = WooPosTypography.BodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            WooPosText(
                text = item.formattedUnitPrice,
                style = WooPosTypography.BodyMedium,
                color = WooPosTheme.colors.onSurfaceVariantHighest
            )
        }
    }
}

@Composable
private fun ReviewRefundContent(
    state: WooPosRefundState.Content,
    onDismissRequest: () -> Unit,
    onContinue: () -> Unit,
    onEditRefund: () -> Unit,
    onEditReason: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ReviewRefundHeader(onDismissRequest = onDismissRequest)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WooPosSpacing.XLarge.value),
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)
        ) {
            ReviewSummaryRow(
                label = pluralStringResource(
                    R.plurals.woopos_orders_items_subtotal_count_plural,
                    state.itemsCount,
                    state.itemsCount
                ),
                value = state.formattedSubtotal,
                isTotal = false
            )
            ReviewSummaryRow(
                label = stringResource(R.string.taxes),
                value = state.formattedTaxes,
                isTotal = false
            )

            Divider()

            Column(
                verticalArrangement = Arrangement.spacedBy(WooPosSpacing.XSmall.value)
            ) {
                ReviewSummaryRow(
                    label = stringResource(R.string.woopos_orders_refund_total),
                    value = state.formattedTotal,
                    isTotal = true
                )
                WooPosText(
                    text = "TEST: Via payment card ••••1456",
                    style = WooPosTypography.BodyMedium,
                    fontWeight = FontWeight.Normal,
                    color = WooPosTheme.colors.onSurfaceVariantHighest
                )
            }

            Divider()

            val editReasonText = stringResource(R.string.woopos_orders_edit_reason)
            Column(
                verticalArrangement = Arrangement.spacedBy(WooPosSpacing.XSmall.value),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
                    .clickable(onClick = onEditReason)
                    .semantics {
                        contentDescription = editReasonText
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WooPosText(
                        text = stringResource(R.string.woopos_orders_refund_reason),
                        style = WooPosTypography.BodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    WooPosText(
                        text = editReasonText,
                        style = WooPosTypography.BodyMedium,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (state.refundReason.isNotBlank()) {
                    WooPosText(
                        text = state.refundReason,
                        style = WooPosTypography.BodyMedium,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = WooPosTheme.colors.onSurfaceVariantHighest
                    )
                }
            }
        }

        ReviewActionButtons(
            onContinue = onContinue,
            onEditRefund = onEditRefund
        )
    }
}

@Composable
private fun ReviewRefundHeader(onDismissRequest: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WooPosSpacing.XLarge.value),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_orders_review_refund),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(
            modifier = Modifier.size(48.dp),
            onClick = onDismissRequest,
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = ImageVector.vectorResource(R.drawable.ic_close_24dp),
                contentDescription = stringResource(R.string.close),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ReviewSummaryRow(
    label: String,
    value: String,
    isTotal: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WooPosText(
            text = label,
            style = if (isTotal) WooPosTypography.BodyLarge else WooPosTypography.BodyMedium,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
        WooPosText(
            text = value,
            style = if (isTotal) WooPosTypography.BodyLarge else WooPosTypography.BodyMedium,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ReviewActionButtons(
    onContinue: () -> Unit,
    onEditRefund: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WooPosSpacing.XLarge.value),
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)
    ) {
        WooPosButton(
            text = stringResource(R.string.continue_button),
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        )
        WooPosOutlinedButton(
            text = stringResource(R.string.woopos_orders_edit_refund),
            onClick = onEditRefund,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun Divider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        color = WooPosTheme.colors.outlineVariant,
        thickness = 0.25.dp
    )
}

@Composable
private fun ConfirmRefundContent(
    state: WooPosRefundState.Content,
    isProcessing: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ConfirmRefundHeader(
            title = stringResource(R.string.woopos_orders_confirm_refund_title, state.formattedTotal),
            onDismissRequest = onDismissRequest
        )

        ConfirmRefundMessage(
            message = stringResource(
                R.string.woopos_orders_confirm_refund_message,
                state.formattedTotal,
                state.paymentMethod
            )
        )

        ConfirmRefundButtons(
            isProcessing = isProcessing,
            onConfirm = onConfirm,
            onBack = onBack
        )
    }
}

@Composable
private fun ConfirmRefundHeader(
    title: String,
    onDismissRequest: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WooPosSpacing.XLarge.value),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WooPosText(
            text = title,
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(
            modifier = Modifier.size(48.dp),
            onClick = onDismissRequest,
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = ImageVector.vectorResource(R.drawable.ic_close_24dp),
                contentDescription = stringResource(R.string.close),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ConfirmRefundMessage(message: String) {
    WooPosText(
        text = message,
        style = WooPosTypography.BodyLarge,
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = WooPosSpacing.XLarge.value)
            .padding(bottom = WooPosSpacing.XLarge.value)
    )
}

@Composable
private fun ConfirmRefundButtons(
    isProcessing: Boolean,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WooPosSpacing.XLarge.value),
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)
    ) {
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = WooPosSpacing.Medium.value),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp)
                )
            }
        } else {
            WooPosButton(
                text = stringResource(R.string.woopos_orders_yes_proceed),
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth()
            )
            WooPosOutlinedButton(
                text = stringResource(R.string.back),
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@WooPosPreview
@Composable
fun SelectItemsContentPreview() {
    val sampleItems = listOf(
        WooPosRefundableItem(
            orderItemId = 1,
            productId = 100,
            variationId = 0,
            name = "Cup",
            unitPrice = BigDecimal("18.00"),
            unitTax = BigDecimal("1.80"),
            formattedUnitPrice = "$18.00",
            formattedUnitTax = "$1.80",
            rowIndex = 0
        ),
        WooPosRefundableItem(
            orderItemId = 2,
            productId = 200,
            variationId = 0,
            name = "Coffee Storage Container",
            unitPrice = BigDecimal("30.00"),
            unitTax = BigDecimal("3.00"),
            formattedUnitPrice = "$30.00",
            formattedUnitTax = "$3.00",
            rowIndex = 0
        ),
        WooPosRefundableItem(
            orderItemId = 3,
            productId = 300,
            variationId = 0,
            name = "Enamel Mug",
            unitPrice = BigDecimal("8.50"),
            unitTax = BigDecimal("0.85"),
            formattedUnitPrice = "$8.50",
            formattedUnitTax = "$0.85",
            rowIndex = 0
        )
    )

    val state = WooPosRefundState.Content(
        orderId = 123,
        orderNumber = "#123",
        currency = "USD",
        refundableItems = sampleItems,
        itemsCount = 3,
        subtotal = BigDecimal("57.00"),
        taxes = BigDecimal("5.65"),
        total = BigDecimal("62.65"),
        formattedSubtotal = "$57.00",
        formattedTaxes = "$5.65",
        formattedTotal = "$62.65",
        paymentMethod = "payment card ••••1456",
        step = WooPosRefundState.Content.RefundStep.SelectItems
    )

    WooPosTheme {
        SelectItemsContent(
            state = state,
            onDismissRequest = {},
            onContinue = {}
        )
    }
}

@WooPosPreview
@Composable
fun ReviewRefundContentPreview() {
    val sampleItems = listOf(
        WooPosRefundableItem(
            orderItemId = 1,
            productId = 100,
            variationId = 0,
            name = "Cup",
            unitPrice = BigDecimal("18.00"),
            unitTax = BigDecimal("1.80"),
            formattedUnitPrice = "$18.00",
            formattedUnitTax = "$1.80",
            rowIndex = 0
        ),
        WooPosRefundableItem(
            orderItemId = 2,
            productId = 200,
            variationId = 0,
            name = "Coffee Storage Container",
            unitPrice = BigDecimal("30.00"),
            unitTax = BigDecimal("3.00"),
            formattedUnitPrice = "$30.00",
            formattedUnitTax = "$3.00",
            rowIndex = 0
        ),
        WooPosRefundableItem(
            orderItemId = 3,
            productId = 300,
            variationId = 0,
            name = "Enamel Mug",
            unitPrice = BigDecimal("8.50"),
            unitTax = BigDecimal("0.85"),
            formattedUnitPrice = "$8.50",
            formattedUnitTax = "$0.85",
            rowIndex = 0
        )
    )

    val state = WooPosRefundState.Content(
        orderId = 123,
        orderNumber = "#123",
        currency = "USD",
        refundableItems = sampleItems,
        itemsCount = 3,
        subtotal = BigDecimal("57.00"),
        taxes = BigDecimal("5.65"),
        total = BigDecimal("62.65"),
        formattedSubtotal = "$57.00",
        formattedTaxes = "$5.65",
        formattedTotal = "$62.65",
        paymentMethod = "payment card ••••1456",
        step = WooPosRefundState.Content.RefundStep.ReviewRefund
    )

    WooPosTheme {
        ReviewRefundContent(
            state = state,
            onDismissRequest = {},
            onContinue = {},
            onEditRefund = {},
            onEditReason = {}
        )
    }
}

@WooPosPreview
@Composable
fun ConfirmRefundContentPreview() {
    val sampleItems = listOf(
        WooPosRefundableItem(
            orderItemId = 1,
            productId = 100,
            variationId = 0,
            name = "Cup",
            unitPrice = BigDecimal("18.00"),
            unitTax = BigDecimal("1.80"),
            formattedUnitPrice = "$18.00",
            formattedUnitTax = "$1.80",
            rowIndex = 0
        ),
        WooPosRefundableItem(
            orderItemId = 2,
            productId = 200,
            variationId = 0,
            name = "Coffee Storage Container",
            unitPrice = BigDecimal("30.00"),
            unitTax = BigDecimal("3.00"),
            formattedUnitPrice = "$30.00",
            formattedUnitTax = "$3.00",
            rowIndex = 0
        ),
        WooPosRefundableItem(
            orderItemId = 3,
            productId = 300,
            variationId = 0,
            name = "Enamel Mug",
            unitPrice = BigDecimal("8.50"),
            unitTax = BigDecimal("0.85"),
            formattedUnitPrice = "$8.50",
            formattedUnitTax = "$0.85",
            rowIndex = 0
        )
    )

    val state = WooPosRefundState.Content(
        orderId = 123,
        orderNumber = "#123",
        currency = "USD",
        refundableItems = sampleItems,
        itemsCount = 3,
        subtotal = BigDecimal("57.00"),
        taxes = BigDecimal("5.65"),
        total = BigDecimal("62.65"),
        formattedSubtotal = "$57.00",
        formattedTaxes = "$5.65",
        formattedTotal = "$62.65",
        paymentMethod = "payment card ••••1456",
        step = WooPosRefundState.Content.RefundStep.ConfirmRefund
    )

    WooPosTheme {
        ConfirmRefundContent(
            state = state,
            isProcessing = false,
            onDismissRequest = {},
            onConfirm = {},
            onBack = {}
        )
    }
}
