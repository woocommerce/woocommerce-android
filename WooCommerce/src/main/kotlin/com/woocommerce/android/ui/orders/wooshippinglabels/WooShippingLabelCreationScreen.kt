package com.woocommerce.android.ui.orders.wooshippinglabels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material.rememberBottomSheetState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.modifiers.dashedBorder
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.model.ShippingLabelHazmatCategory
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.CustomsState
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.CustomsState.ItnMissing
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.CustomsState.NotRequired
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.CustomsState.Unavailable
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.HazmatState.Declared
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.PackageSelectionState
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.PackageSelectionState.DataAvailable
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.PackageSelectionState.NotSelected
import com.woocommerce.android.ui.orders.wooshippinglabels.address.AddressStatus
import com.woocommerce.android.ui.orders.wooshippinglabels.components.ShipmentTabData
import com.woocommerce.android.ui.orders.wooshippinglabels.components.ShipmentsTabRow
import com.woocommerce.android.ui.orders.wooshippinglabels.components.ShippingLabelPurchaseStatusSection
import com.woocommerce.android.ui.orders.wooshippinglabels.components.ShippingLabelsSnackbar
import com.woocommerce.android.ui.orders.wooshippinglabels.components.ShippingLabelsSnackbarData
import com.woocommerce.android.ui.orders.wooshippinglabels.components.ShippingLabelsSnackbarVisuals
import com.woocommerce.android.ui.orders.wooshippinglabels.hazmat.HazmatCard
import com.woocommerce.android.ui.orders.wooshippinglabels.models.DestinationShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.WooShippingLabelPaperSize
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.components.ErrorMessageWithButton
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.ShippingRatesSection
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.ShippingSortOption
import kotlinx.coroutines.launch

@Composable
fun WooShippingLabelCreationScreen(viewModel: WooShippingLabelCreationViewModel) {
    when (val viewState = viewModel.viewState.collectAsState().value) {
        is WooShippingLabelCreationViewModel.WooShippingViewState.Loading -> {
            LoadingScreen(title = viewState.screenTitle, onNavigateBack = viewModel::onNavigateBack)
        }

        is WooShippingLabelCreationViewModel.WooShippingViewState.DataState -> {
            val context = LocalContext.current
            WooShippingLabelCreationScreen(
                onSelectPackageClick = viewModel::onSelectPackageClicked,
                shipmentUIList = viewState.shipmentUIList,
                shouldShowSplitShipmentButton = viewState.shouldShowSplitShipmentButton,
                totalItems = viewState.totalItems,
                totalItemsCost = viewState.totalItemsCost,
                shippingLines = viewState.shippingLines,
                paymentsSectionUI = viewState.paymentsSectionUI,
                purchaseSectionUI = viewState.purchaseSectionUI,
                shippingAddresses = viewState.shippingAddresses,
                onSelectedShipmentChanged = viewModel::onSelectedShipmentChanged,
                onOriginAddressSelected = viewModel::onOriginAddressSelected,
                onEditOriginAddress = viewModel::onEditOriginAddress,
                onSelectedRateSortOrderChanged = viewModel::onSelectedRateSortOrderChanged,
                onRefreshShippingRates = viewModel::onRefreshShippingRates,
                weightInputList = viewModel.weightInputsFlow.collectAsState().value,
                weightUnit = viewState.weightUnit,
                onWeightChange = viewModel::onWeightChange,
                uiState = viewState.uiState,
                onNavigateBack = viewModel::onNavigateBack,
                onEditCustomsClick = viewModel::onEditCustomsClick,
                onEditDestinationAddress = viewModel::onEditDestinationAddress,
                destinationStatus = viewState.destinationStatus,
                snackbarData = viewModel.snackbarData,
                onSplitShipment = viewModel::onSplitShipmentButtonTapped,
                onHazmatNoticeClick = viewModel::onHazmatNoticeClick,
                onLabelPaperSizeOptionSelected = viewModel::onLabelPaperSizeOptionSelected,
                onPrintShippingLabelClicked = viewModel::onPrintShippingLabelClicked,
                onTrackShipmentClicked = viewModel::onTrackShipmentClicked,
                onSchedulePickUpClicked = viewModel::onSchedulePickUpClicked,
                onRefundClicked = viewModel::onRefundClicked,
                onPrintCustomsClicked = { viewModel.onPrintCustomsClicked(context.filesDir) },
                onLearnMoreClicked = viewModel::onLearnMoreClicked,
            )
        }

        WooShippingLabelCreationViewModel.WooShippingViewState.Error -> {
            WooThemeWithBackground {
                ErrorScreen(
                    onNavigateBack = viewModel::onNavigateBack,
                    onRetryClick = viewModel::onRetry,
                )
            }
        }
    }
}

