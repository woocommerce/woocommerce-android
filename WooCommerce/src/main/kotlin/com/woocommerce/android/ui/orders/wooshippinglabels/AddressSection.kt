package com.woocommerce.android.ui.orders.wooshippinglabels

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
@Suppress("DestructuringDeclarationWithTooManyEntries")
internal fun AddressSection(
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
internal fun AddressSectionLandscape(
    shipFrom: Address,
    shipTo: Address,
    modifier: Modifier = Modifier
) {
    RoundedCornerBoxWithBorder(modifier) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Row(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.orderdetail_shipping_label_item_shipfrom),
                    modifier = Modifier
                        .padding(
                            start = dimensionResource(R.dimen.major_100),
                            top = dimensionResource(R.dimen.major_100),
                            end = dimensionResource(R.dimen.major_100)
                        )
                )
                Text(
                    text = shipFrom.toShippingFromString().uppercase(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier
                        .padding(
                            top = dimensionResource(R.dimen.major_100),
                            end = dimensionResource(R.dimen.major_100),
                            start = dimensionResource(R.dimen.major_100),
                            bottom = dimensionResource(R.dimen.minor_100)
                        )
                        .weight(1f)
                )
                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .padding(
                            top = dimensionResource(R.dimen.minor_50),
                            end = dimensionResource(R.dimen.minor_100)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreHoriz,
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary
                    )
                }
            }
            VerticalDivider()
            Row(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.orderdetail_shipping_label_item_shipto),
                    modifier = Modifier
                        .padding(
                            start = dimensionResource(R.dimen.major_100),
                            top = dimensionResource(R.dimen.major_100),
                            bottom = dimensionResource(R.dimen.major_100)
                        )
                )
                Text(
                    text = shipTo.toString(),
                    modifier = Modifier
                        .padding(
                            top = dimensionResource(R.dimen.major_100),
                            bottom = dimensionResource(R.dimen.major_100),
                            start = dimensionResource(R.dimen.major_100),
                            end = dimensionResource(R.dimen.minor_100)
                        )
                        .weight(1f)
                )
                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .padding(
                            top = dimensionResource(R.dimen.minor_100),
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
}

@Preview(widthDp = 750, heightDp = 200)
@Composable
private fun AddressSectionLandscapePreview() {
    WooThemeWithBackground {
        Box(modifier = Modifier.padding(dimensionResource(R.dimen.major_100))) {
            AddressSectionLandscape(
                shipFrom = getShipFrom(),
                shipTo = getShipTo()
            )
        }
    }
}

internal fun getShipFrom() = Address(
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

internal fun getShipTo() = Address(
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
