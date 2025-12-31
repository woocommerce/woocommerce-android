package com.woocommerce.android.ui.woopos.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.ShadowType
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonSmall
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosItemImage
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosOrderDetails(
    modifier: Modifier = Modifier,
    details: WooPosOrdersState.OrderDetailsViewState.Computed.Details,
    onUIEvent: (WooPosOrdersUIEvent) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(
                start = WooPosSpacing.Medium.value,
                end = WooPosSpacing.Medium.value,
                bottom = WooPosSpacing.XLarge.value
            )
    ) {
        Row(
            modifier = Modifier.heightIn(min = WOO_POS_ORDERS_TOOLBAR_HEIGHT),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WooPosText(
                text = details.number,
                style = WooPosTypography.Heading,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.weight(1f))

            when {
                details.actions.size > 1 -> {
                    val primaryAction = details.actions.first()
                    val overflowActions = details.actions.drop(1)

                    OrderActionButton(
                        action = primaryAction,
                        onClick = { onUIEvent(WooPosOrdersUIEvent.OrderActionClicked(it)) }
                    )

                    Spacer(Modifier.width(WooPosSpacing.Small.value))

                    OrderDetailsOverflowMenu(
                        actions = overflowActions,
                        onClick = { onUIEvent(WooPosOrdersUIEvent.OrderActionClicked(it)) }
                    )
                }

                details.actions.size == 1 -> {
                    OrderActionButton(
                        action = details.actions.first(),
                        onClick = { onUIEvent(WooPosOrdersUIEvent.OrderActionClicked(it)) }
                    )
                }
            }
        }

        Spacer(Modifier.height(WooPosSpacing.Small.value))

        OrdersHeader(details = details)

        Spacer(Modifier.height(WooPosSpacing.Large.value))

        OrdersProducts(lineItems = details.lineItems)

        Spacer(Modifier.height(WooPosSpacing.Medium.value))

        OrdersTotals(details = details)
    }
}

@Composable
private fun OrdersHeader(details: WooPosOrdersState.OrderDetailsViewState.Computed.Details) {
    Column(modifier = Modifier.fillMaxWidth()) {
        WooPosText(
            text = details.dateTime,
            style = WooPosTypography.BodyMedium,
            color = WooPosTheme.colors.onSurfaceVariantHighest
        )

        details.customerEmail?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(WooPosSpacing.XSmall.value))
            WooPosText(
                text = it,
                style = WooPosTypography.BodyMedium,
                color = WooPosTheme.colors.onSurfaceVariantHighest
            )
        }

        Spacer(Modifier.height(WooPosSpacing.Small.value))

        WooPosOrdersStatusBadge(status = details.status)
    }
}

@Composable
private fun OrdersProducts(lineItems: List<WooPosOrdersState.OrderDetailsViewState.Computed.Details.LineItemRow>) {
    WooPosCard(shadowType = ShadowType.Soft) {
        Column(Modifier.padding(WooPosSpacing.Medium.value)) {
            WooPosText(
                text = stringResource(R.string.woopos_orders_details_products_title),
                style = WooPosTypography.BodyXLarge,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(WooPosSpacing.Medium.value))

            lineItems.forEachIndexed { ind, item ->
                OrderProductItem(row = item)

                if (ind < lineItems.size - 1) {
                    DividerWithSpacing()
                }
            }
        }
    }
}

@Composable
@Suppress("DestructuringDeclarationWithTooManyEntries")
private fun OrderProductItem(row: WooPosOrdersState.OrderDetailsViewState.Computed.Details.LineItemRow) {
    val marginMedium = WooPosSpacing.Medium.value
    val marginSmall = WooPosSpacing.Small.value
    val marginXSmall = WooPosSpacing.XSmall.value
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = marginSmall)
    ) {
        val (image, nameText, qtyText, totalText) = createRefs()

        OrderLineItemImage(
            imageUrl = row.imageUrl,
            modifier = Modifier.constrainAs(image) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
            }
        )

        WooPosText(
            text = row.name,
            style = WooPosTypography.BodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.constrainAs(nameText) {
                top.linkTo(image.top)
                start.linkTo(image.end, margin = marginMedium)
                end.linkTo(totalText.start, margin = marginSmall)
                width = Dimension.fillToConstraints
            }
        )

        WooPosText(
            text = row.qtyAndUnitPrice,
            style = WooPosTypography.BodyMedium,
            color = WooPosTheme.colors.onSurfaceVariantHighest,
            modifier = Modifier.constrainAs(qtyText) {
                top.linkTo(nameText.bottom, margin = marginXSmall)
                start.linkTo(nameText.start)
                end.linkTo(totalText.start, margin = marginSmall)
                width = Dimension.fillToConstraints
            }
        )

        WooPosText(
            text = row.lineTotal,
            style = WooPosTypography.BodyMedium,
            modifier = Modifier.constrainAs(totalText) {
                top.linkTo(nameText.top)
                end.linkTo(parent.end)
            }
        )
    }
}