@Composable
fun WooShippingLabelCreationScreen(
    shipmentUIList: List<ShipmentUI>,
    shouldShowSplitShipmentButton: Boolean,
    totalItems: Int,
    totalItemsCost: String,
    shippingLines: List<ShippingLineSummaryUI>,
    paymentsSectionUI: PaymentsSectionUI,
    purchaseSectionUI: PurchaseSectionUI,
    shippingAddresses: List<WooShippingAddresses>,
    onSelectedShipmentChanged: (Int) -> Unit,
    onOriginAddressSelected: (OriginShippingAddress) -> Unit,
    onEditOriginAddress: (OriginShippingAddress) -> Unit,
    onSelectPackageClick: () -> Unit,
    onSelectedRateSortOrderChanged: (ShippingSortOption) -> Unit,
    onRefreshShippingRates: () -> Unit,
    weightInputList: List<WooShippingLabelCreationViewModel.WeightInput>,
    weightUnit: String,
    onWeightChange: (String) -> Unit,
    uiState: WooShippingLabelCreationViewModel.UIControlsState,
    onEditCustomsClick: () -> Unit,
    onNavigateBack: () -> Unit,
    onEditDestinationAddress: (DestinationShippingAddress) -> Unit,
    destinationStatus: AddressStatus,
    modifier: Modifier = Modifier,
    snackbarData: ShippingLabelsSnackbarData? = null,
    onSplitShipment: () -> Unit = {},
    onHazmatNoticeClick: () -> Unit = {},
    onLabelPaperSizeOptionSelected: (WooShippingLabelPaperSize) -> Unit,
    onPrintShippingLabelClicked: () -> Unit,
    onTrackShipmentClicked: () -> Unit,
    onSchedulePickUpClicked: () -> Unit,
    onRefundClicked: () -> Unit,
    onPrintCustomsClicked: () -> Unit,
    onLearnMoreClicked: () -> Unit,
) {
    val shipmentDetailsBottomSheetState = rememberBottomSheetState(
        initialValue = BottomSheetValue.Collapsed
    )

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = shipmentDetailsBottomSheetState
    )

    Box(modifier = Modifier.fillMaxSize()) {
        LabelCreationScreenWithBottomSheet(
            shipmentUIList = shipmentUIList,
            shouldShowSplitShipmentButton = shouldShowSplitShipmentButton,
            totalItems = totalItems,
            totalItemsCost = totalItemsCost,
            modifier = modifier,
            onSelectPackageClick = onSelectPackageClick,
            scaffoldState = scaffoldState,
            shippingLines = shippingLines,
            paymentsSectionUI = paymentsSectionUI,
            purchaseSectionUI = purchaseSectionUI,
            shippingAddresses = shippingAddresses,
            onSelectedShipmentChanged = onSelectedShipmentChanged,
            onOriginAddressSelected = onOriginAddressSelected,
            onEditOriginAddress = onEditOriginAddress,
            onSelectedRateSortOrderChanged = onSelectedRateSortOrderChanged,
            onRefreshShippingRates = onRefreshShippingRates,
            weightInputList = weightInputList,
            weightUnit = weightUnit,
            onWeightChange = onWeightChange,
            uiState = uiState,
            onNavigateBack = onNavigateBack,
            onEditCustomsClick = onEditCustomsClick,
            onEditDestinationAddress = onEditDestinationAddress,
            destinationStatus = destinationStatus,
            snackbarData = snackbarData,
            onSplitShipment = onSplitShipment,
            onHazmatNoticeClick = onHazmatNoticeClick,
            onLabelPaperSizeOptionSelected = onLabelPaperSizeOptionSelected,
            onPrintShippingLabelClicked = onPrintShippingLabelClicked,
            onTrackShipmentClicked = onTrackShipmentClicked,
            onSchedulePickUpClicked = onSchedulePickUpClicked,
            onRefundClicked = onRefundClicked,
            onPrintCustomsClicked = onPrintCustomsClicked,
            onLearnMoreClicked = onLearnMoreClicked,
        )

        val selectedShipment = shipmentUIList[uiState.selectedIndex]
        if (selectedShipment.isPurchaseAPILoading) {
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

@Suppress("CyclomaticComplexMethod")
@Composable
private fun LabelCreationScreenWithBottomSheet(
    shipmentUIList: List<ShipmentUI>,
    shouldShowSplitShipmentButton: Boolean,
    totalItems: Int,
    totalItemsCost: String,
    shippingLines: List<ShippingLineSummaryUI>,
    paymentsSectionUI: PaymentsSectionUI,
    purchaseSectionUI: PurchaseSectionUI,
    onSelectPackageClick: () -> Unit,
    shippingAddresses: List<WooShippingAddresses>,
    onSelectedShipmentChanged: (Int) -> Unit,
    onEditOriginAddress: (OriginShippingAddress) -> Unit,
    onOriginAddressSelected: (OriginShippingAddress) -> Unit,
    onSelectedRateSortOrderChanged: (ShippingSortOption) -> Unit,
    onRefreshShippingRates: () -> Unit,
    weightInputList: List<WooShippingLabelCreationViewModel.WeightInput>,
    weightUnit: String,
    onWeightChange: (String) -> Unit,
    uiState: WooShippingLabelCreationViewModel.UIControlsState,
    scaffoldState: BottomSheetScaffoldState,
    onNavigateBack: () -> Unit,
    onEditCustomsClick: () -> Unit,
    onEditDestinationAddress: (DestinationShippingAddress) -> Unit,
    destinationStatus: AddressStatus,
    modifier: Modifier = Modifier,
    snackbarData: ShippingLabelsSnackbarData? = null,
    onSplitShipment: () -> Unit = {},
    onHazmatNoticeClick: () -> Unit = {},
    onLabelPaperSizeOptionSelected: (WooShippingLabelPaperSize) -> Unit,
    onPrintShippingLabelClicked: () -> Unit,
    onTrackShipmentClicked: () -> Unit,
    onSchedulePickUpClicked: () -> Unit,
    onRefundClicked: () -> Unit,
    onPrintCustomsClicked: () -> Unit,
    onLearnMoreClicked: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val selectedShipment = shipmentUIList[uiState.selectedIndex]
    val selectedAddress = shippingAddresses[uiState.selectedIndex]

    var bottomSheetPeekHeight by remember { mutableStateOf(0.dp) }

    val screenTitle = if (shipmentUIList[uiState.selectedIndex].isPurchased) {
        R.string.shipping_label_print_screen_title
    } else {
        R.string.shipping_label_create_title
    }

    BottomSheetScaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                val visuals = data.visuals as ShippingLabelsSnackbarVisuals
                ShippingLabelsSnackbar(visuals = visuals, action = { data.performAction() })
            }
        },
        sheetContent = {
            ShipmentDetails(
                bottomSheetState = scaffoldState.bottomSheetState,
                totalItems = totalItems,
                totalItemsCost = totalItemsCost,
                shippingLines = shippingLines,
                shippingAddresses = selectedAddress,
                shipmentCostUI = selectedShipment.shipmentCostUI,
                paymentsSectionUI = paymentsSectionUI,
                purchaseSectionUI = purchaseSectionUI,
                onEditDestinationAddress = onEditDestinationAddress,
                onEditOriginAddress = onEditOriginAddress,
                onOriginAddressSelected = onOriginAddressSelected,
                destinationStatus = destinationStatus,
                noticeBannerUiState = uiState.noticeBannerUiState,
                readOnly = selectedShipment.isReadOnly,
                onPeekHeightChanged = { bottomSheetPeekHeight = it },
            )
        },
        sheetPeekHeight = bottomSheetPeekHeight,
        scaffoldState = scaffoldState,
        topBar = { TopBar(screenTitle, onNavigateBack) },
    ) { innerPadding ->
        Surface(
            modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier
                    .padding(bottom = 8.dp),
            ) {
                val scope = rememberCoroutineScope()
                val pagerState = rememberPagerState(initialPage = uiState.selectedIndex) { shipmentUIList.size }

                LaunchedEffect(pagerState.targetPage) {
                    onSelectedShipmentChanged(pagerState.targetPage)
                }

                if (shipmentUIList.size == 1 && shouldShowSplitShipmentButton) {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.products),
                            style = MaterialTheme.typography.h6,
                            modifier = Modifier.weight(1f)
                        )
                        if (shipmentUIList.first().totalItemQuantity > 1) {
                            Text(
                                text = stringResource(R.string.woo_shipping_split_shipment),
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier
                                    .clickable { onSplitShipment() }
                                    .padding(dimensionResource(R.dimen.minor_100))
                            )
                        }
                    }
                } else if (shipmentUIList.size > 1) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            ShipmentsTabRow(
                                shipmentTabs = shipmentUIList.mapIndexed { index, shipment ->
                                    ShipmentTabData(shipmentIndex = index + 1, isPurchased = shipment.isPurchased)
                                },
                                selectedTabIndex = if (pagerState.currentPage < pagerState.pageCount) {
                                    pagerState.currentPage
                                } else {
                                    0
                                },
                                onTabSelected = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                                modifier = Modifier.weight(1f)
                            )
                            if (shouldShowSplitShipmentButton) {
                                IconButton(
                                    onClick = onSplitShipment,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_edit_filled_24dp),
                                        tint = colorResource(id = R.color.color_icon_menu),
                                        contentDescription = stringResource(id = R.string.woo_shipping_split_shipment)
                                    )
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.Top,
                ) { page ->
                    CreateShippingCards(
                        shipmentUI = shipmentUIList[page],
                        onHazmatNoticeClick = onHazmatNoticeClick,
                        onEditCustomsClick = onEditCustomsClick,
                        onSelectPackageClick = onSelectPackageClick,
                        weight = weightInputList[uiState.selectedIndex].weight,
                        weightUnit = weightUnit,
                        onWeightChange = onWeightChange,
                        uiState = uiState,
                        onSelectedRateSortOrderChanged = onSelectedRateSortOrderChanged,
                        onRefreshShippingRates = onRefreshShippingRates,
                        onLabelPaperSizeOptionSelected = onLabelPaperSizeOptionSelected,
                        onPrintShippingLabelClicked = onPrintShippingLabelClicked,
                        onTrackShipmentClicked = onTrackShipmentClicked,
                        onSchedulePickUpClicked = onSchedulePickUpClicked,
                        onRefundClicked = onRefundClicked,
                        onPrintCustomsClicked = onPrintCustomsClicked,
                        onLearnMoreClicked = onLearnMoreClicked,
                    )
                }
            }

            val actionSnackbarMessage = snackbarData?.let { stringResource(it.message) }
            val actionSnackbarActionLabel = snackbarData?.let { stringResource(it.actionLabel) }

            LaunchedEffect(snackbarData) {
                snackbarData?.let {
                    val result = snackbarHostState.showSnackbar(
                        visuals = ShippingLabelsSnackbarVisuals(
                            message = actionSnackbarMessage.orEmpty(),
                            actionLabel = actionSnackbarActionLabel,
                            duration = snackbarData.duration,
                            hasSuccessCheckmark = it.hasIcon
                        )
                    )
                    when (result) {
                        SnackbarResult.ActionPerformed -> snackbarData.action()
                        SnackbarResult.Dismissed -> snackbarData.dismissAction()
                    }
                } ?: snackbarHostState.currentSnackbarData?.dismiss()
            }
        }
    }
}

