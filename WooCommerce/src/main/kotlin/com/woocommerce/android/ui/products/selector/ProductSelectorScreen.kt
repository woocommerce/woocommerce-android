package com.woocommerce.android.ui.products.selector

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.R.color
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.InfiniteListHandler
import com.woocommerce.android.ui.compose.component.SearchLayoutWithParams
import com.woocommerce.android.ui.compose.component.SearchLayoutWithParamsState
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.products.ProductType.BUNDLE
import com.woocommerce.android.ui.products.ProductType.GROUPED
import com.woocommerce.android.ui.products.ProductType.SIMPLE
import com.woocommerce.android.ui.products.ProductType.VARIABLE
import com.woocommerce.android.ui.products.ProductType.VARIABLE_SUBSCRIPTION
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.FilterState
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.ListItem
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.LoadingState.APPENDING
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.LoadingState.IDLE
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.LoadingState.LOADING
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.SelectionMode
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.ViewState
import com.woocommerce.android.ui.products.selector.SelectionState.PARTIALLY_SELECTED
import com.woocommerce.android.ui.products.selector.SelectionState.SELECTED
import com.woocommerce.android.ui.products.selector.components.SelectorListItem
import com.woocommerce.android.util.StringUtils

@Composable
fun ProductSelectorScreen(viewModel: ProductSelectorViewModel) {
    val viewState by viewModel.viewState.observeAsState()
    BackHandler(onBack = viewModel::onNavigateBack)
    viewState?.let { state ->
        Scaffold(topBar = {
            if (state.selectionMode != SelectionMode.LIVE) {
                TopAppBar(
                    title = {
                        Text(
                            text = state.screenTitleOverride
                                ?: stringResource(id = string.coupon_conditions_products_select_products_title)
                        )
                    },
                    navigationIcon = {
                        IconButton(viewModel::onNavigateBack) {
                            Icon(
                                imageVector = if (state.searchState.isActive) {
                                    Icons.AutoMirrored.Filled.ArrowBack
                                } else {
                                    Icons.Filled.Close
                                },
                                contentDescription = stringResource(id = string.back)
                            )
                        }
                    },
                    backgroundColor = colorResource(id = color.color_toolbar),
                    elevation = 0.dp,
                )
            }
        }) { padding ->
            ProductSelectorScreen(
                modifier = Modifier.padding(padding),
                state = state,
                onDoneButtonClick = viewModel::onDoneButtonClick,
                onClearButtonClick = viewModel::onClearButtonClick,
                onFilterButtonClick = viewModel::onFilterButtonClick,
                onProductClick = viewModel::onProductClick,
                onLoadMore = viewModel::onLoadMore,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onClearFiltersButtonClick = viewModel::onClearFiltersButtonClick,
                onSearchTypeChanged = viewModel::onSearchTypeChanged,
                trackConfigurableProduct = viewModel::trackConfigurableProduct,
                onEditConfiguration = viewModel::onEditConfiguration,
            )
        }
    }
}

@Composable
fun ProductSelectorScreen(
    modifier: Modifier = Modifier,
    state: ViewState,
    onDoneButtonClick: () -> Unit,
    onClearButtonClick: () -> Unit,
    onFilterButtonClick: () -> Unit,
    onProductClick: (ListItem, ProductSourceForTracking) -> Unit,
    onLoadMore: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSearchTypeChanged: (Int) -> Unit,
    onClearFiltersButtonClick: () -> Unit,
    trackConfigurableProduct: () -> Unit,
    onEditConfiguration: (ListItem.ConfigurableListItem) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface)
    ) {
        if (state.filterState.filterOptions.isEmpty()) {
            SearchLayoutWithParams(
                state = SearchLayoutWithParamsState(
                    hint = string.product_selector_search_hint,
                    searchQuery = state.searchState.searchQuery,
                    isSearchFocused = state.searchState.isActive,
                    areSearchTypesAlwaysVisible = false,
                    supportedSearchTypes = listOf(
                        SearchLayoutWithParamsState.SearchType(
                            labelResId = string.product_search_all,
                            isSelected = state.searchState.searchType.labelResId == string.product_search_all
                        ),
                        SearchLayoutWithParamsState.SearchType(
                            labelResId = string.product_search_sku,
                            isSelected = state.searchState.searchType.labelResId == string.product_search_sku
                        ),
                    )
                ),
                paramsFillWidth = true,
                onSearchQueryChanged = onSearchQueryChanged,
                onSearchTypeSelected = onSearchTypeChanged,
            )
        }

        when {
            state.products.isNotEmpty() -> ProductList(
                state = state,
                onDoneButtonClick = onDoneButtonClick,
                onClearButtonClick = onClearButtonClick,
                onFilterButtonClick = onFilterButtonClick,
                onProductClick = onProductClick,
                onLoadMore = onLoadMore,
                trackConfigurableProduct = trackConfigurableProduct,
                onEditConfiguration = onEditConfiguration
            )

            state.products.isEmpty() && state.loadingState == LOADING -> ProductListSkeleton()
            else -> EmptyProductList(state, onClearFiltersButtonClick)
        }
    }
}

