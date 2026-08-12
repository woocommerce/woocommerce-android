package com.woocommerce.android.ui.orders.wooshippinglabels.split

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.ui.compose.component.WCOverflowMenu
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.wooshippinglabels.ExpandableSelectableShippingProduct
import com.woocommerce.android.ui.orders.wooshippinglabels.ProductsSummary
import com.woocommerce.android.ui.orders.wooshippinglabels.SelectableShippingProduct
import com.woocommerce.android.ui.orders.wooshippinglabels.components.ShipmentTabData
import com.woocommerce.android.ui.orders.wooshippinglabels.components.ShipmentsTabRow
import com.woocommerce.android.ui.orders.wooshippinglabels.components.ShippingLabelsSnackbar
import com.woocommerce.android.ui.orders.wooshippinglabels.components.ShippingLabelsSnackbarVisuals
import com.woocommerce.android.ui.orders.wooshippinglabels.split.WooShippingSplitShipmentViewModel.SplitShipmentViewState
import kotlinx.coroutines.launch

@Composable
fun WooShippingSplitShipmentScreen(
    viewModel: WooShippingSplitShipmentViewModel,
    modifier: Modifier = Modifier
) {
    viewModel.viewState.observeAsState().value?.let {
        when (it) {
            is SplitShipmentViewState.Loading -> LoadingScreen()
            is SplitShipmentViewState.DataState -> WooShippingSplitShipmentScreen(
                viewState = it,
                onBack = viewModel::onNavigateBack,
                onDone = viewModel::onDoneTapped,
                onDismissInstructions = viewModel::onDismissInstructions,
                onUpdateSelection = viewModel::onUpdateSelection,
                onUpdateShipment = viewModel::onUpdateShipment,
                onRemoveShipments = viewModel::onRemoveShipments,
                onUpdateSelectedShipment = viewModel::onUpdateSelectedShipment,
                onRemoveShipmentMenuTapped = viewModel::onRemoveShipmentMenuTapped,
                onDismissRemoveSheet = viewModel::onDismissRemoveSheet,
                modifier = modifier
            )
        }
    }
}

@Suppress("CyclomaticComplexMethod")
@Composable
fun WooShippingSplitShipmentScreen(
    viewState: SplitShipmentViewState.DataState,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onDismissInstructions: () -> Unit,
    onUpdateSelection: (index: Int, selectedIndexes: Set<Int>?) -> Unit,
    onUpdateShipment: (splitMovement: SplitMovement) -> Unit,
    onRemoveShipments: (removingShipmentKeys: List<Int>, movingToShipmentKey: Int?) -> Unit,
    onUpdateSelectedShipment: (shipmentKey: Int) -> Unit,
    onRemoveShipmentMenuTapped: (shipmentKeys: List<Int>) -> Unit,
    onDismissRemoveSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = { TopBar(onBack, onDone) },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                val visuals = data.visuals as ShippingLabelsSnackbarVisuals
                ShippingLabelsSnackbar(visuals = visuals, action = { data.performAction() })
            }
        }
    ) { padding ->
        Surface(
            modifier = modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Box(modifier.fillMaxSize()) {
                val productsExtraPadding = when {
                    viewState.splitMessage != null && viewState.splitMovements.isNotEmpty() -> 240.dp
                    viewState.splitMessage != null -> 120.dp
                    viewState.splitMovements.isNotEmpty() -> 120.dp
                    else -> 16.dp
                }

                val shipments = viewState.selectableItems.keys.toList()

                if (shipments.size > 1) {
                    MultipleShipments(
                        viewState,
                        shipments,
                        productsExtraPadding,
                        onUpdateSelectedShipment,
                        onUpdateSelection,
                        onRemoveShipmentMenuTapped,
                        modifier
                    )

                    val onUpdateShipmentAndChangeSelection: (splitMovement: SplitMovement) -> Unit = { splitMovement ->
                        if (splitMovement.isRemoveMovement) {
                            val nextPage = shipments.indexOfFirst { it == splitMovement.destinationShipmentKey }
                                .takeIf { it != -1 && it < shipments.lastIndex }
                                ?: shipments.first { it != splitMovement.sourceShipmentKey }
                            onUpdateSelectedShipment(shipments[nextPage])
                            onUpdateShipment(splitMovement)
                        } else {
                            onUpdateShipment(splitMovement)
                        }
                    }

                    SplitMessagesSection(
                        shipments = shipments,
                        splitMessage = viewState.splitMessage,
                        splitMovements = viewState.splitMovements,
                        onDismissInstructions = onDismissInstructions,
                        onUpdateShipment = onUpdateShipmentAndChangeSelection,
                        modifier = modifier
                            .padding(16.dp)
                            .align(Alignment.BottomCenter)
                    )
                } else {
                    SelectableProductsSection(
                        shipment = viewState.selectableItems.values.first(),
                        onUpdateSelection = onUpdateSelection,
                        extraBottomPadding = productsExtraPadding,
                        modifier = modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    )
                    SplitMessagesSection(
                        shipments = shipments,
                        splitMessage = viewState.splitMessage,
                        splitMovements = viewState.splitMovements,
                        onDismissInstructions = onDismissInstructions,
                        onUpdateShipment = onUpdateShipment,
                        modifier = modifier
                            .padding(16.dp)
                            .align(Alignment.BottomCenter)
                    )
                }
            }
        }

        viewState.removeShipmentSheet?.let { removeShipmentSheet ->
            RemoveShipmentBottomSheet(removeShipmentSheet, onDismissRemoveSheet, onRemoveShipments)
        }
    }

    val resources = LocalResources.current
    LaunchedEffect(viewState.splitMessage) {
        val snackbarData = if (viewState.splitMessage is SplitShipmentMessage.Success) {
            viewState.splitMessage.snackbarData
        } else if (viewState.splitMessage is SplitShipmentMessage.Error) {
            viewState.splitMessage.snackbarData
        } else {
            return@LaunchedEffect
        }

        @Suppress("SpreadOperator")
        val result = snackbarHostState.showSnackbar(
            visuals = ShippingLabelsSnackbarVisuals(
                message = if (snackbarData.messageParameters.isEmpty()) {
                    resources.getString(snackbarData.message)
                } else {
                    resources.getString(
                        snackbarData.message,
                        *snackbarData.messageParameters.toTypedArray()
                    )
                },
                actionLabel = resources.getString(snackbarData.actionLabel),
                duration = snackbarData.duration,
                hasSuccessCheckmark = viewState.splitMessage is SplitShipmentMessage.Success
            )
        )
        when (result) {
            SnackbarResult.ActionPerformed -> snackbarData.action()
            SnackbarResult.Dismissed -> snackbarData.dismissAction()
        }
    }
}

