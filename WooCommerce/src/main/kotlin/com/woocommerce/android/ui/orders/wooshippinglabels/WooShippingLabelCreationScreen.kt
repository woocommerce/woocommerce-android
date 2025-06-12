package com.woocommerce.android.ui.orders.wooshippinglabels

import android.content.res.Configuration
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
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
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelHazmatCategory
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.CustomsState
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.CustomsState.ItnMissing
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.CustomsState.NotRequired
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.CustomsState.Unavailable
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.HazmatState.Declared
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.PackageSelectionState
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.PackageSelectionState.DataAvailable
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.PackageSelectionState.NotSelected
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.ShippingRatesState
import com.woocommerce.android.ui.orders.wooshippinglabels.address.AddressStatus
import com.woocommerce.android.ui.orders.wooshippinglabels.components.PrintShippingLabelSection
import com.woocommerce.android.ui.orders.wooshippinglabels.components.ShipmentTabData
import com.woocommerce.android.ui.orders.wooshippinglabels.components.ShipmentsTabRow
import com.woocommerce.android.ui.orders.wooshippinglabels.components.ShippingLabelsSnackbarData
import com.woocommerce.android.ui.orders.wooshippinglabels.components.SuccessSnackbarHost
import com.woocommerce.android.ui.orders.wooshippinglabels.components.WooShippingLabelPaperSize
import com.woocommerce.android.ui.orders.wooshippinglabels.hazmat.HazmatCard
import com.woocommerce.android.ui.orders.wooshippinglabels.models.DestinationShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.PurchaseState
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.components.ErrorMessageWithButton
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.ShippingRateUI
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
            WooShippingLabelCreationScreen(
                onSelectPackageClick = viewModel::onSelectPackageClicked,
                onPurchaseShippingLabel = viewModel::onPurchaseShippingLabel,
                shipmentUIList = viewState.shipmentUIList,
                shouldShowSplitShipmentButton = viewState.shouldShowSplitShipmentButton,
                totalItems = viewState.totalItems,
                totalItemsCost = viewState.totalItemsCost,
                shippingLines = viewState.shippingLines,
                shippingAddresses = viewState.shippingAddresses,
                onSelectedShipmentChanged = viewModel::onSelectedShipmentChanged,
                onOriginAddressSelected = viewModel::onOriginAddressSelected,
                onEditOriginAddress = viewModel::onEditOriginAddress,
                onSelectedRateSortOrderChanged = viewModel::onSelectedRateSortOrderChanged,
                onRefreshShippingRates = viewModel::onRefreshShippingRates,
                onSelectedSippingRateChanged = viewModel::onSelectedSippingRateChanged,
                customWeightList = viewModel.customWeight,
                onCustomWeightChange = viewModel::onCustomWeightChange,
                uiState = viewState.uiState,
                onMarkOrderCompleteChange = viewModel::onMarkOrderCompleteChange,
                onNavigateBack = viewModel::onNavigateBack,
                onShipmentDetailsExpandedChange = viewModel::onShipmentDetailsExpandedChange,
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
                onLearnMoreClicked = viewModel::onLearnMoreClicked
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
    shippingAddresses: WooShippingAddresses,
    onSelectedShipmentChanged: (index: Int) -> Unit,
    onOriginAddressSelected: (OriginShippingAddress) -> Unit,
    onEditOriginAddress: (OriginShippingAddress) -> Unit,
    onSelectPackageClick: () -> Unit,
    onPurchaseShippingLabel: () -> Unit,
    onSelectedRateSortOrderChanged: (ShippingSortOption) -> Unit,
    onRefreshShippingRates: () -> Unit,
    onCustomWeightChange: (String) -> Unit,
    onSelectedSippingRateChanged: (rate: ShippingRateUI) -> Unit,
    customWeightList: List<String>,
    uiState: WooShippingLabelCreationViewModel.UIControlsState,
    onMarkOrderCompleteChange: (Boolean) -> Unit,
    onShipmentDetailsExpandedChange: (Boolean) -> Unit,
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
    onLearnMoreClicked: () -> Unit,
) {
    val shipmentDetailsValue = if (uiState.isShipmentDetailsExpanded) {
        BottomSheetValue.Expanded
    } else {
        BottomSheetValue.Collapsed
    }

    val shipmentDetailsBottomSheetState = BottomSheetState(
        initialValue = shipmentDetailsValue,
        animationSpec = BottomSheetScaffoldDefaults.AnimationSpec,
        density = LocalDensity.current,
        confirmValueChange = {
            onShipmentDetailsExpandedChange(it == BottomSheetValue.Expanded)
            true
        }
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
            shippingAddresses = shippingAddresses,
            onSelectedShipmentChanged = onSelectedShipmentChanged,
            onOriginAddressSelected = onOriginAddressSelected,
            onEditOriginAddress = onEditOriginAddress,
            onSelectedRateSortOrderChanged = onSelectedRateSortOrderChanged,
            onRefreshShippingRates = onRefreshShippingRates,
            customWeightList = customWeightList,
            onCustomWeightChange = onCustomWeightChange,
            onSelectedShippingRateChanged = onSelectedSippingRateChanged,
            uiState = uiState,
            onNavigateBack = onNavigateBack,
            onMarkOrderCompleteChange = onMarkOrderCompleteChange,
            onShipmentDetailsExpandedChange = onShipmentDetailsExpandedChange,
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
            onLearnMoreClicked = onLearnMoreClicked,
        )
        val isDarkTheme = isSystemInDarkTheme()
        val isCollapsed = scaffoldState.bottomSheetState.isCollapsed
        val elevation = when {
            isDarkTheme && isCollapsed -> 7.dp
            !isDarkTheme && isCollapsed -> 0.dp
            isDarkTheme && !isCollapsed -> 16.dp
            else -> 8.dp
        }
        val selectedShipment = shipmentUIList[uiState.selectedIndex]
        val selectedShippingRatesState = selectedShipment.shippingRatesState
        if (selectedShippingRatesState is ShippingRatesState.DataState && !selectedShipment.purchased) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Surface(elevation = elevation) {
                    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        PurchasesSectionLandscape(
                            total = selectedShippingRatesState.selectedRate?.selectedOption?.formatedPrice,
                            markOrderComplete = uiState.markOrderComplete,
                            onMarkOrderCompleteChange = onMarkOrderCompleteChange,
                            onPurchaseShippingLabel = onPurchaseShippingLabel
                        )
                    } else {
                        PurchaseButton(
                            total = selectedShippingRatesState.selectedRate?.selectedOption?.formatedPrice,
                            onPurchaseShippingLabel = onPurchaseShippingLabel
                        )
                    }
                }
            }
        }
        val selectedPurchaseState = selectedShipment.purchaseState
        if (selectedPurchaseState is PurchaseState.InProgress) {
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
    onSelectPackageClick: () -> Unit,
    shippingAddresses: WooShippingAddresses,
    onSelectedShipmentChanged: (index: Int) -> Unit,
    onEditOriginAddress: (OriginShippingAddress) -> Unit,
    onOriginAddressSelected: (OriginShippingAddress) -> Unit,
    onSelectedRateSortOrderChanged: (ShippingSortOption) -> Unit,
    onRefreshShippingRates: () -> Unit,
    customWeightList: List<String>,
    onCustomWeightChange: (String) -> Unit,
    onSelectedShippingRateChanged: (rate: ShippingRateUI) -> Unit,
    uiState: WooShippingLabelCreationViewModel.UIControlsState,
    scaffoldState: BottomSheetScaffoldState,
    onMarkOrderCompleteChange: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    onShipmentDetailsExpandedChange: (Boolean) -> Unit,
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
    onLearnMoreClicked: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val selectedShipment = shipmentUIList[uiState.selectedIndex]
    val shippingRatesState = selectedShipment.shippingRatesState
    val isPurchaseButtonDisplayed = shippingRatesState is ShippingRatesState.DataState && !selectedShipment.purchased
    val requiresLargePeekHeight = isPurchaseButtonDisplayed || uiState.noticeBannerUiState != null

    val bottomSheetPeekHeight = when {
        requiresLargePeekHeight -> 128.dp
        else -> 72.dp
    } * LocalConfiguration.current.fontScale

    val paddingBottom = when {
        isPurchaseButtonDisplayed -> 72.dp
        else -> 0.dp
    }
    val snackbarPaddingBottom = if (isPurchaseButtonDisplayed && scaffoldState.bottomSheetState.isExpanded) {
        paddingBottom
    } else {
        0.dp
    }
    val shippingRateSummary = (shippingRatesState as? ShippingRatesState.DataState)?.selectedRate?.summary

    val screenTitle = if (shipmentUIList[uiState.selectedIndex].purchased) {
        R.string.shipping_label_print_screen_title
    } else {
        R.string.shipping_label_create_title
    }

    BottomSheetScaffold(
        snackbarHost = {
            SuccessSnackbarHost(
                snackbarHostState,
                modifier = Modifier.padding(bottom = snackbarPaddingBottom)
            )
        },
        sheetContent = {
            ShipmentDetails(
                totalItems = totalItems,
                totalItemsCost = totalItemsCost,
                shippingLines = shippingLines,
                onMarkOrderCompleteChange = onMarkOrderCompleteChange,
                shippingAddresses = shippingAddresses,
                shippingRateSummary = shippingRateSummary,
                scaffoldState = scaffoldState,
                isShipmentDetailsExpanded = uiState.isShipmentDetailsExpanded,
                markOrderComplete = uiState.markOrderComplete,
                onShipmentDetailsExpandedChange = onShipmentDetailsExpandedChange,
                onEditDestinationAddress = onEditDestinationAddress,
                onEditOriginAddress = onEditOriginAddress,
                onOriginAddressSelected = onOriginAddressSelected,
                destinationStatus = destinationStatus,
                noticeBannerUiState = uiState.noticeBannerUiState,
                isReadOnly = selectedShipment.purchased
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
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val scope = rememberCoroutineScope()
                val pagerState = rememberPagerState { shipmentUIList.size }

                LaunchedEffect(pagerState.targetPage) {
                    onSelectedShipmentChanged(pagerState.targetPage)
                }

                if (shipmentUIList.size == 1 && shouldShowSplitShipmentButton) {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
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
                    Row(modifier = Modifier.fillMaxWidth()) {
                        ShipmentsTabRow(
                            shipmentTabs = shipmentUIList.mapIndexed { index, shipment ->
                                ShipmentTabData(shipmentIndex = index + 1, isPurchased = shipment.purchased)
                            },
                            selectedTabIndex = if (pagerState.currentPage < pagerState.pageCount) {
                                pagerState.currentPage
                            } else {
                                0
                            },
                            onTabSelected = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                            modifier = modifier.weight(1f)
                        )
                        if (shouldShowSplitShipmentButton) {
                            IconButton(
                                onClick = onSplitShipment,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    tint = colorResource(id = R.color.color_icon_menu),
                                    contentDescription = stringResource(id = R.string.woo_shipping_split_shipment)
                                )
                            }
                        }
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Top,
                ) { page ->
                    CreateShippingCards(
                        shipmentUI = shipmentUIList[page],
                        onHazmatNoticeClick = onHazmatNoticeClick,
                        onEditCustomsClick = onEditCustomsClick,
                        onSelectPackageClick = onSelectPackageClick,
                        customWeight = customWeightList[uiState.selectedIndex],
                        uiState = uiState,
                        onCustomWeightChange = onCustomWeightChange,
                        onSelectedRateSortOrderChanged = onSelectedRateSortOrderChanged,
                        onRefreshShippingRates = onRefreshShippingRates,
                        onSelectedShippingRateChanged = onSelectedShippingRateChanged,
                        onLabelPaperSizeOptionSelected = onLabelPaperSizeOptionSelected,
                        onPrintShippingLabelClicked = onPrintShippingLabelClicked,
                        onTrackShipmentClicked = onTrackShipmentClicked,
                        onSchedulePickUpClicked = onSchedulePickUpClicked,
                        onRefundClicked = onRefundClicked,
                        onLearnMoreClicked = onLearnMoreClicked,
                    )
                }
            }

            val actionSnackbarMessage = snackbarData?.let { stringResource(it.message) }
            val actionSnackbarActionLabel = snackbarData?.let { stringResource(it.actionLabel) }

            LaunchedEffect(snackbarData) {
                snackbarData?.let {
                    val result = snackbarHostState.showSnackbar(
                        message = actionSnackbarMessage ?: "",
                        actionLabel = actionSnackbarActionLabel,
                        duration = snackbarData.duration,
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
    customWeight: String,
    uiState: WooShippingLabelCreationViewModel.UIControlsState,
    onCustomWeightChange: (String) -> Unit,
    onSelectedRateSortOrderChanged: (ShippingSortOption) -> Unit,
    onRefreshShippingRates: () -> Unit,
    onSelectedShippingRateChanged: (rate: ShippingRateUI) -> Unit,
    onLabelPaperSizeOptionSelected: (WooShippingLabelPaperSize) -> Unit,
    onPrintShippingLabelClicked: () -> Unit,
    onTrackShipmentClicked: () -> Unit,
    onSchedulePickUpClicked: () -> Unit,
    onRefundClicked: () -> Unit,
    onLearnMoreClicked: () -> Unit,
) {
    Column {
        val isExpanded = remember { mutableStateOf(false) }

        if (shipmentUI.purchased) {
            PrintShippingLabelSection(
                status = shipmentUI.status,
                selectedLabelPaperSizeOption = uiState.paperSizeOption,
                onLabelPaperSizeOptionSelected = onLabelPaperSizeOptionSelected,
                onPrintShippingLabelClicked = onPrintShippingLabelClicked,
                onTrackShipmentClicked = onTrackShipmentClicked,
                onSchedulePickUpClicked = onSchedulePickUpClicked,
                onRefundClicked = onRefundClicked,
                onLearnMoreClicked = onLearnMoreClicked,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        ShippingProductsCard(
            shippableItems = shipmentUI,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = dimensionResource(R.dimen.major_100),
                    end = dimensionResource(R.dimen.major_100),
                    bottom = dimensionResource(R.dimen.major_100)
                ),
            isExpanded = isExpanded.value,
            onExpand = { isExpanded.value = it }
        )
        HazmatCard(
            onClick = if (shipmentUI.purchased) null else onHazmatNoticeClick,
            selectedCategory = shipmentUI.hazmatState.hazmatSelection,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp)
        )
        if (!shipmentUI.purchased) {
            CustomsCard(
                customsState = shipmentUI.customsState,
                onEditCustomsClick = onEditCustomsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            PackageCard(
                modifier = Modifier.padding(16.dp),
                packageSelectionState = shipmentUI.packageSelectionState,
                onSelectPackageClick = onSelectPackageClick,
                customWeight = customWeight,
                onCustomWeightChange = onCustomWeightChange
            )
            ShippingRatesSection(
                shippingRatesState = shipmentUI.shippingRatesState,
                onSelectedRateSortOrderChanged = onSelectedRateSortOrderChanged,
                onRefreshShippingRates = onRefreshShippingRates,
                onSelectedSippingRateChanged = onSelectedShippingRateChanged
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
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                    purchased = false,
                    packageSelectionState = NotSelected,
                    customsState = Unavailable,
                    hazmatState = Declared(ShippingLabelHazmatCategory.CLASS_1),
                    shippingRatesState = ShippingRatesState.DataState(
                        selectedRatesSortOrder = ShippingSortOption.CHEAPEST,
                        shippingRates = emptyMap(),
                        selectedRate = null
                    ),
                )
            ),
            shouldShowSplitShipmentButton = true,
            totalItems = 6,
            totalItemsCost = "$92.78",
            shippingLines = ShippingLabelSampleData.getShippingLines(),
            modifier = Modifier.fillMaxSize(),
            onSelectPackageClick = {},
            onPurchaseShippingLabel = {},
            shippingAddresses = WooShippingAddresses(
                shipFrom = ShippingLabelSampleData.getShipFrom(),
                shipTo = ShippingLabelSampleData.getShipTo(),
                originAddresses = listOf(ShippingLabelSampleData.getShipFrom())
            ),
            onSelectedShipmentChanged = {},
            onOriginAddressSelected = {},
            onRefreshShippingRates = {},
            onSelectedRateSortOrderChanged = {},
            customWeightList = listOf(""),
            onCustomWeightChange = {},
            onSelectedSippingRateChanged = {},
            onMarkOrderCompleteChange = {},
            onNavigateBack = {},
            onEditOriginAddress = {},
            uiState = WooShippingLabelCreationViewModel.UIControlsState(
                markOrderComplete = false,
                isShipmentDetailsExpanded = false,
                paperSizeOption = WooShippingLabelPaperSize.LABEL
            ),
            onShipmentDetailsExpandedChange = { true },
            onEditCustomsClick = {},
            onEditDestinationAddress = {},
            destinationStatus = AddressStatus.VERIFIED,
            onLabelPaperSizeOptionSelected = {},
            onPrintShippingLabelClicked = {},
            onTrackShipmentClicked = {},
            onSchedulePickUpClicked = {},
            onRefundClicked = {},
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

@Preview
@Composable
private fun ErrorScreenPreview() = WooThemeWithBackground { ErrorScreen() }
