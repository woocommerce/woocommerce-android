package com.woocommerce.android.ui.woopos.home.products

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerBox
import com.woocommerce.android.ui.woopos.home.products.WooPosProductsUIEvent.EndOfProductsGridReached
import com.woocommerce.android.ui.woopos.home.products.WooPosProductsUIEvent.ItemClicked
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
fun WooPosProductsScreen() {
    val productsViewModel: WooPosProductsViewModel = hiltViewModel()
    WooPosProductsScreen(
        productsStateFlow = productsViewModel.viewState,
        onItemClicked = { productsViewModel.onUIEvent(ItemClicked(it)) },
        onEndOfProductsGridReached = { productsViewModel.onUIEvent(EndOfProductsGridReached) },
    )
}

@Composable
private fun WooPosProductsScreen(
    productsStateFlow: StateFlow<WooPosProductsViewState>,
    onItemClicked: (item: WooPosProductsListItem) -> Unit,
    onEndOfProductsGridReached: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(top = 36.dp, start = 40.dp, end = 40.dp, bottom = 0.dp)
    ) {
        Text(
            text = stringResource(id = R.string.woopos_products_screen_title),
            style = MaterialTheme.typography.h3,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        val state = productsStateFlow.collectAsState()

        when (val productsState = state.value) {
            is WooPosProductsViewState.Content -> {
                ProductsList(productsState, onItemClicked, onEndOfProductsGridReached)
            }

            WooPosProductsViewState.Loading -> {
                ProductsLoadingIndicator()
            }

            WooPosProductsViewState.Empty -> TODO()
            WooPosProductsViewState.Error -> TODO()
        }
    }
}

@Composable
private fun ProductsList(
    state: WooPosProductsViewState.Content,
    onItemClicked: (item: WooPosProductsListItem) -> Unit,
    onEndOfProductsGridReached: () -> Unit,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(2.dp),
        state = listState
    ) {
        items(
            state.products,
            key = { product -> product.id }
        ) { product ->
            ProductItem(item = product, onItemClicked = onItemClicked)
        }

        if (state.loadingMore) {
            item {
                ProductLoadingItem()
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    InfiniteListHandler(listState) {
        onEndOfProductsListReached()
    }
}

@Composable
fun ProductsLoadingIndicator() {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(2.dp),
    ) {
        items(10) {
            ProductLoadingItem()
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProductLoadingItem() {
    Card(
        shape = RoundedCornerShape(8.dp),
        backgroundColor = MaterialTheme.colors.surface,
    ) {
        Row(
            modifier = Modifier
                .height(112.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .background(WooPosTheme.colors.loadingSkeleton)
            )

            Spacer(modifier = Modifier.width(32.dp))

            WooPosShimmerBox(
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.width(184.dp))

            WooPosShimmerBox(
                modifier = Modifier
                    .height(30.dp)
                    .width(76.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.width(24.dp))
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
    ) {
        Row(
            modifier = Modifier
                .clickable { onItemClicked(item) }
                .height(112.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.imageUrl)
                    .crossfade(true)
                    .build(),
                fallback = ColorPainter(WooPosTheme.colors.loadingSkeleton),
                error = ColorPainter(WooPosTheme.colors.loadingSkeleton),
                placeholder = ColorPainter(WooPosTheme.colors.loadingSkeleton),
                contentDescription = stringResource(id = R.string.woopos_product_image_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(112.dp)
            )

            Spacer(modifier = Modifier.width(32.dp))

            Text(
                modifier = Modifier.weight(1f),
                text = item.name,
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(32.dp))

            Text(
                text = item.price,
                style = MaterialTheme.typography.h5,
            )

            Spacer(modifier = Modifier.width(24.dp))
        }
    }
}

@Composable
private fun InfiniteListHandler(
    listState: LazyListState,
    onEndOfProductsGridReached: () -> Unit
) {
    val buffer = 5
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
        WooPosProductsViewState.Content(
            products = listOf(
                WooPosProductsListItem(
                    1,
                    name = "Product 1, Product 1, Product 1, " +
                        "Product 1, Product 1, Product 1, Product 1, Product 1" +
                        "Product 1, Product 1, Product 1, Product 1, Product 1",
                    price = "10.0$",
                    imageUrl = null,
                ),
                WooPosProductsListItem(
                    2,
                    name = "Product 2",
                    price = "2000.00$",
                    imageUrl = null,
                ),
                WooPosProductsListItem(
                    3,
                    name = "Product 3",
                    price = "1.0$",
                    imageUrl = null,
                ),
            ),
            loadingMore = true,
        )
    )
    WooPosTheme {
        WooPosProductsScreen(
            productsStateFlow = productState,
            onItemClicked = {},
            onEndOfProductsGridReached = {}
        )
    }
}

@Composable
@WooPosPreview
fun WooPosHomeScreenLoadingPreview() {
    val productState = MutableStateFlow(WooPosProductsViewState.Loading)
    WooPosTheme {
        WooPosProductsScreen(
            productsStateFlow = productState,
            onItemClicked = {},
            onEndOfProductsGridReached = {}
        )
    }
}