@Composable
private fun TopBar(onBack: (() -> Unit)? = null, onDone: (() -> Unit)? = null) {
    TopAppBar(
        title = { Text(stringResource(R.string.woo_shipping_split_shipment)) },
        navigationIcon = {
            IconButton(onBack ?: {}) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_close_24dp),
                    contentDescription = stringResource(id = R.string.close)
                )
            }
        },
        backgroundColor = colorResource(id = R.color.color_toolbar),
        actions = {
            WCTextButton(
                enabled = onDone != null,
                onClick = onDone ?: {},
                text = stringResource(id = R.string.done)
            )
        }
    )
}

@Composable
private fun MultipleShipments(
    viewState: SplitShipmentViewState.DataState,
    shipments: List<Int>,
    productsExtraPadding: Dp,
    onUpdateSelectedShipment: (shipmentKey: Int) -> Unit,
    onUpdateSelection: (index: Int, selectedIndexes: Set<Int>?) -> Unit,
    onRemoveShipmentMenuTapped: (shipmentKeys: List<Int>) -> Unit,
    modifier: Modifier
) {
    val pagerState = rememberPagerState { shipments.size }
    val scope = rememberCoroutineScope()

    Column {
        LaunchedEffect(pagerState.targetPage) {
            onUpdateSelectedShipment(shipments[pagerState.targetPage])
        }

        LaunchedEffect(viewState.shipmentSelected) {
            val viewStateCurrentPage = shipments.indexOf(viewState.shipmentSelected)
            if (pagerState.currentPage != viewStateCurrentPage && viewStateCurrentPage != -1) {
                pagerState.animateScrollToPage(viewStateCurrentPage)
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            ShipmentsTabRow(
                shipmentTabs = shipments.mapIndexed { index, shipmentKey ->
                    ShipmentTabData(
                        shipmentIndex = index + 1,
                        isPurchased = viewState.selectableItems[index]?.purchased == true
                    )
                },
                selectedTabIndex = if (pagerState.currentPage < pagerState.pageCount) pagerState.currentPage else 0,
                onTabSelected = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                modifier = Modifier.weight(1f)
            )
            if (viewState.overflowMenuItems.isNotEmpty()) {
                WCOverflowMenu(
                    items = viewState.overflowMenuItems,
                    mapper = { shipmentIndexList ->
                        if (shipmentIndexList.size == 1) {
                            // Example: "Remove shipment 1"
                            stringResource(
                                R.string.woo_shipping_split_shipment_shipment_remove,
                                stringResource(
                                    R.string.woo_shipping_split_shipment_shipment_name,
                                    shipmentIndexList.first() + 1
                                ).toLowerCase(Locale.current)
                            )
                        } else {
                            stringResource(R.string.woo_shipping_split_shipment_merge_unfulfilled_menu)
                        }
                    },
                    onSelected = onRemoveShipmentMenuTapped,
                    modifier = modifier.align(Alignment.CenterVertically)
                )
            }
        }

        HorizontalDivider()

        HorizontalPager(
            state = pagerState,
            modifier = modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
            pageSpacing = 16.dp
        ) { page ->
            viewState.selectableItems.getValue(shipments[page]).let {
                SelectableProductsSection(
                    shipment = it,
                    onUpdateSelection = onUpdateSelection,
                    modifier = modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    extraBottomPadding = productsExtraPadding
                )
            }
        }
    }
}

@Composable
private fun SplitMessagesSection(
    shipments: List<Int>,
    splitMessage: SplitShipmentMessage?,
    splitMovements: List<SplitMovement>,
    onDismissInstructions: () -> Unit,
    onUpdateShipment: (splitMovement: SplitMovement) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        AnimatedVisibility(
            visible = splitMessage != null,
            label = "message_transition",
            enter = slideInVertically(initialOffsetY = { it })
        ) {
            if (splitMessage is SplitShipmentMessage.Instructions) {
                InstructionsMessage(
                    message = annotatedStringRes(R.string.woo_shipping_split_shipment_instructions),
                    onClose = onDismissInstructions
                )
            }
        }
        AnimatedVisibility(
            visible = splitMovements.isNotEmpty(),
            label = "movements_transition",
            enter = slideInVertically(initialOffsetY = { it })
        ) {
            SplitMovements(
                shipments = shipments,
                movements = splitMovements,
                onUpdateShipment = onUpdateShipment,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
}

@Composable
private fun InstructionsMessage(
    message: AnnotatedString,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        backgroundColor = colorResource(id = R.color.woo_message_surface),
        modifier = modifier,
        shape = RoundedCornerShape(corner = CornerSize(8.dp))
    ) {
        Row {
            Text(
                text = message,
                color = MaterialTheme.colors.onPrimary,
                modifier = Modifier
                    .weight(1f, true)
                    .padding(start = 16.dp, top = 16.dp, bottom = 16.dp)
            )
            IconButton(onClick = { onClose() }) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_close_24dp),
                    tint = MaterialTheme.colors.onPrimary.copy(alpha = .60f),
                    contentDescription = stringResource(id = R.string.close),
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

@Composable
private fun SplitMovements(
    shipments: List<Int>,
    movements: List<SplitMovement>,
    onUpdateShipment: (splitMovement: SplitMovement) -> Unit,
    modifier: Modifier = Modifier
) {
    if (movements.isNotEmpty()) {
        Card(
            backgroundColor = MaterialTheme.colors.surface,
            modifier = modifier,
            shape = RoundedCornerShape(corner = CornerSize(8.dp)),
            elevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.selection_count, movements.first().totalItemsToMove),
                    color = MaterialTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                val containsOnlyANewKey = movements.size == 1 &&
                    (movements.first().destinationShipmentKey in shipments).not()
                if (containsOnlyANewKey) {
                    Text(
                        text = stringResource(R.string.woo_shipping_split_move_to_new).uppercase(),
                        style = MaterialTheme.typography.subtitle2,
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier
                            .clickable { onUpdateShipment(movements.first()) }
                            .padding(8.dp)
                    )
                } else {
                    var expanded by remember { mutableStateOf(false) }
                    val shipmentIndex = shipments.mapIndexed { index, shipment -> shipment to index }.toMap()

                    Box {
                        Row(
                            modifier = Modifier.clickable { expanded = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.woo_shipping_split_move_to).uppercase(),
                                style = MaterialTheme.typography.subtitle2,
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier.padding(8.dp)
                            )
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_drop_down),
                                contentDescription = null,
                                tint = MaterialTheme.colors.primary
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.sizeIn(minWidth = 150.dp)
                        ) {
                            movements.forEach { movement ->
                                DropdownMenuItem(onClick = {
                                    onUpdateShipment(movement)
                                    expanded = false
                                }) {
                                    Text(
                                        text = shipmentIndex[movement.destinationShipmentKey]?.let {
                                            stringResource(R.string.woo_shipping_split_shipment_shipment_name, it + 1)
                                        } ?: stringResource(R.string.woo_shipping_split_shipment_shipment_new),
                                        style = MaterialTheme.typography.subtitle1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectableProductsSection(
    shipment: SelectableShippableItemsUI,
    onUpdateSelection: (index: Int, selectedIndexes: Set<Int>?) -> Unit,
    extraBottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (shipment.purchased) {
            Column(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .fillMaxWidth()
                    .background(
                        color = colorResource(R.color.woo_shipping_label_success_surface),
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.woo_shipping_split_shipment_purchased_message_title),
                    color = colorResource(R.color.woo_shipping_label_success),
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.woo_shipping_split_shipment_purchased_message_desc),
                    color = colorResource(R.color.woo_shipping_label_success),
                    style = MaterialTheme.typography.body2
                )
            }
        }

        ProductsSummary(
            totalItems = shipment.totalItemQuantity,
            totalWeight = shipment.formattedTotalWeight,
            totalPrice = shipment.formattedTotalPrice,
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )

        LazyColumn {
            itemsIndexed(
                items = shipment.shippableItems,
            ) { index, shippableItem ->
                when (shippableItem) {
                    is SelectableShippableItemUI.SingleSelectableShippableItemUI -> {
                        SelectableShippingProduct(
                            title = shippableItem.shippableItem.title,
                            description = shippableItem.shippableItem.formattedSize,
                            weight = shippableItem.shippableItem.formattedWeight,
                            price = shippableItem.shippableItem.formattedPrice,
                            quantity = shippableItem.shippableItem.quantity,
                            imageUrl = shippableItem.shippableItem.imageUrl,
                            selectable = !shipment.purchased,
                            isSelected = shippableItem.isSelected,
                            onSelectionChange = if (shipment.purchased) {
                                null
                            } else {
                                { onUpdateSelection(index, null) }
                            },
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    is SelectableShippableItemUI.ExpandableSelectableShippableItemUI -> {
                        var expanded by remember { mutableStateOf(false) }
                        ExpandableSelectableShippingProduct(
                            title = shippableItem.shippableItem.title,
                            description = shippableItem.shippableItem.formattedSize,
                            weight = shippableItem.shippableItem.formattedWeight,
                            price = shippableItem.shippableItem.formattedPrice,
                            quantity = shippableItem.shippableItem.quantity,
                            imageUrl = shippableItem.shippableItem.imageUrl,
                            selectable = !shipment.purchased,
                            isSelected = shippableItem.isSelected,
                            onSelectionChange = if (shipment.purchased) {
                                null
                            } else {
                                { onUpdateSelection(index, null) }
                            },
                            isExpanded = expanded,
                            onExpand = { expanded = !expanded },
                            singleWeight = shippableItem.innerShippableItem.formattedWeight,
                            singlePrice = shippableItem.innerShippableItem.formattedPrice,
                            selectedIndexes = shippableItem.selectedIndexes,
                            onInnerSelectionChange = if (shipment.purchased) {
                                null
                            } else {
                                { isSelected, innerIndex ->
                                    val indexes = shippableItem.selectedIndexes.toMutableSet()
                                    if (isSelected) indexes.remove(innerIndex) else indexes.add(innerIndex)

                                    onUpdateSelection(index, indexes)
                                }
                            },
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
            if (extraBottomPadding > 0.dp) {
                item {
                    Spacer(modifier = Modifier.padding(bottom = extraBottomPadding))
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Scaffold(topBar = { TopBar() }) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Preview
@Composable
private fun WooShippingSplitShipmentScreenPreview() = WooThemeWithBackground {
    WooShippingSplitShipmentScreen(
        viewState = SplitShipmentViewState.DataState(
            shipmentSelected = 0,
            selectableItems = mapOf(
                0 to SelectableShippableItemsUI(
                    shippableItems = emptyList(),
                    formattedTotalWeight = "",
                    formattedTotalPrice = "",
                    purchased = false
                ),
                1 to SelectableShippableItemsUI(
                    shippableItems = emptyList(),
                    formattedTotalWeight = "",
                    formattedTotalPrice = "",
                    purchased = false
                )
            )
        ),
        onBack = {},
        onDone = {},
        onDismissInstructions = {},
        onUpdateSelection = { _, _ -> },
        onUpdateShipment = {},
        onRemoveShipments = { _, _ -> },
        onUpdateSelectedShipment = {},
        onRemoveShipmentMenuTapped = { _ -> },
        onDismissRemoveSheet = {}
    )
}
