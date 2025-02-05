package com.woocommerce.android.ui.orders.wooshippinglabels

import android.content.res.Configuration
import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetScaffoldDefaults
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.BottomSheetState
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetDefaults
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.modifiers.dashedBorder
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.CustomsState
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.CustomsState.NotRequired
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.CustomsState.Unavailable
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.PackageSelectionState
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.PackageSelectionState.DataAvailable
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.PackageSelectionState.NotSelected
import com.woocommerce.android.ui.orders.wooshippinglabels.address.AddressSelection
import com.woocommerce.android.ui.orders.wooshippinglabels.address.getShipFrom
import com.woocommerce.android.ui.orders.wooshippinglabels.address.getShipTo
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.ShippingRateUI
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.ShippingRatesSection
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.ShippingSortOption
import kotlinx.parcelize.Parcelize

@Composable
fun WooShippingLabelCreationScreen(viewModel: WooShippingLabelCreationViewModel) {
    when (val viewState = viewModel.viewState.collectAsState().value) {
        WooShippingLabelCreationViewModel.WooShippingViewState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is WooShippingLabelCreationViewModel.WooShippingViewState.DataState -> {
            WooShippingLabelCreationScreen(
                onSelectPackageClick = viewModel::onSelectPackageClicked,
                onPurchaseShippingLabel = viewModel::onPurchaseShippingLabel,
                shippableItems = viewState.shippableItems,
                shippingLines = viewState.shippingLines,
                shippingAddresses = viewState.shippingAddresses,
                shippingRatesState = viewState.shippingRates,
                packageSelectionState = viewState.packageSelection,
                customsState = viewState.customsState,
                onShippingFromAddressChange = viewModel::onShippingFromAddressChange,
                onEditOriginAddress = viewModel::onEditOriginAddress,
                onSelectedRateSortOrderChanged = viewModel::onSelectedRateSortOrderChanged,
                onRefreshShippingRates = viewModel::onRefreshShippingRates,
                onSelectedSippingRateChanged = viewModel::onSelectedSippingRateChanged,
                customWeight = viewModel.customWeight,
                onCustomWeightChange = viewModel::onCustomWeightChange,
                uiState = viewState.uiState,
                onMarkOrderCompleteChange = viewModel::onMarkOrderCompleteChange,
                onNavigateBack = viewModel::onNavigateBack,
                purchaseState = viewState.purchaseState,
                onShipmentDetailsExpandedChange = viewModel::onShipmentDetailsExpandedChange,
                onSelectAddressExpandedChange = viewModel::onSelectAddressExpandedChange,
                onEditCustomsClick = viewModel::onEditCustomsClick
            )
        }

        WooShippingLabelCreationViewModel.WooShippingViewState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error")
            }
        }
    }
}

