package com.woocommerce.android.ui.orders.wooshippinglabels

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.BottomSheetState
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.preview.OrientationPreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.wooshippinglabels.address.AddressSectionLandscape
import com.woocommerce.android.ui.orders.wooshippinglabels.address.AddressSectionPortrait
import com.woocommerce.android.ui.orders.wooshippinglabels.address.AddressStatus
import com.woocommerce.android.ui.orders.wooshippinglabels.components.NoticeBanner
import com.woocommerce.android.ui.orders.wooshippinglabels.components.NoticeBannerUiState
import com.woocommerce.android.ui.orders.wooshippinglabels.models.DestinationShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.util.StringUtils
import kotlinx.coroutines.launch

@Composable
fun ShipmentDetails(
    bottomSheetState: BottomSheetState,
    totalItems: Int,
    totalItemsCost: String,
    shippingLines: List<ShippingLineSummaryUI>,
    shippingAddresses: WooShippingAddresses,
    shipmentCostUI: ShipmentCostUI?,
    paymentsSectionUI: PaymentsSectionUI,
    purchaseSectionUI: PurchaseSectionUI,
    modifier: Modifier = Modifier,
    noticeBannerUiState: NoticeBannerUiState? = null,
    onEditDestinationAddress: (DestinationShippingAddress) -> Unit,
    onEditOriginAddress: (OriginShippingAddress) -> Unit,
    onOriginAddressSelected: (OriginShippingAddress) -> Unit,
    destinationStatus: AddressStatus,
    readOnly: Boolean,
    onPeekHeightChanged: (Dp) -> Unit
) {
    val expandProgress = bottomSheetState.progress(
        from = BottomSheetValue.Collapsed,
        to = BottomSheetValue.Expanded
    ).let {
        if (it == 1f && bottomSheetState.isCollapsed &&
            bottomSheetState.targetValue == BottomSheetValue.Collapsed
        ) {
            // Sometimes the progress is 1f at the end of the collapse drag, we want to reset it to 0f
            0f
        } else {
            it
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val toggleSheet: () -> Unit = {
        coroutineScope.launch {
            if (bottomSheetState.isCollapsed) {
                bottomSheetState.expand()
            } else {
                bottomSheetState.collapse()
            }
        }
    }
    val density = LocalDensity.current

    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    var expandableContentHeight by remember { mutableIntStateOf(0) }
    var sheetHandleHeight by remember { mutableIntStateOf(0) }
    var topSectionHeight by remember { mutableIntStateOf(0) }
    var purchaseSectionHeight by remember { mutableIntStateOf(0) }

    val collapsedContentHeight by remember(purchaseSectionUI.isVisible) {
        derivedStateOf {
            sheetHandleHeight + topSectionHeight + if (purchaseSectionUI.isVisible) purchaseSectionHeight else 0
        }
    }

    BackHandler(enabled = bottomSheetState.isExpanded) {
        coroutineScope.launch { bottomSheetState.collapse() }
    }

    LaunchedEffect(collapsedContentHeight) {
        val peekHeight = with(density) {
            collapsedContentHeight.toDp()
        }
        onPeekHeightChanged(peekHeight)
    }

    Column(modifier.fillMaxHeight()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { sheetHandleHeight = it.height }
                .clickable(
                    onClick = toggleSheet,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                )
        ) {
            Icon(
                painter = if (expandProgress > 0.5f) {
                    painterResource(R.drawable.ic_arrow_down_26)
                } else {
                    painterResource(R.drawable.ic_arrow_up_26)
                },
                contentDescription = stringResource(R.string.order_creation_expand_collapse_order_totals),
                tint = colorResource(id = R.color.color_primary),
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        Box(
            Modifier
                .onSizeChanged { expandableContentHeight = it.height }
                .weight(1f)
        ) {
            if (expandProgress < 1f) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .onSizeChanged { topSectionHeight = it.height }
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 16.dp)
                        .fillMaxWidth()
                        .alpha(1 - expandProgress)
                ) {
                    Text(
                        text = stringResource(R.string.shipping_label_shipment_details_title),
                        color = MaterialTheme.colors.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                onClick = toggleSheet,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            )
                    )
                    NoticeBanner(noticeBannerUiState)
                }
            }

            if (expandProgress > 0f) {
                Column(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .alpha(expandProgress)
                ) {
                    if (isLandscape) {
                        ShipmentDetailsLandscape(
                            totalItems = totalItems,
                            totalItemsCost = totalItemsCost,
                            shippingLines = shippingLines,
                            shippingAddresses = shippingAddresses,
                            shipmentCostUI = shipmentCostUI,
                            paymentsSectionUI = paymentsSectionUI,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            readOnly = readOnly,
                            onEditDestinationAddress = onEditDestinationAddress,
                            onEditOriginAddress = onEditOriginAddress,
                            onOriginAddressSelected = onOriginAddressSelected,
                            destinationStatus = destinationStatus
                        )
                    } else {
                        ShipmentDetailsPortrait(
                            totalItems = totalItems,
                            totalItemsCost = totalItemsCost,
                            shippingLines = shippingLines,
                            shippingAddresses = shippingAddresses,
                            shipmentCostUI = shipmentCostUI,
                            paymentsSectionUI = paymentsSectionUI,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            readOnly = readOnly,
                            onEditDestinationAddress = onEditDestinationAddress,
                            onEditOriginAddress = onEditOriginAddress,
                            onOriginAddressSelected = onOriginAddressSelected,
                            destinationStatus = destinationStatus
                        )
                    }

                    Divider()
                }
            }
        }

        PurchaseSection(
            state = purchaseSectionUI,
            orderCompleteToggleVisible = isLandscape || bottomSheetState.isExpanded,
            modifier = Modifier
                .onSizeChanged { purchaseSectionHeight = it.height }
                .graphicsLayer(
                    translationY = (expandProgress - 1) * (expandableContentHeight - topSectionHeight)
                )
                .fillMaxWidth()
                .background(MaterialTheme.colors.surface)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun ShipmentDetailsPortrait(
    totalItems: Int,
    totalItemsCost: String,
    shippingLines: List<ShippingLineSummaryUI>,
    shippingAddresses: WooShippingAddresses,
    shipmentCostUI: ShipmentCostUI?,
    paymentsSectionUI: PaymentsSectionUI,
    onEditDestinationAddress: (DestinationShippingAddress) -> Unit,
    onEditOriginAddress: (OriginShippingAddress) -> Unit,
    onOriginAddressSelected: (OriginShippingAddress) -> Unit,
    destinationStatus: AddressStatus,
    modifier: Modifier = Modifier,
    readOnly: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        OrderDetailsSection(
            shippingAddresses = shippingAddresses,
            totalItems = totalItems,
            totalItemsCost = totalItemsCost,
            shippingLines = shippingLines,
            isReadOnly = readOnly,
            onEditDestinationAddress = onEditDestinationAddress,
            onEditOriginAddress = onEditOriginAddress,
            onOriginAddressSelected = onOriginAddressSelected,
            destinationStatus = destinationStatus
        )
        Divider()
        if (!readOnly) {
            PaymentSection(paymentsSectionUI = paymentsSectionUI)
            Divider()
        }
        ShipmentCostSection(shipmentCostUI = shipmentCostUI)
    }
}

@Composable
private fun ShipmentDetailsLandscape(
    totalItems: Int,
    totalItemsCost: String,
    shippingLines: List<ShippingLineSummaryUI>,
    shippingAddresses: WooShippingAddresses,
    shipmentCostUI: ShipmentCostUI?,
    paymentsSectionUI: PaymentsSectionUI,
    onEditDestinationAddress: (DestinationShippingAddress) -> Unit,
    onEditOriginAddress: (OriginShippingAddress) -> Unit,
    onOriginAddressSelected: (OriginShippingAddress) -> Unit,
    destinationStatus: AddressStatus,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false
) {
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        AddressSectionLandscape(
            shippingAddresses = shippingAddresses,
            isReadOnly = readOnly,
            onEditDestinationAddress = onEditDestinationAddress,
            onEditOriginAddress = onEditOriginAddress,
            onOriginAddressSelected = onOriginAddressSelected,
            destinationStatus = destinationStatus
        )
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .fillMaxWidth()
        ) {
            OrderDetailsSectionLandscape(
                totalItems = totalItems,
                totalItemsCost = totalItemsCost,
                shippingLines = shippingLines,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            )
            VerticalDivider(Modifier.padding(top = 16.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (!readOnly) {
                    PaymentSection(
                        paymentsSectionUI = paymentsSectionUI,
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                            .padding(start = 16.dp)
                    )
                }
                Divider()
                ShipmentCostSection(
                    shipmentCostUI = shipmentCostUI,
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .padding(start = 16.dp)
                )
            }
        }
    }
}