@Composable
private fun CreateShippingCards(
    shipmentUI: ShipmentUI,
    onHazmatNoticeClick: () -> Unit = {},
    onEditCustomsClick: () -> Unit,
    onSelectPackageClick: () -> Unit,
    weight: String,
    weightUnit: String,
    onWeightChange: (String) -> Unit,
    uiState: WooShippingLabelCreationViewModel.UIControlsState,
    onSelectedRateSortOrderChanged: (ShippingSortOption) -> Unit,
    onRefreshShippingRates: () -> Unit,
    onLabelPaperSizeOptionSelected: (WooShippingLabelPaperSize) -> Unit,
    onPrintShippingLabelClicked: () -> Unit,
    onTrackShipmentClicked: () -> Unit,
    onSchedulePickUpClicked: () -> Unit,
    onRefundClicked: () -> Unit,
    onPrintCustomsClicked: () -> Unit,
    onLearnMoreClicked: () -> Unit,
) {
    Column {
        val isExpanded = remember { mutableStateOf(false) }

        ShippingLabelPurchaseStatusSection(
            labelPurchaseStatus = shipmentUI.labelPurchaseStatus,
            selectedLabelPaperSizeOption = uiState.paperSizeOption,
            onLabelPaperSizeOptionSelected = onLabelPaperSizeOptionSelected,
            onPrintShippingLabelClicked = onPrintShippingLabelClicked,
            onTrackShipmentClicked = onTrackShipmentClicked,
            onSchedulePickUpClicked = onSchedulePickUpClicked,
            onRefundClicked = onRefundClicked,
            onPrintCustomsClicked = onPrintCustomsClicked,
            onLearnMoreClicked = onLearnMoreClicked,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        ShippingProductsCard(
            shippableItems = shipmentUI,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
            isExpanded = isExpanded.value,
            onExpand = { isExpanded.value = it }
        )
        HazmatCard(
            onClick = if (shipmentUI.isReadOnly) null else onHazmatNoticeClick,
            selectedCategory = shipmentUI.hazmatState.hazmatSelection,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp)
        )
        if (!shipmentUI.isReadOnly) {
            CustomsCard(
                customsState = shipmentUI.customsState,
                onEditCustomsClick = onEditCustomsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            PackageCard(
                packageSelectionState = shipmentUI.packageSelectionState,
                onSelectPackageClick = onSelectPackageClick,
                weight = weight,
                weightUnit = weightUnit,
                onWeightChange = onWeightChange,
                modifier = Modifier.padding(16.dp),
            )
            ShippingRatesSection(
                shippingRatesState = shipmentUI.shippingRatesState,
                onSelectedRateSortOrderChanged = onSelectedRateSortOrderChanged,
                onRefreshShippingRates = onRefreshShippingRates
            )
        }
    }
}

@Composable
private fun TopBar(title: Int = R.string.shipping_label_create_title, onNavigateBack: () -> Unit) = TopAppBar(
    title = { Text(stringResource(title)) },
    navigationIcon = {
        IconButton(onNavigateBack) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_back_24dp),
                contentDescription = stringResource(id = R.string.back)
            )
        }
    },
    backgroundColor = colorResource(id = R.color.color_toolbar),
    elevation = 0.dp,
)

