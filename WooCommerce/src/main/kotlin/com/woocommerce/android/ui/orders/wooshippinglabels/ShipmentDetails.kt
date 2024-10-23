package com.woocommerce.android.ui.orders.wooshippinglabels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.ConstraintLayout
import com.woocommerce.android.R
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.compose.component.BottomSheetHandle
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
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
            .padding(dimensionResource(R.dimen.major_200))
    ) {
        OrderDetailsSection(
            shipFrom = getShipFrom(),
            shipTo = getShipTo()
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
    modifier: Modifier = Modifier,
    shipFrom: Address,
    shipTo: Address,
) {
    Column(modifier.fillMaxWidth()) {
        ShipmentDetailsSectionTitle(
            title = stringResource(R.string.shipping_label_shipment_details_order_details)
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.major_100)))
        AddressSection(
            shipFrom = shipFrom,
            shipTo = shipTo
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

fun Address.toShippingFromString() = this.getEnvelopeAddress().replace("\n", " ")
