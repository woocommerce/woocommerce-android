package com.woocommerce.android.ui.products.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooActionChip
import com.woocommerce.android.ui.compose.designsystem.component.WooDivider
import com.woocommerce.android.ui.compose.designsystem.component.WooOutlinedIconButton
import com.woocommerce.android.ui.compose.designsystem.component.WooPageHeader
import com.woocommerce.android.ui.compose.designsystem.component.WooSearchField
import com.woocommerce.android.ui.compose.designsystem.component.WooTab
import com.woocommerce.android.ui.compose.designsystem.component.WooTabRow
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground
import com.woocommerce.android.ui.compose.designsystem.icons.AngleDown
import com.woocommerce.android.ui.compose.designsystem.icons.ArrowDownArrowUp
import com.woocommerce.android.ui.compose.designsystem.icons.BarcodeScan
import com.woocommerce.android.ui.compose.designsystem.icons.BarsFilter
import com.woocommerce.android.ui.compose.designsystem.icons.MagnifyingGlass
import com.woocommerce.android.ui.compose.designsystem.icons.Plus
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
@Suppress("LongParameterList")
internal fun ProductListScreen(
    state: ProductListScreenState,
    scrollToTopRequests: Flow<Unit>,
    onSearchClicked: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSearchSubmitted: () -> Unit,
    onSearchClosed: () -> Unit,
    onSearchTypeChanged: (Boolean) -> Unit,
    onBarcodeClicked: () -> Unit,
    onAddProductClicked: () -> Unit,
    onEmptyAddProductClicked: () -> Unit,
    onSortClicked: () -> Unit,
    onFiltersClicked: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onProductTapped: (Long) -> Unit,
    onProductLongPressed: (Long) -> Unit,
    onProductSelectionToggled: (Long) -> Unit,
    onListAtTopChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val showAddProductFab = state.isAddProductAvailable && !state.isSelecting

    LaunchedEffect(scrollToTopRequests, listState) {
        scrollToTopRequests.collect { listState.animateScrollToItem(0) }
    }
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }.collect(onListAtTopChanged)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(ProductListTestTags.SCREEN),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (state.isSearchActive) {
                ProductSearch(
                    query = state.searchQuery,
                    isSkuSearch = state.isSkuSearch,
                    filterCount = state.filterCount,
                    onQueryChanged = onSearchQueryChanged,
                    onSearchSubmitted = onSearchSubmitted,
                    onSearchClosed = onSearchClosed,
                    onSearchTypeChanged = onSearchTypeChanged,
                )
            } else {
                ProductListHeader(
                    showActions = !state.isSelecting,
                    showBarcode = state.isBarcodeScanningAvailable,
                    onSearchClicked = onSearchClicked,
                    onBarcodeClicked = onBarcodeClicked,
                )
            }

            if (state.showBrowsingControls && !state.isSearchActive && !state.isSelecting) {
                ProductBrowsingControls(
                    sortingTitle = state.sortingTitle,
                    filterCount = state.filterCount,
                    onSortClicked = onSortClicked,
                    onFiltersClicked = onFiltersClicked,
                )
            }

            ProductListContent(
                state = state,
                listState = listState,
                showAddProductFab = showAddProductFab,
                onRefresh = onRefresh,
                onLoadMore = onLoadMore,
                onEmptyAddProductClicked = onEmptyAddProductClicked,
                onProductTapped = onProductTapped,
                onProductLongPressed = onProductLongPressed,
                onProductSelectionToggled = onProductSelectionToggled,
            )
        }

        AnimatedVisibility(
            visible = showAddProductFab,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(WooTheme.padding.padding5)
                .testTag(ProductListTestTags.ADD_FAB),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
        ) {
            FloatingActionButton(
                onClick = onAddProductClicked,
                containerColor = WooTheme.colors.primary,
                contentColor = WooTheme.colors.onPrimary,
                modifier = Modifier.testTag(ProductListTestTags.ADD_ACTION),
            ) {
                Icon(
                    imageVector = WooIcons.Regular.Plus,
                    contentDescription = stringResource(R.string.add_products_button),
                    modifier = Modifier.size(WooTheme.iconSize.size24),
                )
            }
        }
    }
}

@Composable
private fun ProductListHeader(
    showActions: Boolean,
    showBarcode: Boolean,
    onSearchClicked: () -> Unit,
    onBarcodeClicked: () -> Unit,
) {
    WooPageHeader(
        title = stringResource(R.string.products),
        actions = {
            if (showActions) {
                WooOutlinedIconButton(
                    imageVector = WooIcons.Regular.MagnifyingGlass,
                    contentDescription = stringResource(R.string.product_search_hint),
                    onClick = onSearchClicked,
                    modifier = Modifier.testTag(ProductListTestTags.SEARCH_ACTION),
                )
                if (showBarcode) {
                    WooOutlinedIconButton(
                        imageVector = WooIcons.Regular.BarcodeScan,
                        contentDescription = stringResource(R.string.scan_barcode),
                        onClick = onBarcodeClicked,
                        modifier = Modifier.testTag(ProductListTestTags.BARCODE_ACTION),
                    )
                }
            }
        },
    )
}