@Composable
private fun CustomsCard(
    customsState: CustomsState,
    onEditCustomsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val missingCustomData = customsState is Unavailable || customsState is ItnMissing
    val (backgroundColor, labelText) = if (missingCustomData) {
        Pair(
            colorResource(id = R.color.woo_red_20),
            stringResource(id = R.string.shipping_labels_customs_missing_info_badge)
        )
    } else {
        Pair(
            colorResource(id = R.color.woo_green_20),
            stringResource(id = R.string.shipping_labels_customs_completed_badge)
        )
    }

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
                        color = backgroundColor,
                        shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_medium))
                    )
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = labelText,
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
                    imageVector = ImageVector.vectorResource(R.drawable.ic_edit_filled_24dp),
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
    onSelectPackageClick: () -> Unit,
    weight: String,
    weightUnit: String,
    onWeightChange: (String) -> Unit,
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
            weight = weight,
            weightUnit = weightUnit,
            onWeightChange = onWeightChange
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
                strokeWidth = 1.dp,
                dashLength = 4.dp,
                gapLength = 6.dp,
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
    onSelectPackageClick: () -> Unit,
    weight: String,
    weightUnit: String,
    onWeightChange: (String) -> Unit,
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
                    imageVector = ImageVector.vectorResource(R.drawable.ic_edit_filled_24dp),
                    tint = colorResource(id = R.color.color_icon_menu),
                    contentDescription = stringResource(id = R.string.shipping_label_package_selected_description)
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onSelectPackageClick)
                .background(color = colorResource(R.color.light_colored_button_background))
                .padding(16.dp),
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

                if (packageData.isStarred) {
                    Icon(
                        tint = colorResource(id = R.color.color_on_surface_disabled),
                        imageVector = ImageVector.vectorResource(R.drawable.ic_star_filled_24dp),
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
                        value = weight,
                        onValueChange = onWeightChange,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onSurface),
                    )
                }
                Text(
                    text = weightUnit,
                    style = MaterialTheme.typography.body2,
                    color = colorResource(id = R.color.color_on_surface_disabled)
                )
            }
        }
    }
}

