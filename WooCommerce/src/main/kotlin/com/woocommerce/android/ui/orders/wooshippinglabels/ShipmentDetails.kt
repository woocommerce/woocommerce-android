package com.woocommerce.android.ui.orders.wooshippinglabels

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.util.StringUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ShipmentDetails(
    scaffoldState: BottomSheetScaffoldState,
    shippableItems: ShippableItemsUI,
    shippingLines: List<ShippingLineSummaryUI>,
    markOrderComplete: Boolean,
    onMarkOrderCompleteChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    handlerModifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    Column(
        handlerModifier
            .clickable(
                onClick = {
                    scope.launch {
                        if (scaffoldState.bottomSheetState.isCollapsed) {
                            scaffoldState.bottomSheetState.expand()
                        } else {
                            scaffoldState.bottomSheetState.collapse()
                        }
                    }
                },
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.minor_100)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            modifier = Modifier.padding(top = dimensionResource(R.dimen.minor_100)),
            painter = if (scaffoldState.bottomSheetState.isExpanded) {
                painterResource(R.drawable.ic_arrow_down_26)
            } else {
                painterResource(R.drawable.ic_arrow_up_26)
            },
            contentDescription = stringResource(R.string.order_creation_expand_collapse_order_totals),
            tint = colorResource(id = R.color.color_primary),
        )
        AnimatedVisibility(visible = scaffoldState.bottomSheetState.isCollapsed) {
            Text(
                text = stringResource(R.string.shipping_label_shipment_details_title),
                color = MaterialTheme.colors.primary,
                modifier = Modifier
                    .padding(top = dimensionResource(R.dimen.minor_100))
            )
        }
    }
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        ShipmentDetailsLandscape(
            shippableItems = shippableItems,
            shippingLines = shippingLines,
            modifier = modifier
        )
    } else {
        ShipmentDetailsPortrait(
            shippableItems = shippableItems,
            shippingLines = shippingLines,
            markOrderComplete = markOrderComplete,
            onMarkOrderCompleteChange = onMarkOrderCompleteChange,
            modifier = modifier
        )
    }
}

@Composable
private fun ShipmentDetailsPortrait(
    shippableItems: ShippableItemsUI,
    shippingLines: List<ShippingLineSummaryUI>,
    markOrderComplete: Boolean,
    onMarkOrderCompleteChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = dimensionResource(R.dimen.major_200))
                .weight(1f)
                .verticalScroll(rememberScrollState())

        ) {
            OrderDetailsSection(
                shipFrom = getShipFrom(),
                shipTo = getShipTo(),
                totalItems = shippableItems.shippableItems.size,
                totalItemsCost = shippableItems.formattedTotalPrice,
                shippingLines = shippingLines
            )
            Divider(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.major_100)))
            ShipmentCostSection(
                subTotal = "$100.00",
                total = "$120.99",
                modifier = Modifier.padding(dimensionResource(R.dimen.major_100))
            )
        }
        Divider()
        MarkComplete(
            markOrderComplete = markOrderComplete,
            onMarkOrderCompleteChange = onMarkOrderCompleteChange
        )
    }
}

@Composable
private fun ShipmentDetailsLandscape(
    shippableItems: ShippableItemsUI,
    shippingLines: List<ShippingLineSummaryUI>,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = dimensionResource(R.dimen.major_200))
                .weight(1f)
                .verticalScroll(rememberScrollState())

        ) {
            AddressSectionLandscape(
                shipFrom = getShipFrom(),
                shipTo = getShipTo(),
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.major_100))
            )
            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .fillMaxWidth()
            ) {
                OrderDetailsSectionLandscape(
                    totalItems = shippableItems.shippableItems.size,
                    totalItemsCost = shippableItems.formattedTotalPrice,
                    shippingLines = shippingLines,
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(modifier = Modifier.padding(top = dimensionResource(R.dimen.major_100)))
                ShipmentCostSection(
                    subTotal = "$100.00",
                    total = "$120.99",
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.major_100))
                        .weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ShipmentDetailsSectionTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.body1,
        color = colorResource(R.color.color_on_surface_medium),
        modifier = modifier
    )
}

@Preview
@Composable
private fun ShipmentDetailsSectionTitlePreview() {
    WooThemeWithBackground {
        ShipmentDetailsSectionTitle(title = "Shipment Details")
    }
}

@Composable
private fun OrderDetailsSection(
    shipFrom: Address,
    shipTo: Address,
    totalItems: Int,
    totalItemsCost: String,
    shippingLines: List<ShippingLineSummaryUI>,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        ShipmentDetailsSectionTitle(
            title = stringResource(R.string.shipping_label_shipment_details_order_details),
            modifier = Modifier.padding(start = dimensionResource(R.dimen.major_100))
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.major_100)))
        AddressSection(
            shipFrom = shipFrom,
            shipTo = shipTo,
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.major_100))
        )
        TotalCard(
            totalItems = totalItems,
            totalItemsCost = totalItemsCost,
            shippingLines = shippingLines,
            modifier = Modifier.padding(dimensionResource(R.dimen.major_100))
        )
    }
}