@Composable
fun WooShippingLabelCreationScreen(
    shippableItems: ShippableItemsUI,
    shippingLines: List<ShippingLineSummaryUI>,
    shippingRatesState: WooShippingLabelCreationViewModel.ShippingRatesState,
    packageSelectionState: PackageSelectionState,
    shippingAddresses: WooShippingAddresses,
    customsState: CustomsState,
    onShippingFromAddressChange: (OriginShippingAddress) -> Unit,
    onEditOriginAddress: (OriginShippingAddress) -> Unit,
    onSelectPackageClick: () -> Unit,
    onPurchaseShippingLabel: () -> Unit,
    onSelectedRateSortOrderChanged: (ShippingSortOption) -> Unit,
    onRefreshShippingRates: () -> Unit,
    onCustomWeightChange: (String) -> Unit,
    onSelectedSippingRateChanged: (rate: ShippingRateUI) -> Unit,
    customWeight: String,
    uiState: WooShippingLabelCreationViewModel.UIControlsState,
    onMarkOrderCompleteChange: (Boolean) -> Unit,
    onShipmentDetailsExpandedChange: (Boolean) -> Boolean,
    onSelectAddressExpandedChange: (Boolean) -> Boolean,
    purchaseState: WooShippingLabelCreationViewModel.PurchaseState,
    onEditCustomsClick: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shipmentDetailsValue = if (uiState.isShipmentDetailsExpanded) {
        BottomSheetValue.Expanded
    } else {
        BottomSheetValue.Collapsed
    }

    val shipFromSelectionBottomSheetValue = if (uiState.isAddressSelectionExpanded) {
        ModalBottomSheetValue.Expanded
    } else {
        ModalBottomSheetValue.Hidden
    }

    val shipFromSelectionBottomSheetState = ModalBottomSheetState(
        density = LocalDensity.current,
        initialValue = shipFromSelectionBottomSheetValue,
        animationSpec = ModalBottomSheetDefaults.AnimationSpec,
        isSkipHalfExpanded = true,
        confirmValueChange = {
            onSelectAddressExpandedChange(it == ModalBottomSheetValue.Expanded)
        }
    )

    val shipmentDetailsBottomSheetState = BottomSheetState(
        initialValue = shipmentDetailsValue,
        animationSpec = BottomSheetScaffoldDefaults.AnimationSpec,
        density = LocalDensity.current,
        confirmValueChange = {
            onShipmentDetailsExpandedChange(it == BottomSheetValue.Expanded)
        }
    )

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = shipmentDetailsBottomSheetState
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LabelCreationScreenWithBottomSheet(
            shippableItems = shippableItems,
            modifier = modifier,
            onSelectPackageClick = onSelectPackageClick,
            scaffoldState = scaffoldState,
            shippingLines = shippingLines,
            shippingAddresses = shippingAddresses,
            customsState = customsState,
            shippingRatesState = shippingRatesState,
            packageSelectionState = packageSelectionState,
            onShippingFromAddressChange = onShippingFromAddressChange,
            onEditOriginAddress = onEditOriginAddress,
            onSelectedRateSortOrderChanged = onSelectedRateSortOrderChanged,
            onRefreshShippingRates = onRefreshShippingRates,
            customWeight = customWeight,
            onCustomWeightChange = onCustomWeightChange,
            onSelectedSippingRateChanged = onSelectedSippingRateChanged,
            uiState = uiState,
            onNavigateBack = onNavigateBack,
            onMarkOrderCompleteChange = onMarkOrderCompleteChange,
            shipFromSelectionBottomSheetState = shipFromSelectionBottomSheetState,
            onShipmentDetailsExpandedChange = onShipmentDetailsExpandedChange,
            onEditCustomsClick = onEditCustomsClick
        )
        val isDarkTheme = isSystemInDarkTheme()
        val isCollapsed = scaffoldState.bottomSheetState.isCollapsed
        val elevation = when {
            isDarkTheme && isCollapsed -> 7.dp
            !isDarkTheme && isCollapsed -> 0.dp
            isDarkTheme && !isCollapsed -> 16.dp
            else -> 8.dp
        }
        if (shippingRatesState is WooShippingLabelCreationViewModel.ShippingRatesState.DataState) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Surface(elevation = elevation) {
                    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        PurchasesSectionLandscape(
                            total = shippingRatesState.selectedRate?.selectedOption?.formatedPrice,
                            markOrderComplete = uiState.markOrderComplete,
                            onMarkOrderCompleteChange = onMarkOrderCompleteChange,
                            onPurchaseShippingLabel = onPurchaseShippingLabel
                        )
                    } else {
                        PurchaseButton(
                            total = shippingRatesState.selectedRate?.selectedOption?.formatedPrice,
                            onPurchaseShippingLabel = onPurchaseShippingLabel
                        )
                    }
                }
            }
        }
        if (purchaseState is WooShippingLabelCreationViewModel.PurchaseState.InProgress) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {}
                    )
                    .background(color = MaterialTheme.colors.surface.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun LabelCreationScreenWithBottomSheet(
    shippableItems: ShippableItemsUI,
    shippingLines: List<ShippingLineSummaryUI>,
    shippingRatesState: WooShippingLabelCreationViewModel.ShippingRatesState,
    packageSelectionState: PackageSelectionState,
    customsState: CustomsState,
    onSelectPackageClick: () -> Unit,
    shippingAddresses: WooShippingAddresses,
    onEditOriginAddress: (OriginShippingAddress) -> Unit,
    onShippingFromAddressChange: (OriginShippingAddress) -> Unit,
    onSelectedRateSortOrderChanged: (ShippingSortOption) -> Unit,
    onRefreshShippingRates: () -> Unit,
    customWeight: String,
    onCustomWeightChange: (String) -> Unit,
    onSelectedSippingRateChanged: (rate: ShippingRateUI) -> Unit,
    uiState: WooShippingLabelCreationViewModel.UIControlsState,
    scaffoldState: BottomSheetScaffoldState,
    shipFromSelectionBottomSheetState: ModalBottomSheetState,
    onMarkOrderCompleteChange: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    onShipmentDetailsExpandedChange: (Boolean) -> Boolean,
    onEditCustomsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPurchaseButtonDisplayed = shippingRatesState is WooShippingLabelCreationViewModel.ShippingRatesState.DataState
    val bottomSheetPeekHeight = if (isPurchaseButtonDisplayed) 132.dp else 76.dp
    val paddingBottom = if (isPurchaseButtonDisplayed) 72.dp else 0.dp
    val shippingRateSummary =
        (shippingRatesState as? WooShippingLabelCreationViewModel.ShippingRatesState.DataState)?.selectedRate?.summary

    BottomSheetScaffold(
        sheetContent = {
            AddressSelection(
                shipFrom = shippingAddresses.shipFrom,
                originAddresses = shippingAddresses.originAddresses,
                onShippingFromAddressChange = onShippingFromAddressChange,
                modalBottomSheetState = shipFromSelectionBottomSheetState,
                modifier = Modifier.padding(bottom = paddingBottom),
                onEditOriginAddress = onEditOriginAddress
            ) {
                ShipmentDetails(
                    shippableItems = shippableItems,
                    shippingLines = shippingLines,
                    shipFromSelectionBottomSheetState = shipFromSelectionBottomSheetState,
                    onMarkOrderCompleteChange = onMarkOrderCompleteChange,
                    shippingAddresses = shippingAddresses,
                    shippingRateSummary = shippingRateSummary,
                    scaffoldState = scaffoldState,
                    isShipmentDetailsExpanded = uiState.isShipmentDetailsExpanded,
                    markOrderComplete = uiState.markOrderComplete,
                    onShipmentDetailsExpandedChange = onShipmentDetailsExpandedChange
                )
            }
        },
        sheetPeekHeight = bottomSheetPeekHeight,
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.shipping_label_create_title)) },
                navigationIcon = {
                    IconButton(onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
                backgroundColor = colorResource(id = R.color.color_toolbar),
                elevation = 0.dp,
            )
        },
    ) { innerPadding ->
        Surface(
            modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier.verticalScroll(rememberScrollState())) {
                val isExpanded = remember { mutableStateOf(false) }
                ShippingProductsCard(
                    shippableItems = shippableItems,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    isExpanded = isExpanded.value,
                    onExpand = { isExpanded.value = it }
                )
                HazmatCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 8.dp)
                )
                CustomsCard(
                    customsState = customsState,
                    onEditCustomsClick = onEditCustomsClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
                PackageCard(
                    modifier = Modifier.padding(16.dp),
                    packageSelectionState = packageSelectionState,
                    onSelectPackageClick = onSelectPackageClick,
                    customWeight = customWeight,
                    onCustomWeightChange = onCustomWeightChange
                )
                ShippingRatesSection(
                    shippingRatesState = shippingRatesState,
                    onSelectedRateSortOrderChanged = onSelectedRateSortOrderChanged,
                    onRefreshShippingRates = onRefreshShippingRates,
                    onSelectedSippingRateChanged = onSelectedSippingRateChanged
                )
            }
        }
    }
}

