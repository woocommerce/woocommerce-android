package com.woocommerce.android.ui.woopos.home.products

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun WooPosProductsScreen() {
    val productsViewModel: WooPosProductsViewModel = hiltViewModel()
    WooPosProductsScreen(
        productsState = productsViewModel.viewState,
        onEndOfProductsGridReached = productsViewModel::onEndOfProductsGridReached,
    )
}

@Composable
private fun WooPosProductsScreen(
    productsState: StateFlow<WooPosProductsViewState>,
    onEndOfProductsGridReached: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 4.dp,
        modifier = Modifier.padding(16.dp)
    ) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            ProductSelector(productsState, onEndOfProductsGridReached)
        }
    }
}

@Composable
fun ProductSelector(
    productsState: StateFlow<WooPosProductsViewState>,
    onEndOfProductsGridReached: () -> Unit,
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxHeight()
            .background(MaterialTheme.colors.surface, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        val state = productsState.collectAsState()
        val gridState = rememberLazyGridState()
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(34.dp),
            verticalArrangement = Arrangement.spacedBy(34.dp),
            contentPadding = PaddingValues(16.dp),
            state = gridState
        ) {
            itemsIndexed(state.value.products) { _, product ->
                ProductItem(product = product)
            }
        }
        InfiniteGridHandler(gridState) {
            onEndOfProductsGridReached()
        }
    }
}

@Composable
private fun ProductItem(product: WooPosProductsListItem) {
    ConstraintLayout(
        modifier = Modifier
            .border(1.dp, Color.Gray, shape = RoundedCornerShape(4.dp))
            .padding(16.dp)
            .fillMaxWidth(0.5f)
    ) {
        Text(
            text = product.title,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSurface,
        )
    }
}

@Composable
private fun InfiniteGridHandler(
    gridState: LazyGridState,
    buffer: Int = 1,
    onEndOfProductsGridReached: () -> Unit
) {
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
fun WooPosHomeScreenPreview() {
    val productState = MutableStateFlow(
        WooPosProductsViewState(
            products = listOf(
                WooPosProductsListItem(1, "Product 1"),
                WooPosProductsListItem(2, "Product 2"),
                WooPosProductsListItem(3, "Product 3"),
            )
        )
    )
    WooPosProductsScreen(
        productsState = productState,
        onEndOfProductsGridReached = {}
    )
}