@Composable
private fun EmptyProductList(
    state: ViewState,
    onClearFiltersButtonClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = dimensionResource(id = dimen.major_200),
                vertical = dimensionResource(id = dimen.major_200)
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val message = if (state.searchState.searchQuery.isNotEmpty()) {
            stringResource(id = string.empty_message_with_search, state.searchState.searchQuery)
        } else if (state.filterState.filterOptions.isNotEmpty()) {
            stringResource(id = string.empty_message_with_filters)
        } else {
            stringResource(id = string.product_selector_empty_state)
        }
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(
                start = dimensionResource(id = dimen.major_150),
                end = dimensionResource(id = dimen.major_150)
            )
        )
        Spacer(Modifier.size(dimensionResource(id = dimen.major_325)))
        Image(
            painter = painterResource(id = R.drawable.img_empty_products),
            contentDescription = null,
        )

        if (state.filterState.filterOptions.isNotEmpty()) {
            Spacer(Modifier.size(dimensionResource(id = dimen.major_325)))
            WCColoredButton(
                onClick = onClearFiltersButtonClick,
                text = stringResource(id = string.product_selector_clear_filters_button_title),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(id = dimen.major_100)),
            )
        }
    }
}

@Composable
private fun PopularProductsList(
    state: ViewState,
    onProductClick: (ListItem, ProductSourceForTracking) -> Unit,
    onEditConfiguration: (ListItem.ConfigurableListItem) -> Unit
) {
    displayProductsSection(
        type = ProductType.POPULAR,
        state = state,
        onProductClick = onProductClick,
        onEditConfiguration = onEditConfiguration
    )
}

@Composable
private fun RecentlySoldProductsList(
    state: ViewState,
    onProductClick: (ListItem, ProductSourceForTracking) -> Unit,
    onEditConfiguration: (ListItem.ConfigurableListItem) -> Unit
) {
    displayProductsSection(
        type = ProductType.RECENT,
        state = state,
        onProductClick = onProductClick,
        onEditConfiguration = onEditConfiguration
    )
}

@Composable
private fun displayProductsSection(
    type: ProductType,
    state: ViewState,
    onProductClick: (ListItem, ProductSourceForTracking) -> Unit,
    onEditConfiguration: (ListItem.ConfigurableListItem) -> Unit
) {
    val (productsList, heading, productSectionForTracking) = when (type) {
        ProductType.POPULAR -> Triple(
            state.popularProducts,
            stringResource(id = string.product_selector_popular_products_heading),
            ProductSourceForTracking.POPULAR
        )

        ProductType.RECENT -> Triple(
            state.recentProducts,
            stringResource(id = string.product_selector_recent_products_heading),
            ProductSourceForTracking.LAST_SOLD
        )
    }
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(top = dimensionResource(id = dimen.major_150))
            .background(color = MaterialTheme.colors.surface)
    ) {
        if (!productsList.isNullOrEmpty()) {
            Text(
                text = heading,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(
                    start = dimensionResource(id = dimen.major_150),
                    end = dimensionResource(id = dimen.major_150)
                )
            )
        }

        productsList.forEachIndexed { index, product ->
            SelectorListItem(
                title = product.title,
                imageUrl = product.imageUrl,
                infoLine1 = product.getInformation(),
                infoLine2 = product.sku?.let {
                    stringResource(string.product_selector_sku_value, it)
                },
                selectionState = product.selectionState,
                isArrowVisible = product.hasVariations(),
                onClickLabel = stringResource(id = string.product_selector_select_product_label, product.title),
                imageContentDescription = stringResource(string.product_image_content_description),
                isCogwheelVisible = product is ListItem.ConfigurableListItem,
                enabled = state.selectionEnabled,
                onEditConfiguration = {
                    (product as? ListItem.ConfigurableListItem)?.let(onEditConfiguration)
                }
            ) {
                onProductClick(product, productSectionForTracking)
            }
            if (index < productsList.size - 1) {
                Divider(
                    modifier = Modifier.padding(start = dimensionResource(id = dimen.major_100)),
                    color = colorResource(id = color.divider_color),
                    thickness = dimensionResource(id = dimen.minor_10)
                )
            }
        }
    }
}

