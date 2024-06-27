package com.woocommerce.android.ui.woopos.home.products

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.home.products.WooPosProductsUIEvent.EndOfProductsGridReached
import com.woocommerce.android.ui.woopos.home.products.WooPosProductsUIEvent.ItemClicked
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun WooPosProductsScreen() {
    val viewModel: WooPosProductsViewModel = hiltViewModel()
    WooPosProductsScreen(
        productsState = viewModel.viewState,
        onItemClicked = { viewModel.onUIEvent(ItemClicked(it)) },
        onEndOfProductsGridReached = { viewModel.onUIEvent(EndOfProductsGridReached) },
    )
}

@Composable
private fun WooPosProductsScreen(
    productsState: StateFlow<WooPosProductsViewState>,
    onItemClicked: (item: WooPosProductsListItem) -> Unit,
    onEndOfProductsGridReached: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        ProductSelector(productsState, onItemClicked, onEndOfProductsGridReached)
    }
}

@Composable
fun ProductSelector(
    productsState: StateFlow<WooPosProductsViewState>,
    onItemClicked: (item: WooPosProductsListItem) -> Unit,
    onEndOfProductsGridReached: () -> Unit,
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxHeight()
            .padding(16.dp)
    ) {
        val state = productsState.collectAsState()
        val listState = rememberLazyListState()
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(2.dp),
            state = listState
        ) {
            itemsIndexed(state.value.products) { _, product ->
                ProductItem(item = product, onItemClicked = onItemClicked)
            }
        }
        InfiniteListHandler(listState) {
            onEndOfProductsGridReached()
        }
    }
}

@Composable
private fun ProductItem(
    item: WooPosProductsListItem,
    onItemClicked: (item: WooPosProductsListItem) -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 2.dp,
    ) {
        ConstraintLayout(
            modifier = Modifier
                .clickable { onItemClicked(item) }
                .height(112.dp)
                .fillMaxWidth()
        ) {
            val (textRef) = createRefs()

            Text(
                text = item.title,
                style = MaterialTheme.typography.h4,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier.constrainAs(textRef) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
            )
        }
    }
}

@Composable
private fun InfiniteListHandler(
    listState: LazyListState,
    buffer: Int = 1,
    onEndOfProductsGridReached: () -> Unit
) {
    val loadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
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
    WooPosTheme {
        WooPosProductsScreen(
            productsState = productState,
            onItemClicked = {},
            onEndOfProductsGridReached = {}
        )
    }
}
