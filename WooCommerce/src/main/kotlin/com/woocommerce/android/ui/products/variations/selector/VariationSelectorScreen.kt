package com.woocommerce.android.ui.products.variations.selector

import android.annotation.SuppressLint
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
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.InfiniteListHandler
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.products.selector.SelectionState.SELECTED
import com.woocommerce.android.ui.products.selector.components.SelectorListItem
import com.woocommerce.android.ui.products.variations.selector.VariationSelectorViewModel.LoadingState.APPENDING
import com.woocommerce.android.ui.products.variations.selector.VariationSelectorViewModel.LoadingState.LOADING
import com.woocommerce.android.ui.products.variations.selector.VariationSelectorViewModel.VariationListItem
import com.woocommerce.android.ui.products.variations.selector.VariationSelectorViewModel.ViewState

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun VariationSelectorScreen(viewModel: VariationSelectorViewModel) {
    val viewState by viewModel.viewSate.observeAsState(ViewState())
    BackHandler(onBack = viewModel::onBackPress)
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = viewState.productName
                    )
                },
                navigationIcon = {
                    IconButton(viewModel::onBackPress) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = string.back)
                        )
                    }
                },
                backgroundColor = colorResource(id = R.color.color_toolbar),
                elevation = 0.dp,
            )
        },
        content = {
            VariationSelectorScreen(
                state = viewState,
                onClearButtonClick = viewModel::onClearButtonClick,
                onVariationClick = viewModel::onVariationClick,
                onLoadMore = viewModel::onLoadMore
            )
        }
    )
}

@Composable
fun VariationSelectorScreen(
    state: ViewState,
    onClearButtonClick: () -> Unit,
    onVariationClick: (VariationListItem) -> Unit,
    onLoadMore: () -> Unit
) {
    when {
        state.variations.isNotEmpty() -> VariationList(
            state = state,
            onClearButtonClick = onClearButtonClick,
            onVariationClick = onVariationClick,
            onLoadMore = onLoadMore
        )
        state.variations.isEmpty() && state.loadingState == LOADING -> VariationListSkeleton()
        else -> EmptyVariationList()
    }
}

@Composable
fun EmptyVariationList() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(id = dimen.major_200)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = string.product_list_empty),
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
private fun VariationList(
    state: ViewState,
    onClearButtonClick: () -> Unit,
    onVariationClick: (VariationListItem) -> Unit,
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
            WCTextButton(
                onClick = onClearButtonClick,
                text = stringResource(id = string.product_selector_clear_button_title),
                allCaps = false,
                enabled = state.selectedItemsCount > 0,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            itemsIndexed(state.variations) { _, variation ->
                SelectorListItem(
                    title = variation.title,
                    imageUrl = variation.imageUrl,
                    infoLine1 = variation.stockAndPrice,
                    infoLine2 = variation.sku?.let {
                        stringResource(string.product_selector_sku_value, variation.sku)
                    },
                    selectionState = variation.selectionState,
                    isArrowVisible = false,
                    onClickLabel = stringResource(id = string.product_selector_select_variation_label, variation.title),
                    imageContentDescription = stringResource(string.product_image_content_description),
                    isCogwheelVisible = false,
                    enabled = true,
                    onEditConfiguration = {}
                ) {
                    onVariationClick(variation)
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
    }
}

@Composable
@Suppress("MagicNumber")
fun VariationListSkeleton() {
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
fun VariationListPreview() {
    val variations = listOf(
        VariationListItem(
            id = 1,
            title = "Variation 1",
            imageUrl = null,
            stockAndPrice = "Not in stock • $25.00",
            sku = "1234",
            selectionState = SELECTED
        ),

        VariationListItem(
            id = 2,
            title = "Variation 2",
            imageUrl = null,
            stockAndPrice = "In stock • $5.00",
            sku = "33333",
            selectionState = SELECTED
        ),

        VariationListItem(
            id = 3,
            title = "Variation 3",
            imageUrl = "",
            stockAndPrice = "Out of stock",
            sku = null
        ),

        VariationListItem(
            id = 4,
            title = "Variation 4",
            imageUrl = null,
            stockAndPrice = null,
            sku = null
        )
    )

    VariationList(state = ViewState(variations = variations, selectedItemsCount = 3), {}, {}, {})
}

@Preview
@Composable
fun VariationListEmptyPreview() {
    EmptyVariationList()
}

@Preview
@Composable
fun VariationListSkeletonPreview() {
    VariationListSkeleton()
}
