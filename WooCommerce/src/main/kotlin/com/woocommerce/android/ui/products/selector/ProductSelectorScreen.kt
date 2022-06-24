package com.woocommerce.android.ui.products.selector

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
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
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
import com.woocommerce.android.R
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.InfiniteListHandler
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCSearchField
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.products.ProductType.GROUPED
import com.woocommerce.android.ui.products.ProductType.SIMPLE
import com.woocommerce.android.ui.products.ProductType.VARIABLE
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.LoadingState.APPENDING
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.LoadingState.IDLE
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.LoadingState.LOADING
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.ProductListItem
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.ViewState
import com.woocommerce.android.ui.products.selector.SelectionState.PARTIALLY_SELECTED
import com.woocommerce.android.ui.products.selector.SelectionState.SELECTED
import com.woocommerce.android.ui.products.selector.components.SelectorListItem
import com.woocommerce.android.util.StringUtils

@Composable
fun ProductSelectorScreen(viewModel: ProductSelectorViewModel) {
    val viewState by viewModel.viewState.observeAsState()
    viewState?.let {
        ProductSelectorScreen(
            state = it,
            onDoneButtonClick = viewModel::onDoneButtonClick,
            onClearButtonClick = viewModel::onClearButtonClick,
            onProductClick = viewModel::onProductClick,
            onLoadMore = viewModel::onLoadMore,
            onSearchQueryChanged = viewModel::onSearchQueryChanged
        )
    }
}

@Composable
fun ProductSelectorScreen(
    state: ViewState,
    onDoneButtonClick: () -> Unit,
    onClearButtonClick: () -> Unit,
    onProductClick: (ProductListItem) -> Unit,
    onLoadMore: () -> Unit,
    onSearchQueryChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface)
    ) {
        WCSearchField(
            value = state.searchQuery,
            onValueChange = onSearchQueryChanged,
            hint = stringResource(id = string.product_selector_search_hint),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(id = dimen.major_100),
                    vertical = dimensionResource(id = dimen.minor_100)
                )
        )
        when {
            state.products.isNotEmpty() -> ProductList(
                state = state,
                onDoneButtonClick = onDoneButtonClick,
                onClearButtonClick = onClearButtonClick,
                onProductClick = onProductClick,
                onLoadMore = onLoadMore
            )
            state.products.isEmpty() && state.loadingState == LOADING -> ProductListSkeleton()
            else -> EmptyProductList(state.searchQuery)
        }
    }
}

@Composable
private fun EmptyProductList(searchQuery: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(id = dimen.major_200)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val message = if (searchQuery.isEmpty()) {
            stringResource(id = string.product_selector_empty_state)
        } else {
            stringResource(id = string.empty_message_with_search, searchQuery)
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
    }
}

@Composable
private fun ProductList(
    state: ViewState,
    onDoneButtonClick: () -> Unit,
    onClearButtonClick: () -> Unit,
    onProductClick: (ProductListItem) -> Unit,
    onLoadMore: () -> Unit,
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
            if (state.selectedItemsCount > 0) {
                WCTextButton(
                    onClick = onClearButtonClick,
                    text = stringResource(id = string.product_selector_clear_button_title),
                    allCaps = false,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }

            WCTextButton(
                onClick = { },
                text = stringResource(id = string.product_selector_filter_button_title),
                allCaps = false,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            itemsIndexed(state.products) { _, product ->
                SelectorListItem(
                    title = product.title,
                    imageUrl = product.imageUrl,
                    infoLine1 = product.stockAndPrice,
                    infoLine2 = product.sku?.let {
                        stringResource(string.product_selector_sku_value, product.sku)
                    },
                    selectionState = product.selectionState,
                    isArrowVisible = product.type == VARIABLE && product.numVariations > 0,
                    onClickLabel = stringResource(id = string.product_selector_select_product_label, product.title),
                    imageContentDescription = stringResource(string.product_image_content_description)
                ) {
                    onProductClick(product)
                }
                Divider(
                    modifier = Modifier.padding(start = dimensionResource(id = dimen.major_100)),
                    color = colorResource(id = R.color.divider_color),
                    thickness = dimensionResource(id = dimen.minor_10)
                )
            }
            if (state.loadingState == APPENDING) {
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

        Divider(
            color = colorResource(id = R.color.divider_color),
            thickness = dimensionResource(id = dimen.minor_10)
        )

        WCColoredButton(
            onClick = onDoneButtonClick,
            text = StringUtils.getQuantityString(
                quantity = state.selectedItemsCount,
                default = string.product_selector_select_button_title_default,
                one = string.product_selector_select_button_title_one,
                zero = string.done
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = dimen.major_100))
        )
    }
}

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
                    color = colorResource(id = R.color.divider_color),
                    thickness = dimensionResource(id = dimen.minor_10)
                )
            }
        }
    }
}

@Preview
@Composable
@Suppress("MagicNumber")
fun ProductListPreview() {
    val products = listOf(
        ProductListItem(
            id = 1,
            title = "Product 1",
            type = SIMPLE,
            imageUrl = null,
            numVariations = 0,
            stockAndPrice = "Not in stock • $25.00",
            sku = "1234",
            selectionState = SELECTED
        ),

        ProductListItem(
            id = 2,
            title = "Product 2",
            type = VARIABLE,
            imageUrl = null,
            numVariations = 3,
            stockAndPrice = "In stock • $5.00 • 3 variations",
            sku = "33333",
            selectionState = PARTIALLY_SELECTED
        ),

        ProductListItem(
            id = 3,
            title = "Product 3",
            type = GROUPED,
            imageUrl = "",
            numVariations = 0,
            stockAndPrice = "Out of stock",
            sku = null
        ),

        ProductListItem(
            id = 4,
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
            products = products,
            selectedItemsCount = 3,
            loadingState = IDLE,
            searchQuery = ""
        ),
        {},
        {},
        {},
        {}
    )
}

@Preview
@Composable
fun ProductListEmptyPreview() {
    EmptyProductList("")
}

@Preview
@Composable
fun ProductListSkeletonPreview() {
    ProductListSkeleton()
}