@Composable
internal fun HazmatCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Row(modifier = modifier.clickable { onClick() }) {
        Text(
            text = stringResource(R.string.shipping_label_hazmat_title),
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(dimensionResource(id = R.dimen.major_100))
                .align(Alignment.CenterVertically)
        )

        Text(
            text = stringResource(R.string.no),
            style = MaterialTheme.typography.subtitle1,
            color = colorResource(id = R.color.color_on_surface_medium),
            modifier = Modifier
                .align(Alignment.CenterVertically)
        )

        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            tint = colorResource(id = R.color.color_on_surface_medium),
            contentDescription =
            stringResource(id = R.string.shipping_label_package_details_items_expand_content_description),
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(end = dimensionResource(R.dimen.minor_50))
        )
    }
}

@Composable
private fun CustomsCard(
    customsState: CustomsState,
    onEditCustomsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (customsState !is NotRequired) {
        Row(
            modifier = modifier
                .background(
                    color = MaterialTheme.colors.surface,
                    shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))
                )
                .border(
                    width = dimensionResource(R.dimen.minor_10),
                    color = colorResource(R.color.divider_color),
                    shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))
                )
                .padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.shipping_labels_customs_title),
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onSurface,
                textAlign = TextAlign.Start,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(1f)
            )
            Box(
                modifier = Modifier
                    .background(
                        color = colorResource(id = R.color.woo_red_20),
                        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_medium))
                    )
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = stringResource(id = R.string.shipping_labels_customs_missing_info_badge),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            IconButton(
                onClick = onEditCustomsClick,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    tint = colorResource(id = R.color.color_icon_menu),
                    contentDescription = stringResource(id = R.string.shipping_label_package_selected_description)
                )
            }
        }
    }
}

