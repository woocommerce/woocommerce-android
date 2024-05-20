package com.woocommerce.android.ui.woopos.cart.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.woocommerce.android.ui.woopos.util.WooPosPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun ProductSelector(
    productsState: StateFlow<ProductSelectorViewModel.ViewState>,
    onEndOfProductsGridReached: () -> Unit,
) {
    ConstraintLayout(
        modifier = Modifier.fillMaxWidth(0.7f)
    ) {
        val state = productsState.collectAsState()
        val gridState = rememberLazyGridState()
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 128.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            state = gridState
        ) {
            items(
                count = state.value.products.size,
                key = { index -> state.value.products[index].productId }
            ) { index ->
                ProductItem(product = state.value.products[index])
            }
        }
        InfiniteGridHandler(gridState) {
            onEndOfProductsGridReached()
        }
    }
}

@Composable
fun ProductItem(product: ProductSelectorViewModel.ListItem) {
    ConstraintLayout(
        modifier = Modifier.background(Color.Yellow)
    ) {
        Text(
            text = product.title,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSurface,
        )
    }
}

@Composable
fun InfiniteGridHandler(gridState: LazyGridState, buffer: Int = 1, onEndOfProductsGridReached: () -> Unit) {
    val loadMore = remember {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1

            lastVisibleItemIndex > (totalItemsNumber - buffer)
        }
    }

    LaunchedEffect(loadMore) {
        snapshotFlow { loadMore.value }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                onEndOfProductsGridReached()
            }
    }
}

@Composable
@WooPosPreview
fun ProductSelectorPreview() {
    val state = MutableStateFlow(
        ProductSelectorViewModel.ViewState(
            listOf(
                ProductSelectorViewModel.ListItem(1, "Product 1"),
                ProductSelectorViewModel.ListItem(2, "Product 2"),
                ProductSelectorViewModel.ListItem(3, "Product 3"),
            )
        )
    )
    ProductSelector(productsState = state, onEndOfProductsGridReached = {})
}