@Composable
internal fun LoadingScreen(title: Int, modifier: Modifier = Modifier, onNavigateBack: () -> Unit = {}) {
    Scaffold(topBar = { TopBar(title, onNavigateBack) }) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
internal fun ErrorScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    onRetryClick: () -> Unit = {}
) = Scaffold(topBar = { TopBar(onNavigateBack = onNavigateBack) }) { padding ->
    ErrorMessageWithButton(modifier = modifier.padding(padding), onRetryClick = onRetryClick)
}

@LightDarkThemePreviews
@Composable
private fun WooShippingLabelCreationScreenPreview() {
    WooThemeWithBackground {
        WooShippingLabelCreationScreen(
            shipmentUIList = listOf(
                ShipmentUI(
                    shippableItems = generateItems(6),
                    formattedTotalWeight = "8.5kg",
                    formattedTotalPrice = "$92.78",
                    packageSelectionState = NotSelected,
                    customsState = Unavailable,
                    hazmatState = Declared(ShippingLabelHazmatCategory.CLASS_1),
                    shippingRatesState = ShippingLabelSampleData.getShippingRatesSection(),
                    shipmentCostUI = ShippingLabelSampleData.getShippingRateSummaryUI(),
                    labelPurchaseStatus = LabelPurchaseStatus.Idle,
                )
            ),
            shouldShowSplitShipmentButton = true,
            totalItems = 6,
            totalItemsCost = "$92.78",
            shippingLines = ShippingLabelSampleData.getShippingLines(),
            paymentsSectionUI = ShippingLabelSampleData.getPaymentsSection(),
            purchaseSectionUI = ShippingLabelSampleData.getPurchaseSection(),
            modifier = Modifier.fillMaxSize(),
            onSelectPackageClick = {},
            shippingAddresses = listOf(
                WooShippingAddresses(
                    shipFrom = ShippingLabelSampleData.getShipFrom(),
                    shipTo = ShippingLabelSampleData.getShipTo(),
                    originAddresses = listOf(ShippingLabelSampleData.getShipFrom())
                )
            ),
            onSelectedShipmentChanged = {},
            onOriginAddressSelected = {},
            onRefreshShippingRates = {},
            onSelectedRateSortOrderChanged = {},
            weightInputList = listOf(WooShippingLabelCreationViewModel.WeightInput(weight = "", autoFilled = true)),
            weightUnit = "kg",
            onWeightChange = {},
            onNavigateBack = {},
            onEditOriginAddress = {},
            uiState = WooShippingLabelCreationViewModel.UIControlsState(
                markOrderComplete = false,
                paperSizeOption = WooShippingLabelPaperSize.LABEL
            ),
            onEditCustomsClick = {},
            onEditDestinationAddress = {},
            destinationStatus = AddressStatus.Verified,
            onLabelPaperSizeOptionSelected = {},
            onPrintShippingLabelClicked = {},
            onTrackShipmentClicked = {},
            onSchedulePickUpClicked = {},
            onRefundClicked = {},
            onPrintCustomsClicked = {},
            onLearnMoreClicked = {},
        )
    }
}

@Preview
@Composable
private fun PackageNotSelectedPreview() {
    WooThemeWithBackground {
        PackageCard(
            modifier = Modifier.padding(16.dp),
            packageSelectionState = NotSelected,
            onSelectPackageClick = {},
            weight = "",
            weightUnit = "kg",
            onWeightChange = {}
        )
    }
}

@Preview
@Composable
private fun PackageSelectedPreview() {
    WooThemeWithBackground {
        PackageCard(
            packageSelectionState = DataAvailable(
                selectedPackage = PackageData(
                    name = "Package 1",
                    dimensions = "1 x 1 x 1",
                    weight = "0.5",
                    isSelected = true,
                    isLetter = false,
                    id = "1",
                ),
                shipmentWeight = "1.5",
                weightUnit = "kg"
            ),
            onSelectPackageClick = {},
            weight = "1.5",
            weightUnit = "kg",
            onWeightChange = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview
@Composable
private fun ErrorScreenPreview() = WooThemeWithBackground { ErrorScreen() }