@Composable
private fun OrderDetailsSectionLandscape(
    totalItems: Int,
    totalItemsCost: String,
    shippingLines: List<ShippingLineSummaryUI>,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        ShipmentDetailsSectionTitle(
            title = stringResource(R.string.shipping_label_shipment_details_order_details),
            modifier = Modifier.padding(
                top = dimensionResource(R.dimen.major_100),
                start = dimensionResource(R.dimen.major_100)
            )
        )
        TotalCard(
            totalItems = totalItems,
            totalItemsCost = totalItemsCost,
            shippingLines = shippingLines,
            modifier = Modifier.padding(dimensionResource(R.dimen.major_100))
        )
    }
}

@Preview(widthDp = 750, heightDp = 400)
@Composable
fun ShipmentDetailsLandscapePreview() {
    WooThemeWithBackground {
        Surface {
            ShipmentDetailsLandscape(
                shippableItems = ShippableItemsUI(
                    shippableItems = generateItems(6),
                    formattedTotalWeight = "8.5kg",
                    formattedTotalPrice = "$92.78"
                ),
                shippingLines = getShippingLines()
            )
        }
    }
}

@Composable
private fun TotalCard(
    totalItems: Int,
    totalItemsCost: String,
    shippingLines: List<ShippingLineSummaryUI>,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        ItemsCost(totalItems, totalItemsCost)
        ShippingLines(shippingLines)
    }
}

@Composable
private fun ItemsCost(
    totalItems: Int,
    totalItemsCost: String,
    modifier: Modifier = Modifier
) {
    val items = StringUtils.getQuantityString(
        context = LocalContext.current,
        quantity = totalItems,
        default = R.string.shipping_label_package_details_items_count_many,
        one = R.string.shipping_label_package_details_items_count_one,
    )
    TotalItem(
        title = items,
        amount = totalItemsCost,
        iconRes = R.drawable.ic_shipping_label_items,
        modifier = modifier
    )
}

@Preview
@Composable
private fun ItemsCostPreview() {
    WooThemeWithBackground {
        ItemsCost(totalItems = 2, totalItemsCost = "$12.99")
    }
}

@Composable
private fun ShippingLines(
    shippingLines: List<ShippingLineSummaryUI>,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        shippingLines.forEach { shippingLine ->
            TotalItem(
                title = shippingLine.title,
                amount = shippingLine.amount,
                iconRes = R.drawable.ic_shipping_label_shipping_line
            )
        }
    }
}

@Preview
@Composable
private fun ShippingLinesPreview() {
    WooThemeWithBackground {
        ShippingLines(
            shippingLines = getShippingLines()
        )
    }
}

@Composable
private fun TotalItem(
    title: String,
    amount: String,
    iconRes: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.minor_50)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.sizeIn(
                minHeight = dimensionResource(R.dimen.image_minor_80),
                minWidth = dimensionResource(R.dimen.image_minor_100)
            )
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = MaterialTheme.colors.onSurface
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = dimensionResource(R.dimen.minor_100))
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier.padding(end = dimensionResource(R.dimen.minor_100))
        )
    }
}

@Composable
private fun ShipmentCostSection(
    subTotal: String?,
    total: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        ShipmentDetailsSectionTitle(
            title = stringResource(R.string.shipping_label_shipment_details_shipment_cost)
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.major_100)))
        Row(modifier = Modifier.padding(top = dimensionResource(R.dimen.minor_100))) {
            Text(
                text = stringResource(R.string.subtotal),
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier.weight(1f)
            )
            subTotal?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.subtitle1,
                    color = MaterialTheme.colors.onSurface
                )
            } ?: SkeletonView(
                width = dimensionResource(id = R.dimen.skeleton_text_medium_width),
                height = dimensionResource(id = R.dimen.major_100)
            )
        }
        Row(modifier = Modifier.padding(top = dimensionResource(R.dimen.major_100))) {
            Text(
                text = stringResource(R.string.total),
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier.weight(1f)
            )
            total?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.subtitle1,
                    color = MaterialTheme.colors.onSurface
                )
            } ?: SkeletonView(
                width = dimensionResource(id = R.dimen.skeleton_text_medium_width),
                height = dimensionResource(id = R.dimen.major_100)
            )
        }
    }
}

@Preview
@Composable
private fun ShipmentCostSectionPreview() {
    WooThemeWithBackground {
        ShipmentCostSection(
            subTotal = "$12.99",
            total = "$12.99",
            modifier = Modifier.padding(dimensionResource(R.dimen.major_100))
        )
    }
}

fun getShippingLines(number: Int = 3) = List(number) { i ->
    ShippingLineSummaryUI(
        title = "Shipping $i",
        amount = "$12.99"
    )
}

fun Address.toShippingFromString() = this.getEnvelopeAddress().replace("\n", " ")

data class ShippingLineSummaryUI(
    val title: String,
    val amount: String
)

@Composable
fun VerticalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 1.dp
) {
    Spacer(
        modifier = modifier
            .fillMaxHeight()
            .width(thickness)
            .background(MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
    )
}