enum class ProductType {
    POPULAR,
    RECENT
}

@Composable
private fun ProductList(
    state: ViewState,
    onDoneButtonClick: () -> Unit,
    onClearButtonClick: () -> Unit,
    onFilterButtonClick: () -> Unit,
    onProductClick: (ListItem, ProductSourceForTracking) -> Unit,
    onLoadMore: () -> Unit,
    trackConfigurableProduct: () -> Unit,
    onEditConfiguration: (ListItem.ConfigurableListItem) -> Unit
) {
    val listState = rememberLazyListState()
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(color = MaterialTheme.colors.surface)
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = dimensionResource(dimen.minor_100))
                .fillMaxWidth()
        ) {
            if (state.selectionMode == SelectionMode.MULTIPLE) {
                WCTextButton(
                    onClick = onClearButtonClick,
                    text = stringResource(id = string.product_selector_clear_button_title),
                    allCaps = false,
                    enabled = state.selectedItemsCount > 0,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }
            if (state.searchState.searchQuery.isEmpty()) {
                WCTextButton(
                    onClick = onFilterButtonClick,
                    text = StringUtils.getQuantityString(
                        quantity = state.filterState.filterOptions.size,
                        default = string.product_selector_filter_button_title_default,
                        zero = string.product_selector_filter_button_title_zero
                    ),
                    allCaps = false,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            if (!state.popularProducts.isNullOrEmpty()) {
                item {
                    PopularProductsList(
                        state = state,
                        onProductClick = onProductClick,
                        onEditConfiguration = onEditConfiguration
                    )
                }
            }
            if (!state.recentProducts.isNullOrEmpty()) {
                item {
                    RecentlySoldProductsList(
                        state = state,
                        onProductClick = onProductClick,
                        onEditConfiguration = onEditConfiguration
                    )
                }
            }
            item {
                if (!state.products.isNullOrEmpty()) {
                    Text(
                        text = stringResource(id = string.product_selector_products_heading),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.padding(
                            start = dimensionResource(id = dimen.major_150),
                            end = dimensionResource(id = dimen.major_150),
                            top = dimensionResource(id = dimen.major_150)
                        )
                    )
                }
            }
            itemsIndexed(state.products) { _, product ->
                if (product is ListItem.ConfigurableListItem) {
                    trackConfigurableProduct()
                }
                SelectorListItem(
                    title = product.title,
                    imageUrl = product.imageUrl,
                    infoLine1 = product.getInformation(),
                    infoLine2 = product.sku?.let {
                        stringResource(string.product_selector_sku_value, it)
                    },
                    selectionState = product.selectionState,
                    isArrowVisible = product.hasVariations(),
                    onClickLabel = stringResource(id = string.product_selector_select_product_label, product.title),
                    imageContentDescription = stringResource(string.product_image_content_description),
                    isCogwheelVisible = product is ListItem.ConfigurableListItem,
                    enabled = state.selectionEnabled,
                    onEditConfiguration = {
                        (product as? ListItem.ConfigurableListItem)?.let(onEditConfiguration)
                    }
                ) {
                    onProductClick(product, ProductSourceForTracking.ALPHABETICAL)
                }
                Divider(
                    modifier = Modifier.padding(start = dimensionResource(id = dimen.major_100)),
                    color = colorResource(id = color.divider_color),
                    thickness = dimensionResource(id = dimen.minor_10)
                )
            }
            if (state.loadingState == APPENDING || state.loadingState == LOADING) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth()
                            .padding(vertical = dimensionResource(id = dimen.minor_100))
                    )
                }
            }
        }

        InfiniteListHandler(listState = listState, buffer = 3) {
            onLoadMore()
        }

        if (state.selectionMode != SelectionMode.LIVE) {
            SelectionConfirmButton(onDoneButtonClick, state)
        }
    }
}

