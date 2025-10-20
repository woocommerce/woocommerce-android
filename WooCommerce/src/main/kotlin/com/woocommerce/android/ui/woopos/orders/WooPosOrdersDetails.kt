package com.woocommerce.android.ui.woopos.orders

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonSmall
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosOrderDetails(
    modifier: Modifier = Modifier,
    details: OrderDetailsViewState,
    onEmailReceiptButtonClicked: (Long) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = WooPosSpacing.Large.value,
                vertical = WooPosSpacing.Medium.value
            )
    ) {
        Row {
            WooPosText(
                text = details.number,
                style = WooPosTypography.Heading
            )

            Spacer(Modifier.weight(1f))

            WooPosButtonSmall(
                text = stringResource(R.string.woopos_orders_email_receipt),
                onClick = { onEmailReceiptButtonClicked(details.id) },
            )
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
private fun OrdersHeader(details: OrderDetailsViewState) {
    ConstraintLayout(modifier = Modifier.fillMaxWidth()) {
        val (dateTimeText, emailText, statusBadge, emailButton) = createRefs()

        WooPosText(
            text = details.dateTime,
            style = WooPosTypography.BodySmall,
            color = WooPosTheme.colors.onSurfaceVariantHighest,
            modifier = Modifier.constrainAs(dateTimeText) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(emailButton.start, margin = WooPosSpacing.Medium.value)
                width = Dimension.fillToConstraints
            }
        )

        details.customerEmail?.takeIf { it.isNotBlank() }?.let {
            WooPosText(
                text = it,
                style = WooPosTypography.BodySmall,
                color = WooPosTheme.colors.onSurfaceVariantHighest,
                modifier = Modifier.constrainAs(emailText) {
                    top.linkTo(dateTimeText.bottom, margin = WooPosSpacing.XSmall.value)
                    start.linkTo(parent.start)
                    end.linkTo(emailButton.start, margin = WooPosSpacing.Medium.value)
                    width = Dimension.fillToConstraints
                }
            )
        }

        Box(
            modifier = Modifier.constrainAs(statusBadge) {
                top.linkTo(
                    if (details.customerEmail?.isNotBlank() == true) emailText.bottom
                    else dateTimeText.bottom,
                    margin = WooPosSpacing.Small.value
                )
                start.linkTo(parent.start)
            }
        ) {
            WooPosOrdersStatusBadge(status = details.status)
        }
    }
}

@Composable
private fun OrdersProducts(lineItems: List<OrderDetailsViewState.LineItemRow>) {
    WooPosCard(
        shape = MaterialTheme.shapes.medium,
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = WooPosElevation.Medium,
        shadowType = com.woocommerce.android.ui.woopos.common.composeui.component.ShadowType.Soft
    ) {
        Column(Modifier.padding(WooPosSpacing.Medium.value)) {
            WooPosText(
                text = stringResource(R.string.woopos_orders_details_products_title),
                style = WooPosTypography.Heading
            )
            Spacer(Modifier.height(WooPosSpacing.Small.value))

            lineItems.forEach { row ->
                OrderProductItem(row = row)
            }
        }
    }
}

@Composable
private fun OrderProductItem(row: OrderDetailsViewState.LineItemRow) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = WooPosSpacing.Small.value)
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
            style = WooPosTypography.BodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.constrainAs(nameText) {
                top.linkTo(parent.top)
                start.linkTo(image.end)
                end.linkTo(totalText.start, margin = WooPosSpacing.Small.value)
                width = Dimension.fillToConstraints
            }
        )

        WooPosText(
            text = row.qtyAndUnitPrice,
            style = WooPosTypography.BodySmall,
            color = WooPosTheme.colors.onSurfaceVariantHighest,
            modifier = Modifier.constrainAs(qtyText) {
                top.linkTo(nameText.bottom, margin = WooPosSpacing.XSmall.value)
                start.linkTo(image.end)
                end.linkTo(totalText.start, margin = WooPosSpacing.Small.value)
                width = Dimension.fillToConstraints
            }
        )

        WooPosText(
            text = row.lineTotal,
            style = WooPosTypography.BodySmall,
            modifier = Modifier.constrainAs(totalText) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                end.linkTo(parent.end)
            }
        )
    }
}