@Composable
private fun ProductSearch(
    query: String,
    isSkuSearch: Boolean,
    filterCount: Int,
    onQueryChanged: (String) -> Unit,
    onSearchSubmitted: () -> Unit,
    onSearchClosed: () -> Unit,
    onSearchTypeChanged: (Boolean) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
    Column {
        WooSearchField(
            value = query,
            onValueChange = onQueryChanged,
            placeholder = stringResource(
                if (filterCount > 0) {
                    R.string.product_search_hint_active_filters
                } else {
                    R.string.product_search_hint
                }
            ),
            onClearClick = { onQueryChanged("") },
            clearContentDescription = stringResource(R.string.clear),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearchSubmitted()
                    keyboardController?.hide()
                }
            ),
            focusRequester = focusRequester,
            trailingActionText = stringResource(R.string.cancel),
            onTrailingActionClick = onSearchClosed,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ProductListTestTags.SEARCH_FIELD),
        )
        WooTabRow(selectedTabIndex = if (isSkuSearch) 1 else 0) {
            WooTab(
                selected = !isSkuSearch,
                onClick = { onSearchTypeChanged(false) },
                text = stringResource(R.string.product_search_all),
                modifier = Modifier.testTag(ProductListTestTags.SEARCH_ALL),
            )
            WooTab(
                selected = isSkuSearch,
                onClick = { onSearchTypeChanged(true) },
                text = stringResource(R.string.product_search_sku),
                modifier = Modifier.testTag(ProductListTestTags.SEARCH_SKU),
            )
        }
    }
}

@Composable
private fun ProductBrowsingControls(
    sortingTitle: String,
    filterCount: Int,
    onSortClicked: () -> Unit,
    onFiltersClicked: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(CONTROL_RAIL_HEIGHT)
            .background(WooTheme.colors.surface.default)
            .testTag(ProductListTestTags.CONTROL_RAIL)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = WooTheme.padding.padding7, vertical = WooTheme.padding.padding3),
        horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WooActionChip(
            label = sortingTitle,
            onClick = onSortClicked,
            modifier = Modifier.testTag(ProductListTestTags.SORT),
            leadingIcon = {
                ProductBrowsingControlIcon(WooIcons.Regular.ArrowDownArrowUp)
            },
            trailingIcon = {
                ProductBrowsingControlIcon(WooIcons.Regular.AngleDown)
            },
        )
        WooActionChip(
            label = if (filterCount > 0) {
                stringResource(R.string.product_list_filters_count, filterCount)
            } else {
                stringResource(R.string.product_list_filters)
            },
            onClick = onFiltersClicked,
            modifier = Modifier.testTag(ProductListTestTags.FILTERS),
            leadingIcon = {
                ProductBrowsingControlIcon(WooIcons.Regular.BarsFilter)
            },
            trailingIcon = {
                ProductBrowsingControlIcon(WooIcons.Regular.AngleDown)
            },
        )
    }
    WooDivider()
}

@Composable
private fun ProductBrowsingControlIcon(imageVector: ImageVector) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        modifier = Modifier.size(WooTheme.iconSize.size14),
    )
}

private val CONTROL_RAIL_HEIGHT = 64.dp
private val previewItems = listOf(
    ProductListItemUiModel(
        remoteId = 1,
        name = "Beanie",
        imageUrl = "",
        status = null,
        isStatusPending = false,
        stockAndPrice = "In stock • $18.00",
        sku = "SKU: woo-beanie",
        isSelected = false,
        isUploadingMedia = false,
        isHighlighted = false,
    ),
    ProductListItemUiModel(
        remoteId = 2,
        name = "Long product title wrapping onto a second line",
        imageUrl = "",
        status = "Draft",
        isStatusPending = false,
        stockAndPrice = "Out of stock • $32.00",
        sku = null,
        isSelected = true,
        isUploadingMedia = true,
        isHighlighted = false,
    ),
)

@Suppress("MagicNumber")
private val figmaParityPreviewItems = listOf(
    previewProduct(
        remoteId = 11,
        name = "Keep Cups",
        status = "Draft",
        stockAndPrice = "In stock • $18.00",
        sku = "SKU: keep-cups",
    ),
    previewProduct(remoteId = 12, name = "Hario V60 Dripper", stockAndPrice = "386 in stock • $32.00"),
    previewProduct(remoteId = 13, name = "Enamel Mug", stockAndPrice = "102 in stock • $24.00"),
    previewProduct(remoteId = 14, name = "Moka Pot", stockAndPrice = "In stock • $42.00"),
    previewProduct(
        remoteId = 15,
        name = "Paper Filter",
        status = "Draft",
        stockAndPrice = "Out of stock • $8.00",
    ),
    previewProduct(remoteId = 16, name = "Coffee Storage", stockAndPrice = "In stock • 76 variations"),
    previewProduct(remoteId = 17, name = "Enamel Mug Set", stockAndPrice = "In stock • 28 variations"),
)

