package com.woocommerce.android.ui.products.categories.selector

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.InfiniteListHandler
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCSearchField
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.products.categories.selector.ProductCategorySelectorViewModel.CategoryUiModel
import com.woocommerce.android.ui.products.categories.selector.ProductCategorySelectorViewModel.LoadingState
import com.woocommerce.android.util.StringUtils

@Composable
fun ProductCategorySelectorScreen(viewModel: ProductCategorySelectorViewModel) {
    val viewState by viewModel.viewState.observeAsState()
    viewState?.let {
        ProductCategorySelectorScreen(
            viewState = it,
            onLoadMore = viewModel::onLoadMore,
            onClearSelectionClick = viewModel::onClearSelectionClick,
            onSearchQueryChanged = viewModel::onSearchQueryChanged,
            onDoneClick = viewModel::onDoneClick
        )
    }
}

@Composable
fun ProductCategorySelectorScreen(
    viewState: ProductCategorySelectorViewModel.ViewState,
    onLoadMore: () -> Unit = {},
    onClearSelectionClick: () -> Unit = {},
    onSearchQueryChanged: (String) -> Unit = {},
    onDoneClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface)
    ) {
        WCSearchField(
            value = viewState.searchQuery,
            onValueChange = onSearchQueryChanged,
            hint = stringResource(id = R.string.product_category_selector_search_hint),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(id = R.dimen.major_100),
                    vertical = dimensionResource(id = R.dimen.minor_100)
                )
        )
        when {
            viewState.categories.isNotEmpty() -> CategoriesList(
                viewState = viewState,
                onLoadMore = onLoadMore,
                onClearSelectionClick = onClearSelectionClick,
                onDoneClick = onDoneClick
            )
            viewState.loadingState == LoadingState.Loading -> CategoriesSkeleton()
            else -> EmptyCategoriesList(viewState.searchQuery)
        }
    }
}

@Composable
private fun CategoriesList(
    viewState: ProductCategorySelectorViewModel.ViewState,
    onLoadMore: () -> Unit,
    onClearSelectionClick: () -> Unit,
    onDoneClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
    ) {
        if (viewState.selectedCategoriesCount > 0) {
            WCTextButton(
                onClick = onClearSelectionClick,
                modifier = Modifier.padding(dimensionResource(id = R.dimen.minor_100))
            ) {
                Text(text = stringResource(id = R.string.product_category_selector_clear_selection))
            }
        }
        val lazyListState = rememberLazyListState()
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.weight(1f)
        ) {
            viewState.categories.forEach {
                categoryItem(item = it)
            }

            if (viewState.loadingState == LoadingState.Appending) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth()
                            .padding(vertical = dimensionResource(id = R.dimen.minor_100))
                    )
                }
            }
        }

        InfiniteListHandler(listState = lazyListState, onLoadMore = onLoadMore, buffer = 3)

        Divider(
            color = colorResource(id = R.color.divider_color),
            thickness = dimensionResource(id = R.dimen.minor_10)
        )

        WCColoredButton(
            onClick = onDoneClick,
            text = StringUtils.getQuantityString(
                quantity = viewState.selectedCategoriesCount,
                default = R.string.product_category_selector_select_button_title_default,
                zero = R.string.done,
                one = R.string.product_category_selector_select_button_title_one
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.major_100)),
        )
    }
}

private fun LazyListScope.categoryItem(item: CategoryUiModel, depth: Int = 0) {
    item {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = dimensionResource(id = R.dimen.line_height_major_100))
                    .clickable(onClick = item.onItemClick)
                    .padding(dimensionResource(id = R.dimen.major_100))
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier
                        .padding(start = dimensionResource(id = R.dimen.major_100) * depth)
                        .weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(
                        id = R.string.product_category_selector_check_content_description
                    ),
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.alpha(if (item.isSelected) 1f else 0f)
                )
            }

            Divider(modifier = Modifier.padding(start = dimensionResource(id = R.dimen.major_100) * (depth + 1)))
        }
    }
    item.children.forEach {
        this@categoryItem.categoryItem(item = it, depth + 1)
    }
}

@Composable
private fun EmptyCategoriesList(searchQuery: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(id = R.dimen.major_200)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val message = if (searchQuery.isEmpty()) {
            stringResource(id = R.string.product_category_selector_empty_state)
        } else {
            stringResource(id = R.string.empty_message_with_search, searchQuery)
        }
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(
                start = dimensionResource(id = R.dimen.major_150),
                end = dimensionResource(id = R.dimen.major_150)
            )
        )
        Spacer(Modifier.size(dimensionResource(id = R.dimen.major_325)))
        Image(
            painter = painterResource(id = R.drawable.img_empty_products),
            contentDescription = null,
        )
    }
}

@Composable
private fun CategoriesSkeleton() {
    val numberOfInboxSkeletonRows = 20
    LazyColumn {
        item {
            SkeletonView(
                modifier = Modifier
                    .padding(dimensionResource(id = R.dimen.major_100))
                    .size(
                        width = dimensionResource(id = R.dimen.skeleton_text_medium_width),
                        height = dimensionResource(R.dimen.major_125)
                    )
            )
        }
        repeat(numberOfInboxSkeletonRows) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_50)),
                    modifier = Modifier.padding(
                        horizontal = dimensionResource(id = R.dimen.major_100),
                        vertical = dimensionResource(id = R.dimen.minor_100)
                    )
                ) {
                    val width = dimensionResource(
                        id = if (it.mod(2) == 0) R.dimen.skeleton_text_large_width
                        else R.dimen.skeleton_text_medium_width
                    )
                    SkeletonView(
                        width = width,
                        height = dimensionResource(id = R.dimen.major_125)
                    )
                }
                Divider(
                    modifier = Modifier.padding(start = dimensionResource(id = R.dimen.major_100))
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewProductCategorySelector() {
    fun generateCategory(id: Long, childrenDepth: Int): CategoryUiModel {
        return CategoryUiModel(
            id = id,
            title = "Category $id",
            children = if (childrenDepth > 0) {
                listOf(generateCategory("$childrenDepth$id".toLong(), childrenDepth - 1))
            } else emptyList(),
            isSelected = (id.mod(2)) == 0,
            onItemClick = {}
        )
    }

    val categories = remember {
        (1L..10L).map {
            generateCategory(it, childrenDepth = it.coerceAtMost(4).toInt())
        }
    }
    WooThemeWithBackground {
        ProductCategorySelectorScreen(
            viewState = ProductCategorySelectorViewModel.ViewState(
                categories = categories,
                selectedCategoriesCount = 1,
                loadingState = LoadingState.Idle,
                searchQuery = ""
            )
        )
    }
}