@Composable
private fun OrdersTotals(details: WooPosOrdersState.OrderDetailsViewState.Computed.Details) {
    WooPosCard(shadowType = ShadowType.Soft) {
        Column(Modifier.padding(WooPosSpacing.Medium.value)) {
            WooPosText(
                text = stringResource(R.string.woopos_orders_details_totals_title),
                style = WooPosTypography.BodyXLarge,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(WooPosSpacing.Small.value))

            val breakdown = details.breakdown
            TotalRowLine(
                label = stringResource(R.string.woopos_orders_details_breakdown_products_label),
                value = breakdown.products,
                boldLabel = false
            )

            Spacer(Modifier.height(WooPosSpacing.Small.value))

            breakdown.discount?.let { discount ->
                val label = if (breakdown.discountCode.isNullOrBlank()) {
                    stringResource(R.string.woopos_orders_details_breakdown_discount_label)
                } else {
                    stringResource(
                        R.string.woopos_orders_details_breakdown_discount_with_code_label,
                        breakdown.discountCode
                    )
                }
                TotalRowLine(label, discount, boldLabel = false)
            }

            Spacer(Modifier.height(WooPosSpacing.Small.value))

            TotalRowLine(
                label = stringResource(R.string.woopos_orders_details_breakdown_taxes_label),
                value = breakdown.taxes,
                boldLabel = false,
            )

            breakdown.shipping?.let {
                Spacer(Modifier.height(WooPosSpacing.Small.value))
                TotalRowLine(
                    label = stringResource(R.string.woopos_orders_details_breakdown_shipping_label),
                    value = it,
                    boldLabel = false
                )
            }

            DividerWithSpacing()

            TotalRowLine(
                label = stringResource(R.string.woopos_orders_details_total_label),
                value = details.total,
            )

            DividerWithSpacing()

            TotalRowLine(
                label = stringResource(R.string.woopos_orders_details_total_paid_label),
                value = details.totalPaid,
            )

            details.paymentMethodTitle?.let {
                Spacer(Modifier.height(WooPosSpacing.XSmall.value))
                WooPosText(
                    text = it,
                    style = WooPosTypography.BodyMedium,
                    color = WooPosTheme.colors.onSurfaceVariantHighest,
                )
            }

            if (breakdown.refunds.isNotEmpty()) {
                DividerWithSpacing()
                breakdown.refunds.forEachIndexed { index, refundAmount ->
                    TotalRowLine(
                        label = stringResource(R.string.woopos_orders_details_refunded_label),
                        value = refundAmount
                    )
                    if (index < breakdown.refunds.size - 1) {
                        DividerWithSpacing()
                    }
                }
            }

            breakdown.netPayment?.let { netPayment ->
                DividerWithSpacing()
                TotalRowLine(
                    label = stringResource(R.string.woopos_orders_details_net_payment_label),
                    value = netPayment,
                )
            }
        }
    }
}

@Composable
private fun TotalRowLine(
    label: String,
    value: String,
    boldLabel: Boolean = true
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        WooPosText(
            text = label,
            style = if (boldLabel) WooPosTypography.BodyLarge else WooPosTypography.BodyMedium,
            fontWeight = if (boldLabel) FontWeight.Bold else FontWeight.Normal,
            color = if (boldLabel) {
                MaterialTheme.colorScheme.onSurface
            } else {
                WooPosTheme.colors.onSurfaceVariantHighest
            }
        )
        Spacer(Modifier.weight(1f))
        WooPosText(
            text = value,
            style = WooPosTypography.BodyMedium,
        )
    }
}