private fun previewProduct(
    remoteId: Long,
    name: String,
    stockAndPrice: String,
    status: String? = null,
    sku: String? = null,
) = ProductListItemUiModel(
    remoteId = remoteId,
    name = name,
    imageUrl = "",
    status = status,
    isStatusPending = false,
    stockAndPrice = stockAndPrice,
    sku = sku,
    isSelected = false,
    isUploadingMedia = false,
    isHighlighted = false,
)

@Composable
private fun ProductListPreview(state: ProductListScreenState) {
    WooDesignSystemThemeWithBackground {
        ProductListScreen(
            state = state,
            scrollToTopRequests = emptyFlow(),
            onSearchClicked = {},
            onSearchQueryChanged = {},
            onSearchSubmitted = {},
            onSearchClosed = {},
            onSearchTypeChanged = {},
            onBarcodeClicked = {},
            onAddProductClicked = {},
            onEmptyAddProductClicked = {},
            onSortClicked = {},
            onFiltersClicked = {},
            onRefresh = {},
            onLoadMore = {},
            onProductTapped = {},
            onProductLongPressed = {},
            onProductSelectionToggled = {},
            onListAtTopChanged = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ProductListBrowsingPreview() {
    ProductListPreview(
        ProductListScreenState(
            products = previewItems,
            sortingTitle = "Newest",
            showBrowsingControls = true,
            isBarcodeScanningAvailable = true,
        )
    )
}

@Suppress("MagicNumber")
@Preview(name = "Figma parity", widthDp = 430, heightDp = 850, locale = "en", showBackground = true)
@Composable
private fun ProductListFigmaParityPreview() {
    ProductListPreview(
        ProductListScreenState(
            products = figmaParityPreviewItems,
            sortingTitle = "Sort by",
            showBrowsingControls = true,
            isBarcodeScanningAvailable = true,
        )
    )
}

@Preview
@Composable
private fun ProductListSearchPreview() {
    ProductListPreview(
        ProductListScreenState(
            products = previewItems.take(1),
            isSearchActive = true,
            searchQuery = "Beanie",
        )
    )
}

@Preview
@Composable
private fun ProductListSelectionAndUploadPreview() {
    ProductListPreview(
        ProductListScreenState(
            products = previewItems.mapIndexed { index, item ->
                item.copy(isSelected = index == 0, isUploadingMedia = index == 1)
            },
            isSelecting = true,
        )
    )
}

@Preview
@Composable
private fun ProductListLoadingPreview() {
    ProductListPreview(
        ProductListScreenState(
            isAddProductAvailable = false,
            isSkeletonShown = true,
        )
    )
}

@Preview
@Composable
private fun ProductListRefreshPreview() {
    ProductListPreview(
        ProductListScreenState(
            products = previewItems,
            isLoading = true,
            isRefreshing = true,
        )
    )
}

@Preview
@Composable
private fun ProductListAppendPreview() {
    ProductListPreview(
        ProductListScreenState(
            products = previewItems,
            isLoading = true,
            isLoadingMore = true,
        )
    )
}

@Preview
@Composable
private fun ProductListEmptyPreview() {
    ProductListPreview(
        ProductListScreenState(
            isAddProductAvailable = false,
            isEmptyViewVisible = true,
        )
    )
}

@Preview
@Composable
private fun ProductListSearchEmptyPreview() {
    ProductListPreview(
        ProductListScreenState(
            isSearchActive = true,
            searchQuery = "Missing product",
            isEmptyViewVisible = true,
        )
    )
}

@Preview
@Composable
private fun ProductListFilteredEmptyPreview() {
    ProductListPreview(
        ProductListScreenState(
            filterCount = 2,
            sortingTitle = "Newest",
            showBrowsingControls = true,
            isAddProductAvailable = true,
            isEmptyViewVisible = true,
        )
    )
}

@Preview
@Composable
private fun ProductListTabletHighlightPreview() {
    ProductListPreview(
        ProductListScreenState(
            products = previewItems.mapIndexed { index, item -> item.copy(isHighlighted = index == 0) },
        )
    )
}

@Preview(fontScale = 2f)
@Composable
private fun ProductListLargeFontPreview() {
    ProductListPreview(ProductListScreenState(products = previewItems))
}

@Preview(locale = "ar")
@Composable
private fun ProductListRtlPreview() {
    ProductListPreview(
        ProductListScreenState(
            products = previewItems,
            sortingTitle = "الأحدث",
            showBrowsingControls = true,
        )
    )
}
