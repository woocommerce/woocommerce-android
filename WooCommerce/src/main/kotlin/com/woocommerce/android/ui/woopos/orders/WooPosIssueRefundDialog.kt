package com.woocommerce.android.ui.woopos.orders

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
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
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import java.math.BigDecimal

@Composable
fun WooPosIssueRefundDialog(
    orderId: Long,
    onDismissRequest: () -> Unit,
    onContinue: () -> Unit
) {
    val viewModel: WooPosRefundViewModel =
        hiltViewModel<WooPosRefundViewModel, WooPosRefundViewModel.Factory> { factory ->
            factory.create(orderId)
        }
    val state by viewModel.state.collectAsStateWithLifecycle()

    WooPosDialogWrapper(
        isVisible = true,
        dialogBackgroundContentDescription = stringResource(
            R.string.woopos_orders_issue_refund_content_description
        ),
        onDismissRequest = onDismissRequest
    ) {
        when (val currentState = state) {
            is WooPosRefundState.Loading -> {
                LoadingContent()
            }
            is WooPosRefundState.Content -> {
                RefundDialogContent(
                    state = currentState,
                    onDismissRequest = onDismissRequest,
                    onContinue = onContinue
                )
            }
            is WooPosRefundState.Error -> {
                ErrorContent(
                    message = currentState.message,
                    onDismissRequest = onDismissRequest
                )
            }
            is WooPosRefundState.NoRefundableItems -> {
                NoItemsContent(onDismissRequest = onDismissRequest)
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(WooPosSpacing.XLarge.value),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
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
            text = "No items available for refund",
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
private fun RefundDialogContent(
    state: WooPosRefundState.Content,
    onDismissRequest: () -> Unit,
    onContinue: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        RefundDialogHeader(onDismissRequest = onDismissRequest)

        ItemsHeaderRow(itemsLabel = state.itemsLabel)

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = WooPosSpacing.XLarge.value),
            color = WooPosTheme.colors.outlineVariant,
            thickness = 0.25.dp
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = WooPosSpacing.XLarge.value)
                .padding(vertical = WooPosSpacing.Medium.value),
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)
        ) {
            itemsIndexed(state.refundableItems) { index, item ->
                RefundableItemRow(item = item)
                if (index < state.refundableItems.lastIndex) {
                    HorizontalDivider(
                        color = WooPosTheme.colors.outlineVariant,
                        thickness = 0.25.dp
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = WooPosSpacing.XLarge.value),
            color = WooPosTheme.colors.outlineVariant,
            thickness = 0.25.dp
        )

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
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.close),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ItemsHeaderRow(itemsLabel: String) {
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
        WooPosText(
            text = itemsLabel,
            style = WooPosTypography.Caption,
            fontWeight = FontWeight.Bold,
            color = WooPosTheme.colors.onSurfaceVariantHighest
        )
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
            placeholderIcon = Icons.Outlined.Inventory2,
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

@WooPosPreview
@Composable
fun WooPosIssueRefundDialogPreview() {
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
        itemsLabel = "SELECT ALL ITEMS (3 SELECTED)",
        subtotal = BigDecimal("57.00"),
        taxes = BigDecimal("5.65"),
        total = BigDecimal("62.65")
    )

    WooPosTheme {
        RefundDialogContent(
            state = state,
            onDismissRequest = {},
            onContinue = {}
        )
    }
}
