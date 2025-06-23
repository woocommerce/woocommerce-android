package com.woocommerce.android.ui.orders.wooshippinglabels.address

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.woocommerce.android.R
import com.woocommerce.android.extensions.appendWithIfNotEmpty
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.wooshippinglabels.RoundedCornerBoxWithBorder
import com.woocommerce.android.ui.orders.wooshippinglabels.ShippingLabelSampleData
import com.woocommerce.android.ui.orders.wooshippinglabels.VerticalDivider
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingAddresses
import com.woocommerce.android.ui.orders.wooshippinglabels.models.DestinationShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress

@Composable
@Suppress("DestructuringDeclarationWithTooManyEntries")
internal fun AddressSectionPortrait(
    shippingAddresses: WooShippingAddresses,
    onEditDestinationAddress: (DestinationShippingAddress) -> Unit,
    onEditOriginAddress: (OriginShippingAddress) -> Unit,
    onOriginAddressSelected: (OriginShippingAddress) -> Unit,
    destinationStatus: AddressStatus,
    modifier: Modifier = Modifier,
    isReadOnly: Boolean = false
) {
    var isAddressSelectionBottomSheetVisible by rememberSaveable { mutableStateOf(false) }

    RoundedCornerBoxWithBorder(modifier.fillMaxWidth()) {
        ConstraintLayout {
            val (
                shipFromLabel,
                shipFromValue,
                shipFromSelect,
                shipToLabel,
                shipToValue,
                shipToEdit,
                divider,
                destinationAddressStatus
            ) = createRefs()

            val barrier = createEndBarrier(shipFromLabel, shipToLabel)
            val endBarrier = createStartBarrier(shipFromSelect)

            val destinationStatusModifier = if (destinationStatus == AddressStatus.MISSING_ADDRESS) {
                Modifier
                    .padding(start = dimensionResource(R.dimen.major_100))
                    .constrainAs(destinationAddressStatus) {
                        top.linkTo(divider.bottom)
                        start.linkTo(barrier)
                        bottom.linkTo(parent.bottom)
                    }
            } else {
                Modifier
                    .padding(
                        bottom = dimensionResource(R.dimen.major_100),
                        start = dimensionResource(R.dimen.major_100)
                    )
                    .constrainAs(destinationAddressStatus) {
                        top.linkTo(shipToValue.bottom)
                        start.linkTo(shipToValue.start)
                    }
            }

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
                    onClick = { isAddressSelectionBottomSheetVisible = true },
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
            if (destinationStatus != AddressStatus.MISSING_ADDRESS) {
                Text(
                    text = shippingAddresses.shipTo.address.toString(),
                    modifier = Modifier
                        .constrainAs(shipToValue) {
                            top.linkTo(shipToLabel.top)
                            start.linkTo(barrier)
                            end.linkTo(shipToEdit.start)
                            width = Dimension.fillToConstraints
                        }
                        .padding(
                            top = dimensionResource(R.dimen.major_100),
                            bottom = dimensionResource(R.dimen.minor_100),
                            start = dimensionResource(R.dimen.major_100),
                            end = dimensionResource(R.dimen.minor_100)
                        ),
                )
            }
            if (isReadOnly.not()) {
                AddressStatusIndicator(
                    addressStatus = destinationStatus,
                    modifier = destinationStatusModifier
                )

                IconButton(
                    onClick = { onEditDestinationAddress(shippingAddresses.shipTo) },
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

    if (isAddressSelectionBottomSheetVisible) {
        AddressSelectionBottomSheet(
            shipFrom = shippingAddresses.shipFrom,
            originAddresses = shippingAddresses.originAddresses,
            onDismiss = { isAddressSelectionBottomSheetVisible = false },
            onOriginAddressSelected = onOriginAddressSelected,
            onEditOriginAddress = onEditOriginAddress
        )
    }
}

@Composable
internal fun AddressSectionLandscape(
    shippingAddresses: WooShippingAddresses,
    onEditDestinationAddress: (DestinationShippingAddress) -> Unit,
    onEditOriginAddress: (OriginShippingAddress) -> Unit,
    onOriginAddressSelected: (OriginShippingAddress) -> Unit,
    destinationStatus: AddressStatus,
    modifier: Modifier = Modifier,
    isReadOnly: Boolean = false
) {
    var isAddressSelectionBottomSheetVisible by rememberSaveable { mutableStateOf(false) }

    RoundedCornerBoxWithBorder(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            ShipFromSelection(
                shipFrom = shippingAddresses.shipFrom,
                modifier = Modifier.weight(1f),
                onSelectShipFromClick = { isAddressSelectionBottomSheetVisible = true },
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

                Column(modifier = Modifier.weight(1f)) {
                    val destinationAddressStatusModifier = if (shippingAddresses.shipTo.address.hasInfo()) {
                        Modifier.padding(
                            start = dimensionResource(R.dimen.major_100),
                            bottom = dimensionResource(R.dimen.major_100)
                        )
                    } else {
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(16.dp)
                            .height(IntrinsicSize.Min)
                    }

                    if (shippingAddresses.shipTo.address.hasInfo()) {
                        Text(
                            text = shippingAddresses.shipTo.address.toString(),
                            modifier = Modifier
                                .padding(
                                    top = dimensionResource(R.dimen.major_100),
                                    bottom = dimensionResource(R.dimen.minor_100),
                                    start = dimensionResource(R.dimen.major_100),
                                    end = dimensionResource(R.dimen.minor_100)
                                )
                        )
                    }
                    if (!isReadOnly) {
                        AddressStatusIndicator(
                            addressStatus = destinationStatus,
                            modifier = destinationAddressStatusModifier
                        )
                    }
                }

                if (isReadOnly.not()) {
                    IconButton(
                        onClick = { onEditDestinationAddress(shippingAddresses.shipTo) },
                        modifier = Modifier
                            .padding(
                                top = dimensionResource(R.dimen.minor_50),
                                end = dimensionResource(R.dimen.minor_100),
                                bottom = dimensionResource(R.dimen.minor_50)
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

    if (isAddressSelectionBottomSheetVisible) {
        AddressSelectionBottomSheet(
            shipFrom = shippingAddresses.shipFrom,
            originAddresses = shippingAddresses.originAddresses,
            onDismiss = { isAddressSelectionBottomSheetVisible = false },
            onOriginAddressSelected = onOriginAddressSelected,
            onEditOriginAddress = onEditOriginAddress
        )
    }
}

@Composable
private fun ShipFromSelection(
    shipFrom: OriginShippingAddress,
    onSelectShipFromClick: () -> Unit,
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
            IconButton(
                onClick = onSelectShipFromClick,
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
fun OriginAddressSelectionItem(
    address: OriginShippingAddress,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEdit: (OriginShippingAddress) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colors.primary
    } else {
        colorResource(R.color.divider_color)
    }

    val backgroundColor = if (isSelected) {
        animateColorAsState(
            targetValue = colorResource(R.color.woo_item_selected),
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
                onClick = { onEdit(address) }
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

@Composable
fun AddressStatusIndicator(
    addressStatus: AddressStatus,
    modifier: Modifier = Modifier
) {
    val text = when (addressStatus) {
        AddressStatus.VERIFIED -> stringResource(id = R.string.woo_shipping_address_verified)
        AddressStatus.UNVERIFIED -> stringResource(id = R.string.woo_shipping_address_unverified)
        AddressStatus.MISSING_INFO -> stringResource(id = R.string.woo_shipping_address_missing_info)
        AddressStatus.SAVE_CHANGES -> stringResource(id = R.string.woo_shipping_address_unsaved_changes)
        AddressStatus.MISSING_ADDRESS -> stringResource(id = R.string.woo_shipping_address_missing)
    }

    val color = when (addressStatus) {
        AddressStatus.VERIFIED -> colorResource(id = R.color.woo_shipping_label_success)
        else -> colorResource(id = R.color.woo_shipping_label_error)
    }

    val icon = when (addressStatus) {
        AddressStatus.VERIFIED -> Icons.Outlined.CheckCircleOutline
        else -> Icons.Outlined.Info
    }

    Row(modifier) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            color = color
        )
    }
}

private fun OriginShippingAddress.getFormattedName(context: Context): String {
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

private fun OriginShippingAddress.toShippingFromString() = StringBuilder()
    .appendWithIfNotEmpty(this.address1)
    .appendWithIfNotEmpty(this.address2)
    .appendWithIfNotEmpty(this.city)
    .appendWithIfNotEmpty(this.state)
    .appendWithIfNotEmpty(this.postcode)
    .toString()

@Preview
@Composable
private fun AddressSectionPortraitPreview() {
    WooThemeWithBackground {
        Box(modifier = Modifier.padding(dimensionResource(R.dimen.major_100))) {
            AddressSectionPortrait(
                shippingAddresses = WooShippingAddresses(
                    shipFrom = ShippingLabelSampleData.getShipFrom(),
                    shipTo = ShippingLabelSampleData.getShipTo(),
                    originAddresses = listOf(ShippingLabelSampleData.getShipFrom())
                ),
                onEditDestinationAddress = {},
                onEditOriginAddress = {},
                onOriginAddressSelected = {},
                isReadOnly = false,
                destinationStatus = AddressStatus.VERIFIED
            )
        }
    }
}

@Preview
@Composable
private fun AddressSectionPortraitMissingAddressPreview() {
    WooThemeWithBackground {
        Box(modifier = Modifier.padding(dimensionResource(R.dimen.major_100))) {
            AddressSectionPortrait(
                shippingAddresses = WooShippingAddresses(
                    shipFrom = ShippingLabelSampleData.getShipFrom(),
                    shipTo = DestinationShippingAddress.EMPTY,
                    originAddresses = listOf(ShippingLabelSampleData.getShipFrom())
                ),
                onEditDestinationAddress = {},
                onEditOriginAddress = {},
                onOriginAddressSelected = {},
                isReadOnly = false,
                destinationStatus = AddressStatus.VERIFIED
            )
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
                    shipFrom = ShippingLabelSampleData.getShipFrom(),
                    shipTo = ShippingLabelSampleData.getShipTo(),
                    originAddresses = listOf(ShippingLabelSampleData.getShipFrom())
                ),
                isReadOnly = false,
                onEditDestinationAddress = {},
                onEditOriginAddress = {},
                onOriginAddressSelected = {},
                destinationStatus = AddressStatus.VERIFIED
            )
        }
    }
}

@Preview(widthDp = 750, heightDp = 100)
@Composable
private fun AddressSectionLandscapeMissingAddressPreview() {
    WooThemeWithBackground {
        Box(modifier = Modifier.padding(dimensionResource(R.dimen.major_100))) {
            AddressSectionLandscape(
                shippingAddresses = WooShippingAddresses(
                    shipFrom = ShippingLabelSampleData.getShipFrom(),
                    shipTo = DestinationShippingAddress.EMPTY,
                    originAddresses = listOf(ShippingLabelSampleData.getShipFrom())
                ),
                isReadOnly = false,
                onEditDestinationAddress = {},
                onEditOriginAddress = {},
                onOriginAddressSelected = {},
                destinationStatus = AddressStatus.VERIFIED
            )
        }
    }
}