@Composable
fun ShipmentDetailsSectionTitle(
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
    shippingAddresses: WooShippingAddresses,
    totalItems: Int,
    totalItemsCost: String,
    shippingLines: List<ShippingLineSummaryUI>,
    onEditDestinationAddress: (DestinationShippingAddress) -> Unit,
    onEditOriginAddress: (OriginShippingAddress) -> Unit,
    onOriginAddressSelected: (OriginShippingAddress) -> Unit,
    destinationStatus: AddressStatus,
    modifier: Modifier = Modifier,
    isReadOnly: Boolean = false
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        ShipmentDetailsSectionTitle(
            title = stringResource(R.string.shipping_label_shipment_details_order_details)
        )
        AddressSectionPortrait(
            shippingAddresses = shippingAddresses,
            isReadOnly = isReadOnly,
            onEditDestinationAddress = onEditDestinationAddress,
            onEditOriginAddress = onEditOriginAddress,
            onOriginAddressSelected = onOriginAddressSelected,
            destinationStatus = destinationStatus
        )
        TotalCard(
            totalItems = totalItems,
            totalItemsCost = totalItemsCost,
            shippingLines = shippingLines,
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
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        ShipmentDetailsSectionTitle(
            title = stringResource(R.string.shipping_label_shipment_details_order_details),
            modifier = Modifier.padding(
                top = dimensionResource(R.dimen.major_100)
            )
        )
        TotalCard(
            totalItems = totalItems,
            totalItemsCost = totalItemsCost,
            shippingLines = shippingLines,
        )
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
            .padding(vertical = dimensionResource(R.dimen.minor_50)),
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
private fun PaymentSection(
    paymentsSectionUI: PaymentsSectionUI,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        ShipmentDetailsSectionTitle(
            title = stringResource(R.string.shipping_label_shipment_details_payment_method),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(onClick = paymentsSectionUI.onEditPaymentMethodClicked)
                .padding(vertical = 4.dp)
        ) {
            if (paymentsSectionUI.selectedPaymentMethod != null) {
                Text(
                    text = paymentsSectionUI.selectedPaymentMethod.cardTypeWithDigits,
                    style = MaterialTheme.typography.body1,
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_edit_filled_24dp),
                    contentDescription = stringResource(R.string.shipping_label_shipment_details_edit_payment_method),
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
            } else {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_add),
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.shipping_label_shipment_details_add_payment_method),
                    style = MaterialTheme.typography.body1
                )
            }
        }
    }
}