@Composable
private fun SelectionConfirmButton(
    onClick: () -> Unit,
    state: ViewState
) {
    Divider(
        color = colorResource(id = color.divider_color),
        thickness = dimensionResource(id = dimen.minor_10)
    )

    WCColoredButton(
        onClick = onClick,
        text = state.ctaButtonTextOverride ?: when (state.selectionMode) {
            SelectionMode.MULTIPLE -> StringUtils.getQuantityString(
                quantity = state.selectedItemsCount,
                default = string.product_selector_select_button_title_default,
                one = string.product_selector_select_button_title_one,
                zero = string.done
            )

            SelectionMode.SINGLE -> stringResource(id = string.done)
            SelectionMode.LIVE -> ""
        },
        enabled = state.isDoneButtonEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(id = dimen.major_100))
    )
}

private fun ListItem.hasVariations() =
    this is ListItem.ProductListItem && (type == VARIABLE || type == VARIABLE_SUBSCRIPTION) && numVariations > 0

@Composable
@Suppress("MagicNumber")
private fun ProductListSkeleton() {
    val numberOfInboxSkeletonRows = 10
    LazyColumn(Modifier.background(color = MaterialTheme.colors.surface)) {
        repeat(numberOfInboxSkeletonRows) {
            item {
                Row(
                    modifier = Modifier.padding(dimensionResource(id = dimen.major_100)),
                    horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = dimen.major_85))
                ) {
                    SkeletonView(
                        dimensionResource(id = dimen.skeleton_image_dimension),
                        dimensionResource(id = dimen.skeleton_image_dimension)
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = dimen.minor_100))
                    ) {
                        SkeletonView(
                            dimensionResource(id = dimen.skeleton_text_large_width),
                            dimensionResource(id = dimen.major_200)
                        )
                        SkeletonView(
                            dimensionResource(id = dimen.skeleton_text_extra_large_width),
                            dimensionResource(id = dimen.major_150)
                        )
                    }
                }
                Divider(
                    modifier = Modifier
                        .offset(x = dimensionResource(id = dimen.major_100)),
                    color = colorResource(id = color.divider_color),
                    thickness = dimensionResource(id = dimen.minor_10)
                )
            }
        }
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
@Suppress("MagicNumber")
fun PopularProductsListPreview() {
    val products = listOf(
        ListItem.ProductListItem(
            productId = 1,
            title = "Product 1",
            type = SIMPLE,
            imageUrl = null,
            numVariations = 0,
            stockAndPrice = "Not in stock • $25.00",
            sku = "1234",
            selectionState = SELECTED
        ),

        ListItem.ProductListItem(
            productId = 2,
            title = "Product 2",
            type = VARIABLE,
            imageUrl = null,
            numVariations = 3,
            stockAndPrice = "In stock • $5.00 • 3 variations",
            sku = "33333",
            selectionState = PARTIALLY_SELECTED
        ),

        ListItem.ProductListItem(
            productId = 3,
            title = "Product 3",
            type = GROUPED,
            imageUrl = "",
            numVariations = 0,
            stockAndPrice = "Out of stock",
            sku = null
        ),

        ListItem.ProductListItem(
            productId = 4,
            title = "Product 4",
            type = GROUPED,
            imageUrl = null,
            numVariations = 0,
            stockAndPrice = null,
            sku = null
        )
    )

    ProductList(
        state = ViewState(
            products = emptyList(),
            selectedItemsCount = 3,
            loadingState = IDLE,
            filterState = FilterState(),
            searchState = ProductSelectorViewModel.SearchState(),
            popularProducts = products,
            recentProducts = emptyList(),
            selectionMode = SelectionMode.MULTIPLE
        ),
        onDoneButtonClick = {},
        onClearButtonClick = {},
        onFilterButtonClick = {},
        onProductClick = { _, _ -> },
        onLoadMore = {},
        trackConfigurableProduct = {},
        onEditConfiguration = {}
    )
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
@Suppress("MagicNumber")
fun RecentProductsListPreview() {
    val products = listOf(
        ListItem.ProductListItem(
            productId = 1,
            title = "Product 1",
            type = SIMPLE,
            imageUrl = null,
            numVariations = 0,
            stockAndPrice = "Not in stock • $25.00",
            sku = "1234",
            selectionState = SELECTED
        ),

        ListItem.ProductListItem(
            productId = 2,
            title = "Product 2",
            type = VARIABLE,
            imageUrl = null,
            numVariations = 3,
            stockAndPrice = "In stock • $5.00 • 3 variations",
            sku = "33333",
            selectionState = PARTIALLY_SELECTED
        ),

        ListItem.ProductListItem(
            productId = 3,
            title = "Product 3",
            type = GROUPED,
            imageUrl = "",
            numVariations = 0,
            stockAndPrice = "Out of stock",
            sku = null
        ),

        ListItem.ProductListItem(
            productId = 4,
            title = "Product 4",
            type = GROUPED,
            imageUrl = null,
            numVariations = 0,
            stockAndPrice = null,
            sku = null
        )
    )

    ProductList(
        state = ViewState(
            products = emptyList(),
            selectedItemsCount = 3,
            loadingState = IDLE,
            filterState = FilterState(),
            searchState = ProductSelectorViewModel.SearchState(),
            popularProducts = emptyList(),
            recentProducts = products,
            selectionMode = SelectionMode.MULTIPLE
        ),
        onDoneButtonClick = {},
        onClearButtonClick = {},
        onFilterButtonClick = {},
        onProductClick = { _, _ -> },
        onLoadMore = {},
        trackConfigurableProduct = {},
        onEditConfiguration = {}
    )
}

@Preview
@Composable
@Suppress("MagicNumber")
fun ProductListPreview() {
    val products = listOf(
        ListItem.ProductListItem(
            productId = 1,
            title = "Product 1",
            type = SIMPLE,
            imageUrl = null,
            numVariations = 0,
            stockAndPrice = "Not in stock • $25.00",
            sku = "1234",
            selectionState = SELECTED
        ),

        ListItem.ProductListItem(
            productId = 2,
            title = "Product 2",
            type = VARIABLE,
            imageUrl = null,
            numVariations = 3,
            stockAndPrice = "In stock • $5.00 • 3 variations",
            sku = "33333",
            selectionState = PARTIALLY_SELECTED
        ),

        ListItem.ProductListItem(
            productId = 3,
            title = "Product 3",
            type = GROUPED,
            imageUrl = "",
            numVariations = 0,
            stockAndPrice = "Out of stock",
            sku = null
        ),

        ListItem.ProductListItem(
            productId = 4,
            title = "Product 4",
            type = GROUPED,
            imageUrl = null,
            numVariations = 0,
            stockAndPrice = null,
            sku = null
        ),

        ListItem.ConfigurableListItem(
            productId = 5,
            title = "Product 5",
            type = BUNDLE,
            imageUrl = null,
            stockAndPrice = null,
            sku = null
        )
    )

    ProductList(
        state = ViewState(
            products = products,
            selectedItemsCount = 3,
            loadingState = IDLE,
            filterState = FilterState(),
            searchState = ProductSelectorViewModel.SearchState(),
            popularProducts = products,
            recentProducts = products,
            selectionMode = SelectionMode.MULTIPLE
        ),
        onDoneButtonClick = {},
        onClearButtonClick = {},
        onFilterButtonClick = {},
        onProductClick = { _, _ -> },
        onLoadMore = {},
        trackConfigurableProduct = {},
        onEditConfiguration = {}
    )
}

@Preview
@Composable
fun ProductListEmptyPreview() {
    EmptyProductList(
        state = ViewState(
            products = emptyList(),
            selectedItemsCount = 3,
            loadingState = IDLE,
            filterState = FilterState(),
            searchState = ProductSelectorViewModel.SearchState(),
            popularProducts = emptyList(),
            recentProducts = emptyList(),
            selectionMode = SelectionMode.MULTIPLE
        ),
        onClearFiltersButtonClick = {}
    )
}

@Preview
@Composable
fun ProductListSkeletonPreview() {
    ProductListSkeleton()
}

@Composable
fun ListItem.getInformation(): String? {
    return if (type == com.woocommerce.android.ui.products.ProductType.BUNDLE) {
        listOfNotNull(stringResource(id = string.product_type_bundle), stockAndPrice)
            .joinToString(" \u2022 ")
    } else {
        stockAndPrice
    }
}
