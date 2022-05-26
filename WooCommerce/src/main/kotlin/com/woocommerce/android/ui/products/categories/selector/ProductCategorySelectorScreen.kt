package com.woocommerce.android.ui.products.categories.selector

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.categories.selector.ProductCategorySelectorViewModel.CategoryUiModel

@Composable
fun CategorySelectorScreen(viewModel: ProductCategorySelectorViewModel) {
    val viewState by viewModel.viewState.observeAsState()
    viewState?.let {
        CategorySelectorScreen(viewState = it)
    }
}

@Composable
fun CategorySelectorScreen(
    viewState: ProductCategorySelectorViewModel.ViewState
) {
    LazyColumn(modifier = Modifier.background(MaterialTheme.colors.surface)) {
        viewState.categories.forEach {
            categoryItem(item = it)
        }
    }
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
