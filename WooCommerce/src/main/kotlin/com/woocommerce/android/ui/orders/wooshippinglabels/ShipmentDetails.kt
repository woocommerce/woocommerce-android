package com.woocommerce.android.ui.orders.wooshippinglabels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.ConstraintLayout
import com.woocommerce.android.R
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.BottomSheetHandle
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.util.StringUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ShipmentDetails(
    scaffoldState: BottomSheetScaffoldState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier
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
        BottomSheetHandle(modifier = Modifier.padding(top = dimensionResource(R.dimen.minor_100)))
        AnimatedVisibility(visible = scaffoldState.bottomSheetState.isCollapsed) {
            Text(
                text = stringResource(R.string.shipping_label_shipment_details_title),
                color = MaterialTheme.colors.primary,
                modifier = Modifier
                    .padding(top = dimensionResource(R.dimen.minor_100))
            )
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(top = dimensionResource(R.dimen.major_200))
    ) {
        OrderDetailsSection(
            shipFrom = getShipFrom(),
            shipTo = getShipTo(),
            totalItems = 5,
            totalItemsCost = "$120.99",
            shippingLines = getShippingLines(3)
        )
        Divider(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.major_100)))
        ShipmentCostSection(
            subTotal = null,
            total = "$120.99",
            modifier = Modifier.padding(dimensionResource(R.dimen.major_100))
        )
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
    shippingLines: List<ShippingLineSummary>,
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
@Suppress("DestructuringDeclarationWithTooManyEntries")
private fun AddressSection(
    shipFrom: Address,
    shipTo: Address,
    modifier: Modifier = Modifier
) {
    RoundedCornerBoxWithBorder(modifier.fillMaxWidth()) {
        ConstraintLayout {
            val (
                shipFromLabel,
                shipFromValue,
                shipFromSelect,
                shipToLabel,
                shipToValue,
                shipToEdit,
                divider
            ) = createRefs()

            val barrier = createEndBarrier(shipFromLabel, shipToLabel)

            Text(
                text = stringResource(id = R.string.orderdetail_shipping_label_item_shipfrom),
                modifier = Modifier
                    .constrainAs(shipFromLabel) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                    }
                    .padding(
                        start = dimensionResource(R.dimen.major_100),
                        top = dimensionResource(R.dimen.major_100),
                        bottom = dimensionResource(R.dimen.major_100)
                    )

            )
            Text(
                text = shipFrom.toShippingFromString().uppercase(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colors.primary,
                modifier = Modifier
                    .constrainAs(shipFromValue) {
                        top.linkTo(shipFromLabel.top)
                        start.linkTo(shipFromLabel.end)
                        end.linkTo(shipFromSelect.start)
                        width = androidx.constraintlayout.compose.Dimension.fillToConstraints
                    }
                    .padding(
                        top = dimensionResource(R.dimen.major_100),
                        bottom = dimensionResource(R.dimen.major_100),
                        start = dimensionResource(R.dimen.major_100),
                        end = dimensionResource(R.dimen.minor_100)
                    )
            )
            IconButton(
                onClick = { },
                modifier = Modifier
                    .constrainAs(shipFromSelect) {
                        top.linkTo(shipFromLabel.top)
                        end.linkTo(parent.end)
                        bottom.linkTo(shipFromLabel.bottom)
                    }
                    .padding(
                        end = dimensionResource(R.dimen.minor_100)
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary
                )
            }
            Divider(
                modifier = Modifier.constrainAs(divider) {
                    top.linkTo(shipFromLabel.bottom)
                    start.linkTo(parent.start)
                }
            )
            Text(
                text = stringResource(id = R.string.orderdetail_shipping_label_item_shipto),
                modifier = Modifier
                    .constrainAs(shipToLabel) {
                        top.linkTo(divider.bottom)
                        start.linkTo(shipFromLabel.start)
                    }
                    .padding(
                        start = dimensionResource(R.dimen.major_100),
                        top = dimensionResource(R.dimen.major_100),
                        bottom = dimensionResource(R.dimen.major_100)
                    )
            )
            Text(
                text = shipTo.toString(),
                modifier = Modifier
                    .constrainAs(shipToValue) {
                        top.linkTo(shipToLabel.top)
                        start.linkTo(barrier)
                        end.linkTo(shipToEdit.start)
                        width = androidx.constraintlayout.compose.Dimension.fillToConstraints
                    }
                    .padding(
                        top = dimensionResource(R.dimen.major_100),
                        bottom = dimensionResource(R.dimen.major_100),
                        start = dimensionResource(R.dimen.major_100),
                        end = dimensionResource(R.dimen.minor_100)
                    )
            )
            IconButton(
                onClick = { },
                modifier = Modifier
                    .constrainAs(shipToEdit) {
                        top.linkTo(shipToLabel.top)
                        end.linkTo(parent.end)
                    }
                    .padding(
                        top = dimensionResource(R.dimen.major_100),
                        end = dimensionResource(R.dimen.minor_100)
                    )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_edit_pencil),
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                )
            }
        }
    }
}

@Preview
@Composable
private fun AddressSectionPreview() {
    WooThemeWithBackground {
        Box(modifier = Modifier.padding(dimensionResource(R.dimen.major_100))) {
            AddressSection(
                shipFrom = getShipFrom(),
                shipTo = getShipTo()
            )
        }
    }
}

@Composable
private fun TotalCard(
    totalItems: Int,
    totalItemsCost: String,
    shippingLines: List<ShippingLineSummary>,
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
    shippingLines: List<ShippingLineSummary>,
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

private fun getShipFrom() = Address(
    firstName = "first name",
    lastName = "last name",
    company = "Company",
    phone = "",
    address1 = "A huge address that should be truncated",
    address2 = "",
    city = "City",
    postcode = "",
    email = "email",
    country = Location("US", "USA"),
    state = AmbiguousLocation.Defined(Location("CA", "California", "USA"))
)

private fun getShipTo() = Address(
    firstName = "first name",
    lastName = "last name",
    company = "Company",
    phone = "",
    address1 = "Another Address",
    address2 = "",
    city = "City",
    postcode = "",
    email = "email",
    country = Location("US", "USA"),
    state = AmbiguousLocation.Defined(Location("CA", "California", "USA"))
)

private fun getShippingLines(number: Int = 3) = List(number) { i ->
    ShippingLineSummary(
        title = "Shipping $i",
        amount = "$12.99"
    )
}

fun Address.toShippingFromString() = this.getEnvelopeAddress().replace("\n", " ")

data class ShippingLineSummary(
    val title: String,
    val amount: String
)