@Composable
private fun ShipmentCostSection(
    shipmentCostUI: ShipmentCostUI?,
    modifier: Modifier = Modifier
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = modifier) {
        ShipmentDetailsSectionTitle(
            title = stringResource(R.string.shipping_label_shipment_details_shipment_cost)
        )

        val serviceName = if (shipmentCostUI?.optionsWithFees?.isNotEmpty() == true) {
            stringResource(
                R.string.shipping_label_shipment_details_shipment_cost_base_fee,
                shipmentCostUI.serviceName
            )
        } else {
            shipmentCostUI?.serviceName
        }
        ShipmentCostRow(
            title = serviceName ?: stringResource(R.string.subtotal),
            total = shipmentCostUI?.formattedBasePrice
        )

        shipmentCostUI?.optionsWithFees?.forEach { (optionName, optionFee) ->
            ShipmentCostRow(
                title = optionName,
                total = optionFee
            )
        }

        ShipmentCostRow(
            title = stringResource(R.string.total),
            total = shipmentCostUI?.formattedTotalPrice,
            titleFontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ShipmentCostRow(
    title: String,
    total: String?,
    modifier: Modifier = Modifier,
    titleFontWeight: FontWeight = FontWeight.Normal
) {
    Row(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier.weight(1f),
            fontWeight = titleFontWeight
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

@LightDarkThemePreviews
@OrientationPreviews
@Composable
fun ShipmentDetailsExpandedPreview() {
    WooThemeWithBackground {
        Surface {
            ShipmentDetails(
                bottomSheetState = rememberBottomSheetState(
                    initialValue = BottomSheetValue.Expanded
                ),
                totalItems = 6,
                totalItemsCost = "$92.78",
                shippingLines = ShippingLabelSampleData.getShippingLines(),
                shippingAddresses = WooShippingAddresses(
                    shipFrom = ShippingLabelSampleData.getShipFrom(),
                    shipTo = ShippingLabelSampleData.getShipTo(),
                    originAddresses = listOf(ShippingLabelSampleData.getShipFrom())
                ),
                shipmentCostUI = null,
                paymentsSectionUI = ShippingLabelSampleData.getPaymentsSection(),
                purchaseSectionUI = ShippingLabelSampleData.getPurchaseSection(),
                modifier = Modifier.fillMaxSize(),
                noticeBannerUiState = null,
                onEditDestinationAddress = {},
                onEditOriginAddress = {},
                onOriginAddressSelected = {},
                destinationStatus = AddressStatus.Verified,
                readOnly = false,
                onPeekHeightChanged = {}
            )
        }
    }
}

@LightDarkThemePreviews
@OrientationPreviews
@Composable
private fun ShipmentDetailsCollapsedPreview() {
    WooThemeWithBackground {
        Surface {
            ShipmentDetails(
                bottomSheetState = rememberBottomSheetState(
                    initialValue = BottomSheetValue.Collapsed
                ),
                totalItems = 6,
                totalItemsCost = "$92.78",
                shippingLines = ShippingLabelSampleData.getShippingLines(),
                shippingAddresses = WooShippingAddresses(
                    shipFrom = ShippingLabelSampleData.getShipFrom(),
                    shipTo = ShippingLabelSampleData.getShipTo(),
                    originAddresses = listOf(ShippingLabelSampleData.getShipFrom())
                ),
                shipmentCostUI = null,
                paymentsSectionUI = ShippingLabelSampleData.getPaymentsSection(),
                purchaseSectionUI = ShippingLabelSampleData.getPurchaseSection(),
                modifier = Modifier.heightIn(max = 180.dp),
                noticeBannerUiState = null,
                onEditDestinationAddress = {},
                onEditOriginAddress = {},
                onOriginAddressSelected = {},
                destinationStatus = AddressStatus.Verified,
                readOnly = false,
                onPeekHeightChanged = {},
            )
        }
    }
}
