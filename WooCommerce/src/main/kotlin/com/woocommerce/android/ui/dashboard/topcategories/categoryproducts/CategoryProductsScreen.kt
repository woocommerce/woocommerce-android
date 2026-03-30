package com.woocommerce.android.ui.dashboard.topcategories.categoryproducts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.ProductThumbnail
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.dashboard.TopPerformerProductUiModel
import com.woocommerce.android.ui.dashboard.topcategories.categoryproducts.CategoryProductsViewModel.CategoryProductsState

@Composable
fun CategoryProductsScreen(
    state: CategoryProductsState,
    onRetryClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        state.isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        state.isError -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(id = R.string.error_generic),
                        style = MaterialTheme.typography.body1
                    )
                    WCTextButton(onClick = onRetryClicked) {
                        Text(text = stringResource(id = R.string.retry))
                    }
                }
            }
        }

        state.products.isEmpty() -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.dashboard_top_categories_empty),
                    style = MaterialTheme.typography.body1
                )
            }
        }

        else -> {
            LazyColumn(modifier = modifier.fillMaxSize()) {
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = stringResource(id = R.string.product),
                            style = MaterialTheme.typography.body2,
                            color = colorResource(id = R.color.color_on_surface_medium_selector),
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(id = R.string.dashboard_top_performers_items_sold),
                            style = MaterialTheme.typography.body2,
                            color = colorResource(id = R.color.color_on_surface_medium_selector),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                itemsIndexed(state.products) { index, product ->
                    CategoryProductItem(
                        product = product,
                        displayDivider = index != state.products.size - 1
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryProductItem(
    product: TopPerformerProductUiModel,
    modifier: Modifier = Modifier,
    displayDivider: Boolean
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { product.onClick(product.productId) }
            .padding(start = 16.dp, end = 16.dp, top = 8.dp),
    ) {
        ProductThumbnail(
            imageUrl = product.imageUrl ?: "",
            contentDescription = stringResource(id = R.string.product_image_content_description),
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Row(modifier = Modifier.padding(bottom = 4.dp, end = 16.dp)) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = product.name,
                    style = MaterialTheme.typography.subtitle1,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = product.timesOrdered,
                    style = MaterialTheme.typography.subtitle1,
                )
            }
            Text(
                text = product.netSales,
                style = MaterialTheme.typography.body2,
                color = colorResource(id = R.color.color_on_surface_medium_selector)
            )
            if (displayDivider) {
                Divider(modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