@Composable
private fun OrderLineItemImage(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    WooPosItemImage(
        imageUrl = imageUrl,
        modifier = modifier
            .size(56.dp)
            .clip(RoundedCornerShape(WooPosCornerRadius.Small.value)),
        placeholderIcon = ImageVector.vectorResource(R.drawable.ic_inventory_2_24dp),
        placeholderIconSize = 24.dp
    )
}

@Composable
private fun DividerWithSpacing() {
    Spacer(Modifier.height(WooPosSpacing.Medium.value))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Spacer(Modifier.height(WooPosSpacing.Medium.value))
}

@Composable
private fun OrderActionButton(
    action: WooPosOrdersState.OrderAction,
    onClick: (WooPosOrdersState.OrderAction) -> Unit
) {
    when (action) {
        is WooPosOrdersState.OrderAction.IssueRefund -> {
            WooPosButtonSmall(
                text = stringResource(R.string.orderdetail_issue_refund_button),
                onClick = { onClick(action) }
            )
        }

        is WooPosOrdersState.OrderAction.EmailReceipt -> {
            WooPosButtonSmall(
                text = stringResource(R.string.woopos_orders_email_receipt),
                onClick = { onClick(action) }
            )
        }
    }
}

@Composable
private fun OrderDetailsOverflowMenu(
    actions: List<WooPosOrdersState.OrderAction>,
    onClick: (WooPosOrdersState.OrderAction) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_menu_more_vert),
                contentDescription = stringResource(R.string.more_menu),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        DropdownMenu(
            modifier = Modifier.background(color = MaterialTheme.colorScheme.surfaceContainerLowest),
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = {
                        val text = when (action) {
                            is WooPosOrdersState.OrderAction.IssueRefund -> stringResource(
                                R.string.orderdetail_issue_refund_button
                            )

                            is WooPosOrdersState.OrderAction.EmailReceipt -> stringResource(
                                R.string.woopos_orders_email_receipt
                            )
                        }
                        WooPosText(
                            text = text,
                            style = WooPosTypography.BodyMedium
                        )
                    },
                    onClick = {
                        showMenu = false
                        onClick(action)
                    }
                )
            }
        }
    }
}

@WooPosPreview
@Composable
fun WooPosOrderDetailsPreview() {
    val orderDetails = WooPosOrdersState.OrderDetailsViewState.Computed.Details(
        id = 1L,
        number = "#014",
        dateTime = "Aug 28, 2025 at 10:31 AM",
        customerEmail = "johndoe@mail.com",
        status = PosOrderStatus(text = "Completed", colorKey = OrderStatusColorKey.COMPLETED),
        lineItems = listOf(
            WooPosOrdersState.OrderDetailsViewState.Computed.Details.LineItemRow(
                101,
                "Cup",
                "2 x $4.00",
                "$8.00",
                null
            ),
            WooPosOrdersState.OrderDetailsViewState.Computed.Details.LineItemRow(
                102,
                "Coffee Container",
                "1 x $10.00",
                "$10.00",
                null
            ),
            WooPosOrdersState.OrderDetailsViewState.Computed.Details.LineItemRow(
                103,
                "A vey tasty coffee that incidentally has a very long name " +
                    "and should go over a few lines without overlapping anything",
                "1 x $5.00",
                "$5.00",
                null
            )
        ),
        breakdown = WooPosOrdersState.OrderDetailsViewState.Computed.Details.TotalsBreakdown(
            products = "$23.00",
            discount = "-$5.00",
            discountCode = "SAVE5",
            taxes = "$0.00",
            shipping = null,
            refunds = listOf("-$3.00", "-$2.00"),
            netPayment = "$13.00"
        ),
        total = "$18.00",
        totalPaid = "$18.00",
        paymentMethodTitle = "WooCommerce In-Person Payments",
        actions = listOf(
            WooPosOrdersState.OrderAction.IssueRefund(1L),
            WooPosOrdersState.OrderAction.EmailReceipt(1L)
        )
    )

    WooPosTheme {
        WooPosOrderDetails(
            details = orderDetails,
            onUIEvent = {}
        )
    }
}