@Composable
private fun PackageCard(
    packageSelectionState: PackageSelectionState,
    customWeight: String,
    onSelectPackageClick: () -> Unit,
    onCustomWeightChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (packageSelectionState) {
        is NotSelected -> SelectPackageCard(
            modifier = modifier,
            onSelectPackageClick = onSelectPackageClick
        )

        is DataAvailable -> PackageSelectionAvailableCard(
            modifier = modifier,
            packageData = packageSelectionState.selectedPackage,
            onSelectPackageClick = onSelectPackageClick,
            defaultWeight = packageSelectionState.defaultWeight,
            customWeight = customWeight,
            customWeightUnit = packageSelectionState.weightUnit,
            onCustomWeightChange = onCustomWeightChange
        )
    }
}

@Composable
private fun SelectPackageCard(
    onSelectPackageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colors.surface,
                shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))
            )
            .dashedBorder(
                color = colorResource(R.color.divider_color),
                strokeWidth = 2.dp,
                dashLength = 8.dp,
                gapLength = 8.dp,
                shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_large))
            )
            .padding(dimensionResource(id = R.dimen.major_200))
    ) {
        WCColoredButton(
            onClick = onSelectPackageClick,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
        ) {
            Text(stringResource(R.string.shipping_label_select_package_button))
        }
        Text(
            text = stringResource(R.string.shipping_label_select_package_title),
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.onSurface,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .padding(top = dimensionResource(id = R.dimen.major_200))
                .align(Alignment.CenterHorizontally)
        )
        Text(
            text = stringResource(R.string.shipping_label_select_package_description),
            style = MaterialTheme.typography.subtitle1,
            color = colorResource(id = R.color.color_on_surface_medium),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = dimensionResource(id = R.dimen.minor_100))
                .align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun PackageSelectionAvailableCard(
    packageData: PackageData,
    defaultWeight: String,
    customWeight: String,
    customWeightUnit: String,
    onSelectPackageClick: () -> Unit,
    onCustomWeightChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.background(color = MaterialTheme.colors.surface)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = R.string.shipping_label_package_selected_title),
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(
                onClick = onSelectPackageClick
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    tint = colorResource(id = R.color.color_icon_menu),
                    contentDescription = stringResource(id = R.string.shipping_label_package_selected_description)
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = colorResource(id = R.color.divider_color),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(dimensionResource(id = R.dimen.major_125)),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = packageData.groupName
                            ?.takeIf { it.isNotEmpty() }
                            ?: stringResource(id = packageData.descriptionResId),
                        style = MaterialTheme.typography.caption,
                        color = colorResource(id = R.color.color_on_surface_disabled)
                    )
                    Text(
                        text = packageData.name
                            .takeIf { it.isNotEmpty() }
                            ?: stringResource(id = R.string.shipping_label_package_default_name),
                        style = MaterialTheme.typography.body1
                    )
                    Text(
                        text = packageData.weight
                            .takeIf { it.isNotEmpty() }
                            ?.let { "${packageData.dimensionForDisplay} • ${packageData.weightForDisplay}" }
                            ?: packageData.dimensionForDisplay,
                        style = MaterialTheme.typography.body2
                    )
                }

                if (packageData.isPredefined) {
                    Icon(
                        tint = colorResource(id = R.color.woo_yellow_20),
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Star",
                    )
                } else {
                    Icon(
                        tint = colorResource(id = R.color.color_on_surface_disabled),
                        imageVector = Icons.Outlined.Star,
                        contentDescription = "Star",
                    )
                }
            }
        }
        Text(
            modifier = Modifier.padding(
                top = dimensionResource(id = R.dimen.major_100),
                bottom = dimensionResource(id = R.dimen.minor_100)
            ),
            text = stringResource(id = R.string.shipping_label_total_shipment_weight),
            style = MaterialTheme.typography.body2
        )
        RoundedCornerBoxWithBorder {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = customWeight,
                        onValueChange = onCustomWeightChange,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onSurface),

                    )
                    if (customWeight.isEmpty()) {
                        Text(
                            text = defaultWeight,
                            style = MaterialTheme.typography.body2,
                            color = colorResource(id = R.color.color_on_surface_disabled)
                        )
                    }
                }
                Text(
                    text = customWeightUnit,
                    style = MaterialTheme.typography.body2,
                    color = colorResource(id = R.color.color_on_surface_disabled)
                )
            }
        }
    }
}

