package com.woocommerce.android.ui.orders.wooshippinglabels.split

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Surface
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.orders.wooshippinglabels.ExpandableSelectableShippingProduct
import com.woocommerce.android.ui.orders.wooshippinglabels.ProductsSummary
import com.woocommerce.android.ui.orders.wooshippinglabels.SelectableShippingProduct
import com.woocommerce.android.ui.orders.wooshippinglabels.split.WooShippingSplitShipmentViewModel.SplitShipmentViewState
import kotlinx.coroutines.launch

@Composable
fun WooShippingSplitShipmentScreen(
    viewModel: WooShippingSplitShipmentViewModel,
    modifier: Modifier = Modifier
) {
    viewModel.viewState.observeAsState().value?.let {
        WooShippingSplitShipmentScreen(
            viewState = it,
            onBack = viewModel::onNavigateBack,
            onDismissInstructions = viewModel::onDismissInstructions,
            onUpdateSelection = viewModel::onUpdateSelection,
            onUpdateShipment = viewModel::onUpdateShipment,
            onUpdateSelectedShipment = viewModel::onUpdateSelectedShipment,
            modifier = modifier
        )
    }
}

@Composable
fun WooShippingSplitShipmentScreen(
    viewState: SplitShipmentViewState,
    onBack: () -> Unit,
    onDismissInstructions: () -> Unit,
    onUpdateSelection: (shipmentKey: Int, index: Int, selectedIndexes: Set<Int>?) -> Unit,
    onUpdateShipment: (splitMovement: SplitMovement) -> Unit,
    onUpdateSelectedShipment: (shipmentKey: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.woo_shipping_split_shipment)) },
                navigationIcon = {
                    IconButton(onBack) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(id = R.string.close)
                        )
                    }
                },
                backgroundColor = colorResource(id = R.color.color_toolbar),
                actions = {
                    WCTextButton(
                        onClick = onBack,
                        text = stringResource(id = R.string.done)
                    )
                }
            )
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
                    val pagerState = rememberPagerState { shipments.size }
                    val scope = rememberCoroutineScope()

                    Column {
                        LaunchedEffect(pagerState.currentPage) {
                            onUpdateSelectedShipment(shipments[pagerState.currentPage])
                        }

                        Row(modifier = modifier.fillMaxWidth()) {
                            ScrollableTabRow(
                                selectedTabIndex = pagerState.currentPage,
                                backgroundColor = MaterialTheme.colors.surface,
                                contentColor = MaterialTheme.colors.primary,
                                divider = {},
                                edgePadding = 0.dp,
                                modifier = modifier
                                    .weight(1f)
                                    .padding(top = 8.dp)
                            ) {
                                shipments.forEachIndexed { index, _ ->
                                    val textColor = if (index == pagerState.currentPage) {
                                        MaterialTheme.colors.primary
                                    } else {
                                        colorResource(id = R.color.color_on_surface_medium)
                                    }
                                    Tab(
                                        text = {
                                            Text(
                                                text = stringResource(
                                                    R.string.woo_shipping_split_shipment_shipment_name,
                                                    index + 1 // Use 1-based indexing for the shipments
                                                ),
                                                color = textColor,
                                                style = MaterialTheme.typography.subtitle1,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        },
                                        selected = pagerState.currentPage == index,
                                        onClick = {
                                            scope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        }
                                    )
                                }
                            }
                            IconButton(onClick = {}, modifier = modifier.align(Alignment.CenterVertically)) {
                                Icon(
                                    imageVector = Icons.Filled.MoreHoriz,
                                    contentDescription = null,
                                    tint = MaterialTheme.colors.primary
                                )
                            }
                        }

                        HorizontalPager(
                            state = pagerState,
                            modifier = modifier.fillMaxSize(),
                            verticalAlignment = Alignment.Top,
                            pageSpacing = 16.dp
                        ) { page ->
                            viewState.selectableItems.getValue(shipments[page]).let {
                                SelectableProductsSection(
                                    shipmentKey = shipments[page],
                                    shipment = it,
                                    onUpdateSelection = onUpdateSelection,
                                    modifier = modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                                    extraBottomPadding = productsExtraPadding
                                )
                            }
                        }
                    }

                    val onUpdateShipmentAndChangeSelection: (splitMovement: SplitMovement) -> Unit = { splitMovement ->
                        if (splitMovement.isRemoveMovement) {
                            scope.launch {
                                val nextPage = shipments.indexOfFirst { it == splitMovement.updatedShipment }
                                    .takeIf { it != -1 && it < shipments.lastIndex }
                                    ?: shipments.first { it != splitMovement.currentShipment }
                                pagerState.animateScrollToPage(nextPage)
                                onUpdateShipment(splitMovement)
                            }
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
                        shipmentKey = viewState.selectableItems.keys.first(),
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
            when (splitMessage) {
                is SplitShipmentMessage.Instructions -> {
                    InstructionsMessage(
                        message = annotatedStringRes(R.string.woo_shipping_split_shipment_instructions),
                        onClose = onDismissInstructions
                    )
                }

                is SplitShipmentMessage.Success -> TODO()
                null -> {}
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
                    imageVector = Icons.Filled.Close,
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

                val containsOnlyANewKey = movements.size == 1 && (movements.first().updatedShipment in shipments).not()
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
                                imageVector = Icons.Filled.ArrowDropDown,
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
                                        text = shipmentIndex[movement.updatedShipment]?.let {
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
    shipmentKey: Int,
    shipment: SelectableShippableItemsUI,
    onUpdateSelection: (shipmentKey: Int, index: Int, selectedIndexes: Set<Int>?) -> Unit,
    extraBottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        ProductsSummary(
            totalItems = shipment.shippableItems.size,
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
                            isSelected = shippableItem.isSelected,
                            onSelectionChange = {
                                onUpdateSelection(
                                    shipmentKey,
                                    index,
                                    null
                                )
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
                            isSelected = shippableItem.isSelected,
                            onSelectionChange = {
                                onUpdateSelection(
                                    shipmentKey,
                                    index,
                                    null
                                )
                            },
                            isExpanded = expanded,
                            onExpand = { expanded = !expanded },
                            singleWeight = shippableItem.innerShippableItem.formattedWeight,
                            singlePrice = shippableItem.innerShippableItem.formattedPrice,
                            selectedIndexes = shippableItem.selectedIndexes,
                            onInnerSelectionChange = { isSelected, innerIndex ->
                                val indexes = shippableItem.selectedIndexes.toMutableSet()
                                if (isSelected) indexes.remove(innerIndex) else indexes.add(innerIndex)

                                onUpdateSelection(
                                    shipmentKey,
                                    index,
                                    indexes
                                )
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