@Composable
private fun OrdersTotals(details: OrderDetailsViewState) {
    WooPosCard(
        shape = MaterialTheme.shapes.medium,
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = WooPosElevation.Medium,
        shadowType = com.woocommerce.android.ui.woopos.common.composeui.component.ShadowType.Soft
    ) {
        Column(Modifier.padding(WooPosSpacing.Medium.value)) {
            WooPosText(
                text = stringResource(R.string.woopos_orders_details_totals_title),
                style = WooPosTypography.Heading
            )
            Spacer(Modifier.height(WooPosSpacing.Small.value))

            val breakdown = details.breakdown
            TotalRowLine(
                label = stringResource(R.string.woopos_orders_details_breakdown_products_label),
                value = breakdown.products
            )

            breakdown.discount?.let { discount ->
                val label = if (breakdown.discountCode.isNullOrBlank()) {
                    stringResource(R.string.woopos_orders_details_breakdown_discount_label)
                } else {
                    stringResource(
                        R.string.woopos_orders_details_breakdown_discount_with_code_label,
                        breakdown.discountCode
                    )
                }
                TotalRowLine(label, discount)
            }

            TotalRowLine(
                label = stringResource(R.string.woopos_orders_details_breakdown_taxes_label),
                value = breakdown.taxes
            )

            breakdown.shipping?.let {
                TotalRowLine(
                    label = stringResource(R.string.woopos_orders_details_breakdown_shipping_label),
                    value = it
                )
            }

            Spacer(Modifier.height(WooPosSpacing.Small.value))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            )

            TotalRowLine(
                label = stringResource(R.string.woopos_orders_details_total_label),
                value = details.total,
                fontWeight = FontWeight.Bold,
                paddingVertical = WooPosSpacing.Small.value
            )

            TotalRowLine(
                label = stringResource(R.string.woopos_orders_details_total_paid_label),
                value = details.totalPaid
            )

            details.paymentMethodTitle?.let {
                Spacer(Modifier.height(WooPosSpacing.XSmall.value))
                WooPosText(
                    text = it,
                    style = WooPosTypography.BodySmall,
                    color = WooPosTheme.colors.onSurfaceVariantHighest,
                )
            }

            if (breakdown.refunds.isNotEmpty()) {
                Spacer(Modifier.height(WooPosSpacing.Small.value))
                breakdown.refunds.forEachIndexed { index, refundAmount ->
                    TotalRowLine(
                        label = stringResource(R.string.woopos_orders_details_refunded_label),
                        value = refundAmount
                    )
                    if (index < breakdown.refunds.size - 1) {
                        Spacer(Modifier.height(WooPosSpacing.XSmall.value))
                    }
                }
            }

            breakdown.netPayment?.let { netPayment ->
                Spacer(Modifier.height(WooPosSpacing.Small.value))
                TotalRowLine(
                    label = stringResource(R.string.woopos_orders_details_net_payment_label),
                    value = netPayment,
                    fontWeight = FontWeight.Bold,
                    paddingBottom = WooPosSpacing.Medium.value
                )
            }
        }
    }
}

@Composable
private fun TotalRowLine(
    label: String,
    value: String,
    fontWeight: FontWeight? = null,
    paddingVertical: Dp = WooPosSpacing.XSmall.value,
    paddingBottom: Dp? = null
) {
    val modifier = if (paddingBottom != null) {
        Modifier
            .fillMaxWidth()
            .padding(bottom = paddingBottom)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(vertical = paddingVertical)
    }

    Row(modifier) {
        WooPosText(
            text = label,
            style = WooPosTypography.BodySmall,
            color = WooPosTheme.colors.onSurfaceVariantHighest,
            fontWeight = fontWeight,
            modifier = Modifier.weight(1f)
        )
        WooPosText(
            text = value,
            style = WooPosTypography.BodySmall,
            fontWeight = fontWeight
        )
    }
}

@Composable
private fun OrderLineItemImage(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(56.dp)
            .height(56.dp)
            .background(MaterialTheme.colorScheme.surfaceDim),
        contentAlignment = Alignment.Center
    ) {
        Image(
            imageVector = Icons.Outlined.Inventory2,
            contentDescription = null,
            colorFilter = ColorFilter.tint(WooPosTheme.colors.onSurfaceVariantLowest),
            modifier = Modifier.size(24.dp)
        )
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop
        )
    }
}

@WooPosPreview
@Composable
fun WooPosOrderDetailsPreview() {
    val orderDetails = OrderDetailsViewState(
        id = 1L,
        number = "#014",
        dateTime = "Aug 28, 2025 at 10:31 AM",
        customerEmail = "johndoe@mail.com",
        status = PosOrderStatus(text = "Completed", colorKey = OrderStatusColorKey.COMPLETED),
        lineItems = listOf(
            OrderDetailsViewState.LineItemRow(101, "Cup", "2 x $4.00", "$8.00", null),
            OrderDetailsViewState.LineItemRow(102, "Coffee Container", "1 x $10.00", "$10.00", null),
            OrderDetailsViewState.LineItemRow(103, "Paper Filter", "1 x $5.00", "$5.00", null)
        ),
        breakdown = OrderDetailsViewState.TotalsBreakdown(
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
        paymentMethodTitle = "WooCommerce In-Person Payments"
    )

    WooPosTheme {
        WooPosOrderDetails(
            details = orderDetails,
            onEmailReceiptButtonClicked = {}
        )
    }
}
