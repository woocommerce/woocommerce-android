package com.woocommerce.android.ui.orders.wooshippinglabels.address

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.constraintlayout.compose.Dimension
import com.woocommerce.android.R
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.compose.component.BottomSheetHandle
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.wooshippinglabels.RoundedCornerBoxWithBorder
import com.woocommerce.android.ui.orders.wooshippinglabels.ShipmentDetailsSectionTitle
import com.woocommerce.android.ui.orders.wooshippinglabels.VerticalDivider
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingAddresses
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.shippingSelectedBackgroundColor
import com.woocommerce.android.ui.orders.wooshippinglabels.toShippingFromString
import kotlinx.coroutines.launch

@Composable
@Suppress("DestructuringDeclarationWithTooManyEntries", "UnusedParameter")
internal fun AddressSectionPortrait(
    shippingAddresses: WooShippingAddresses,
    shipFromSelectionBottomSheetState: ModalBottomSheetState,
    modifier: Modifier = Modifier,
    isReadOnly: Boolean = false
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
            val endBarrier = createStartBarrier(shipFromSelect)
            val scope = rememberCoroutineScope()

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
                text = shippingAddresses.shipFrom.toShippingFromString().uppercase(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colors.primary,
                modifier = Modifier
                    .constrainAs(shipFromValue) {
                        top.linkTo(shipFromLabel.top)
                        start.linkTo(shipFromLabel.end)
                        end.linkTo(endBarrier)
                        width = Dimension.fillToConstraints
                    }
                    .padding(
                        top = dimensionResource(R.dimen.major_100),
                        bottom = dimensionResource(R.dimen.major_100),
                        start = dimensionResource(R.dimen.major_100),
                        end = dimensionResource(R.dimen.minor_100)
                    )
            )
            if (isReadOnly.not()) {
                IconButton(
                    onClick = {
                        if (shipFromSelectionBottomSheetState.isVisible.not()) {
                            scope.launch {
                                shipFromSelectionBottomSheetState.show()
                            }
                        }
                    },
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
                text = shippingAddresses.shipTo.toString(),
                modifier = Modifier
                    .constrainAs(shipToValue) {
                        top.linkTo(shipToLabel.top)
                        start.linkTo(barrier)
                        end.linkTo(shipToEdit.start)
                        width = Dimension.fillToConstraints
                    }
                    .padding(
                        top = dimensionResource(R.dimen.major_100),
                        bottom = dimensionResource(R.dimen.major_100),
                        start = dimensionResource(R.dimen.major_100),
                        end = dimensionResource(R.dimen.minor_100)
                    )
            )
            if (isReadOnly.not()) {
                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .constrainAs(shipToEdit) {
                            top.linkTo(shipToLabel.top)
                            end.linkTo(parent.end)
                            bottom.linkTo(shipToLabel.bottom)
                        }
                        .padding(end = dimensionResource(R.dimen.minor_100))
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

@Preview
@Composable
private fun AddressSectionPortraitPreview() {
    WooThemeWithBackground {
        Box(modifier = Modifier.padding(dimensionResource(R.dimen.major_100))) {
            AddressSectionPortrait(
                shippingAddresses = WooShippingAddresses(
                    shipFrom = getShipFrom(),
                    shipTo = getShipTo(),
                    originAddresses = listOf(getShipFrom())
                ),
                shipFromSelectionBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden),
                isReadOnly = false
            )
        }
    }
}

@Composable
@Suppress("UnusedParameter")
internal fun AddressSectionLandscape(
    shippingAddresses: WooShippingAddresses,
    shipFromSelectionBottomSheetState: ModalBottomSheetState,
    modifier: Modifier = Modifier,
    isReadOnly: Boolean = false
) {
    RoundedCornerBoxWithBorder(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            ShipFromSelection(
                shipFrom = shippingAddresses.shipFrom,
                modifier = Modifier.weight(1f),
                shipFromSelectionBottomSheetState = shipFromSelectionBottomSheetState,
                isReadOnly = isReadOnly
            )
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
                    text = shippingAddresses.shipTo.toString(),
                    modifier = Modifier
                        .padding(
                            top = dimensionResource(R.dimen.major_100),
                            bottom = dimensionResource(R.dimen.major_100),
                            start = dimensionResource(R.dimen.major_100),
                            end = dimensionResource(R.dimen.minor_100)
                        )
                        .weight(1f)
                )

                if (isReadOnly.not()) {
                    val iconModifier = if (shippingAddresses.shipTo == Address.EMPTY) {
                        Modifier
                            .padding(end = dimensionResource(R.dimen.minor_100))
                            .align(Alignment.CenterVertically)
                    } else {
                        Modifier
                            .padding(
                                top = dimensionResource(R.dimen.minor_100),
                                end = dimensionResource(R.dimen.minor_100)
                            )
                    }

                    IconButton(
                        onClick = { },
                        modifier = iconModifier
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
}

@Composable
private fun ShipFromSelection(
    shipFrom: OriginShippingAddress,
    shipFromSelectionBottomSheetState: ModalBottomSheetState,
    modifier: Modifier = Modifier,
    isReadOnly: Boolean = false
) {
    Row(modifier = modifier) {
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
        if (isReadOnly.not()) {
            val scope = rememberCoroutineScope()
            IconButton(
                onClick = {
                    if (shipFromSelectionBottomSheetState.isVisible.not()) {
                        scope.launch {
                            shipFromSelectionBottomSheetState.show()
                        }
                    }
                },
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
    }
}

@Composable
fun AddressSelection(
    modalBottomSheetState: ModalBottomSheetState,
    shipFrom: OriginShippingAddress,
    originAddresses: List<OriginShippingAddress>,
    onShippingFromAddressChange: (OriginShippingAddress) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    ModalBottomSheetLayout(
        modifier = modifier,
        sheetState = modalBottomSheetState,
        sheetContent = {
            BottomSheetHandle(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = dimensionResource(id = R.dimen.minor_100))
            )
            ShipmentDetailsSectionTitle(
                title = stringResource(R.string.orderdetail_shipping_label_item_shipfrom),
                modifier = Modifier.padding(
                    start = dimensionResource(id = R.dimen.major_100),
                    top = dimensionResource(id = R.dimen.major_100),
                    bottom = dimensionResource(id = R.dimen.minor_100)
                )
            )
            LazyColumn {
                items(originAddresses) { option ->
                    val isSelected = option == shipFrom
                    AddressSelectionItem(
                        address = option,
                        isSelected = isSelected,
                        onClick = {
                            onShippingFromAddressChange(option)
                        },
                        modifier = Modifier.padding(
                            top = dimensionResource(id = R.dimen.minor_100),
                            start = dimensionResource(id = R.dimen.major_100),
                            end = dimensionResource(id = R.dimen.major_100)
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        },
        sheetShape = RoundedCornerShape(
            topStart = dimensionResource(id = R.dimen.corner_radius_large),
            topEnd = dimensionResource(id = R.dimen.corner_radius_large)
        ),
        content = content
    )
}

@Composable
fun AddressSelectionItem(
    address: OriginShippingAddress,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colors.primary
    } else {
        colorResource(R.color.divider_color)
    }

    val backgroundColor = if (isSelected) {
        animateColorAsState(
            targetValue = MaterialTheme.colors.shippingSelectedBackgroundColor,
            label = "colorAnimation"
        )
    } else {
        animateColorAsState(targetValue = MaterialTheme.colors.surface, label = "colorAnimation")
    }

    RoundedCornerBoxWithBorder(
        modifier = modifier,
        innerModifier = Modifier
            .clickable { onClick() }
            .padding(dimensionResource(id = R.dimen.major_100)),
        borderColor = borderColor,
        backgroundColor = backgroundColor.value
    ) {
        Row {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = address.getFormattedName(LocalContext.current),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                )
                Text(
                    text = address.toShippingFromString(),
                    modifier = Modifier.padding(top = dimensionResource(id = R.dimen.minor_100))
                )
            }
            IconButton(
                onClick = { }
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

@Preview(widthDp = 750, heightDp = 200)
@Composable
private fun AddressSectionLandscapePreview() {
    WooThemeWithBackground {
        Box(modifier = Modifier.padding(dimensionResource(R.dimen.major_100))) {
            AddressSectionLandscape(
                shippingAddresses = WooShippingAddresses(
                    shipFrom = getShipFrom(),
                    shipTo = getShipTo(),
                    originAddresses = listOf(getShipFrom())
                ),
                shipFromSelectionBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden),
                isReadOnly = false
            )
        }
    }
}

internal fun getShipFrom() = OriginShippingAddress(
    firstName = "first name",
    lastName = "last name",
    company = "Company",
    phone = "",
    address1 = "A huge address that should be truncated",
    address2 = "",
    city = "City",
    postcode = "",
    email = "email",
    country = "USA",
    state = "California",
    id = "id_1",
    isDefault = true,
    isVerified = true
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

fun OriginShippingAddress.getFormattedName(context: Context): String {
    val name = when {
        !firstName.isNullOrEmpty() && !lastName.isNullOrEmpty() -> "$firstName $lastName"
        !firstName.isNullOrEmpty() -> firstName
        !lastName.isNullOrEmpty() -> lastName
        !company.isNullOrEmpty() -> company
        else -> context.getString(R.string.shipping_label_select_origin_address)
    }
    return if (this.isDefault) {
        context.getString(R.string.shipping_label_select_origin_default_address, name)
    } else {
        name
    }
}
