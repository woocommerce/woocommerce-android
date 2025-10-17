package com.woocommerce.android.ui.woopos.orders

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToolbar
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun OrderDetails(
    modifier: Modifier = Modifier,
    details: OrderDetailsViewState,
    onEmailReceiptButtonClicked: (Long) -> Unit = {}
) {
    Column(modifier = modifier.fillMaxSize()) {
        WooPosToolbar(
            modifier = Modifier.fillMaxWidth(),
            titleText = details.number,
            onBackClicked = null
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = WooPosSpacing.Large.value,
                    end = WooPosSpacing.Large.value,
                    top = WooPosSpacing.Medium.value,
                    bottom = WooPosSpacing.XLarge.value
                ),
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Large.value)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    WooPosText(
                        text = details.dateTime,
                        style = WooPosTypography.BodySmall,
                        color = WooPosTheme.colors.onSurfaceVariantHighest
                    )
                    details.customerEmail?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(WooPosSpacing.XSmall.value))
                        WooPosText(
                            text = it,
                            style = WooPosTypography.BodySmall,
                            color = WooPosTheme.colors.onSurfaceVariantHighest
                        )
                    }
                    Spacer(Modifier.height(WooPosSpacing.Small.value))
                    OrderStatusBadge(details.status)
                }

                WooPosButton(
                    text = stringResource(R.string.woopos_orders_email_receipt),
                    onClick = { onEmailReceiptButtonClicked(details.id) }
                )
            }

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

                    details.lineItems.forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = WooPosSpacing.Small.value),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OrderLineItemImage(imageUrl = row.imageUrl)

                            Column(Modifier.weight(1f)) {
                                WooPosText(
                                    text = row.name,
                                    style = WooPosTypography.BodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.height(WooPosSpacing.XSmall.value))
                                WooPosText(
                                    text = row.qtyAndUnitPrice,
                                    style = WooPosTypography.BodySmall,
                                    color = WooPosTheme.colors.onSurfaceVariantHighest
                                )
                            }
                            WooPosText(
                                text = row.lineTotal,
                                style = WooPosTypography.BodySmall
                            )
                        }
                    }
                }
            }

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

                    @Composable
                    fun RowLine(label: String, value: String) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = WooPosSpacing.XSmall.value)
                        ) {
                            WooPosText(
                                text = label,
                                style = WooPosTypography.BodySmall,
                                color = WooPosTheme.colors.onSurfaceVariantHighest,
                                modifier = Modifier.weight(1f)
                            )
                            WooPosText(
                                text = value,
                                style = WooPosTypography.BodySmall
                            )
                        }
                    }

                    val b = details.breakdown
                    RowLine(
                        label = stringResource(R.string.woopos_orders_details_breakdown_products_label),
                        value = b.products
                    )

                    b.discount?.let { d ->
                        val label =
                            if (b.discountCode.isNullOrBlank()) {
                                stringResource(R.string.woopos_orders_details_breakdown_discount_label)
                            } else {
                                stringResource(
                                    R.string.woopos_orders_details_breakdown_discount_with_code_label,
                                    b.discountCode
                                )
                            }
                        RowLine(label, d)
                    }

                    RowLine(
                        label = stringResource(R.string.woopos_orders_details_breakdown_taxes_label),
                        value = b.taxes
                    )

                    b.shipping?.let {
                        RowLine(
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

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = WooPosSpacing.Small.value)
                    ) {
                        WooPosText(
                            text = stringResource(R.string.woopos_orders_details_total_label),
                            style = WooPosTypography.BodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        WooPosText(
                            text = details.total,
                            style = WooPosTypography.BodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column {
                Row(Modifier.fillMaxWidth()) {
                    WooPosText(
                        text = stringResource(R.string.woopos_orders_details_total_paid_label),
                        style = WooPosTypography.BodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    WooPosText(
                        text = details.totalPaid,
                        style = WooPosTypography.BodySmall
                    )
                }
                details.paymentMethodTitle?.let {
                    Spacer(Modifier.height(WooPosSpacing.XSmall.value))
                    WooPosText(
                        text = it,
                        style = WooPosTypography.BodySmall,
                        color = WooPosTheme.colors.onSurfaceVariantHighest
                    )
                }
            }

            if (details.breakdown.refunds.isNotEmpty()) {
                Column {
                    details.breakdown.refunds.forEach { refundAmount ->
                        Row(Modifier.fillMaxWidth()) {
                            WooPosText(
                                text = stringResource(R.string.woopos_orders_details_refunded_label),
                                style = WooPosTypography.BodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            WooPosText(
                                text = refundAmount,
                                style = WooPosTypography.BodySmall
                            )
                        }
                        Spacer(Modifier.height(WooPosSpacing.XSmall.value))
                    }
                }
            }

            details.breakdown.netPayment?.let { netPayment ->
                Spacer(Modifier.height(WooPosSpacing.Small.value))
                Row(Modifier.fillMaxWidth()) {
                    WooPosText(
                        text = stringResource(R.string.woopos_orders_details_net_payment_label),
                        style = WooPosTypography.BodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    WooPosText(
                        text = netPayment,
                        style = WooPosTypography.BodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderLineItemImage(imageUrl: String?) {
    Box(
        modifier = Modifier
            .width(56.dp)
            .fillMaxHeight()
            .heightIn(min = 56.dp)
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
