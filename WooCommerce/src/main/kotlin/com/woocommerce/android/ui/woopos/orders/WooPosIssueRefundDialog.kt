package com.woocommerce.android.ui.woopos.orders

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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

private val COLUMN_WIDTH = 104.dp

@Composable
fun WooPosIssueRefundDialog(
    isVisible: Boolean,
    lineItems: List<OrderDetailsViewState.Computed.Details.LineItemRow>,
    itemsSelectedLabel: String,
    onDismissRequest: () -> Unit,
    onContinue: () -> Unit
) {
    val selectAllContentDescription = stringResource(R.string.order_refunds_items_select_all)

    WooPosDialogWrapper(
        isVisible = isVisible,
        dialogBackgroundContentDescription = stringResource(
            R.string.woopos_orders_issue_refund_content_description
        ),
        onDismissRequest = onDismissRequest
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = WooPosSpacing.XLarge.value)
                    .padding(bottom = WooPosSpacing.Medium.value),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Large.value)
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
                            checkmarkColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    WooPosText(
                        text = itemsSelectedLabel,
                        style = WooPosTypography.Caption,
                        fontWeight = FontWeight.Bold,
                        color = WooPosTheme.colors.onSurfaceVariantHighest
                    )
                }
                WooPosText(
                    modifier = Modifier.width(COLUMN_WIDTH),
                    text = stringResource(R.string.woopos_orders_amount),
                    style = WooPosTypography.Caption,
                    fontWeight = FontWeight.Bold,
                    color = WooPosTheme.colors.onSurfaceVariantHighest,
                    textAlign = TextAlign.End,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))
                WooPosText(
                    modifier = Modifier.width(COLUMN_WIDTH),
                    text = stringResource(R.string.woopos_orders_tax),
                    style = WooPosTypography.Caption,
                    fontWeight = FontWeight.Bold,
                    color = WooPosTheme.colors.onSurfaceVariantHighest,
                    textAlign = TextAlign.End,
                    overflow = TextOverflow.Ellipsis
                )
            }

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
                itemsIndexed(lineItems) { index, item ->
                    LineItemRow(item = item)
                    if (index < lineItems.lastIndex) {
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
}

@Composable
private fun LineItemRow(
    item: OrderDetailsViewState.Computed.Details.LineItemRow
) {
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
                checkmarkColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        Spacer(modifier = Modifier.size(WooPosSpacing.Large.value))

        WooPosItemImage(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(WooPosCornerRadius.Small.value)),
            imageUrl = item.imageUrl,
            placeholderIcon = Icons.Outlined.Inventory2,
            placeholderIconSize = 24.dp
        )
        Spacer(modifier = Modifier.size(WooPosSpacing.Medium.value))

        WooPosText(
            text = item.name,
            style = WooPosTypography.BodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        WooPosText(
            text = item.lineTotal,
            style = WooPosTypography.BodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.width(COLUMN_WIDTH),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.size(WooPosSpacing.Medium.value))
        WooPosText(
            text = "-",
            style = WooPosTypography.BodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.width(COLUMN_WIDTH),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@WooPosPreview
@Composable
fun WooPosIssueRefundDialogPreview() {
    val sampleLineItems = listOf(
        OrderDetailsViewState.Computed.Details.LineItemRow(
            id = 1,
            name = "Cup",
            lineTotal = "$999999.00",
            imageUrl = null
        ),
        OrderDetailsViewState.Computed.Details.LineItemRow(
            id = 2,
            name = "Coffee Storage Container",
            lineTotal = "$30.00",
            imageUrl = null
        ),
        OrderDetailsViewState.Computed.Details.LineItemRow(
            id = 3,
            name = "Enamel Mug",
            lineTotal = "$8.50",
            imageUrl = null
        )
    )

    WooPosTheme {
        WooPosIssueRefundDialog(
            isVisible = true,
            lineItems = sampleLineItems,
            itemsSelectedLabel = "ITEMS (${sampleLineItems.size} SELECTED)",
            onDismissRequest = {},
            onContinue = {}
        )
    }
}