@Parcelize
data class ShippableItemUI(
    val itemId: Long,
    val productId: Long,
    val title: String,
    val formattedSize: String,
    val formattedWeight: String,
    val formattedPrice: String,
    val quantity: Float,
    val imageUrl: String? = null
) : Parcelable

@Parcelize
data class ShippableItemsUI(
    val shippableItems: List<ShippableItemUI>,
    val formattedTotalWeight: String,
    val formattedTotalPrice: String
) : Parcelable

@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.PIXEL)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO, device = Devices.PIXEL)
@Composable
private fun WooShippingLabelCreationScreenPreview() {
    WooThemeWithBackground {
        WooShippingLabelCreationScreen(
            shippableItems = ShippableItemsUI(
                shippableItems = generateItems(6),
                formattedTotalWeight = "8.5kg",
                formattedTotalPrice = "$92.78"
            ),
            shippingLines = getShippingLines(),
            modifier = Modifier.fillMaxSize(),
            onSelectPackageClick = {},
            onPurchaseShippingLabel = {},
            shippingAddresses = WooShippingAddresses(
                shipFrom = getShipFrom(),
                shipTo = getShipTo(),
                originAddresses = listOf(getShipFrom())
            ),
            shippingRatesState = WooShippingLabelCreationViewModel.ShippingRatesState.NoAvailable,
            packageSelectionState = NotSelected,
            customsState = Unavailable,
            onShippingFromAddressChange = {},
            onRefreshShippingRates = {},
            onSelectedRateSortOrderChanged = {},
            customWeight = "",
            onCustomWeightChange = {},
            onSelectedSippingRateChanged = {},
            onMarkOrderCompleteChange = {},
            onNavigateBack = {},
            onEditOriginAddress = {},
            purchaseState = WooShippingLabelCreationViewModel.PurchaseState.NoStarted,
            uiState = WooShippingLabelCreationViewModel.UIControlsState(
                markOrderComplete = false,
                isShipmentDetailsExpanded = false,
                isAddressSelectionExpanded = false
            ),
            onShipmentDetailsExpandedChange = { true },
            onSelectAddressExpandedChange = { true },
            onEditCustomsClick = {}
        )
    }
}

@Preview
@Composable
private fun HazmatCardPreview() {
    WooThemeWithBackground {
        HazmatCard(modifier = Modifier.padding(16.dp))
    }
}

@Preview
@Composable
private fun PackageNotSelectedPreview() {
    WooThemeWithBackground {
        PackageCard(
            modifier = Modifier.padding(16.dp),
            packageSelectionState = NotSelected,
            customWeight = "",
            onSelectPackageClick = {},
            onCustomWeightChange = {}
        )
    }
}

@Preview
@Composable
private fun PackageSelectedPreview() {
    WooThemeWithBackground {
        PackageCard(
            modifier = Modifier.padding(16.dp),
            packageSelectionState = DataAvailable(
                selectedPackage = PackageData(
                    name = "Package 1",
                    dimensions = "10 x 10 x 10",
                    weight = "1.5",
                    isSelected = true,
                    isLetter = false,
                    id = "1",
                ),
                defaultWeight = "1",
                weightUnit = "kg",
            ),
            customWeight = "",
            onSelectPackageClick = {},
            onCustomWeightChange = {}
        )
    }
}
