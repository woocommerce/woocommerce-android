package com.woocommerce.android.ui.products.categories.selector

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.InfiniteListHandler
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.products.categories.selector.ProductCategorySelectorViewModel.CategoryUiModel

@Composable
fun ProductCategorySelectorScreen(viewModel: ProductCategorySelectorViewModel) {
    val viewState by viewModel.viewState.observeAsState()
    viewState?.let {
        ProductCategorySelectorScreen(
            viewState = it,
            onLoadMore = viewModel::onLoadMore
        )
    }
}

@Composable
fun ProductCategorySelectorScreen(
    viewState: ProductCategorySelectorViewModel.ViewState,
    onLoadMore: () -> Unit = {}
) {
    val lazyListState = rememberLazyListState()
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.background(MaterialTheme.colors.surface)
    ) {
        viewState.categories.forEach {
            categoryItem(item = it)
        }
    }

    InfiniteListHandler(listState = lazyListState, onLoadMore = onLoadMore)
}

private fun LazyListScope.categoryItem(item: CategoryUiModel, depth: Int = 0) {
    item {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100))) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier.padding(
                        start = dimensionResource(id = R.dimen.major_100) * depth,
                    )
                )
            }

            Divider(modifier = Modifier.padding(start = dimensionResource(id = R.dimen.major_100) * (depth + 1)))
        }
    }
    item.children.forEach {
        this@categoryItem.categoryItem(item = it, depth + 1)
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
            isSelected = (id.mod(2)) == 0
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
                categories = categories
            )
        )
    }
}
