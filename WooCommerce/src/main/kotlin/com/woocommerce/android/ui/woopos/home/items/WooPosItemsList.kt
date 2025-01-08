package com.woocommerce.android.ui.woopos.home.items

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.ShadowType
import com.woocommerce.android.ui.woopos.common.composeui.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosLazyColumn
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerBox
import com.woocommerce.android.ui.woopos.home.items.WooPosItem.SimpleProduct
import com.woocommerce.android.ui.woopos.home.items.WooPosItem.VariableProduct
import com.woocommerce.android.ui.woopos.home.items.WooPosItem.Variation
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WooPosItemList(
    state: ContentViewState,
    listState: LazyListState,
    onItemClicked: (item: WooPosItem) -> Unit,
    onEndOfProductsListReached: () -> Unit,
    onErrorWhilePaginating: @Composable () -> Unit,
) {
    WooPosLazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(2.dp),
        state = listState,
    ) {
        items(
            state.items,
            key = { product -> product.id }
        ) { product ->
            when (product) {
                is SimpleProduct -> {
                    ProductItem(
                        modifier = Modifier.animateItem(),
                        item = product,
                        onItemClicked = onItemClicked
                    )
                }

                is VariableProduct -> {
                    VariableProductItem(
                        modifier = Modifier.animateItem(),
                        item = product,
                        onItemClicked = onItemClicked
                    )
                }

                is Variation -> {
                    VariationItem(
                        modifier = Modifier.animateItem(),
                        item = product,
                        onItemClicked = onItemClicked
                    )
                }
            }
        }

        when (state.paginationState) {
            PaginationState.Error -> {
                item {
                    onErrorWhilePaginating()
                }
            }
            PaginationState.Loading -> {
                item {
                    ItemsLoadingItem()
                }
            }
            PaginationState.None -> {
            }
        }

        item {
            Spacer(modifier = Modifier.height(104.dp))
        }
    }
    InfiniteListHandler(listState, state) {
        onEndOfProductsListReached()
    }
}

@Composable
private fun ProductItem(
    modifier: Modifier = Modifier,
    item: SimpleProduct,
    onItemClicked: (item: WooPosItem) -> Unit
) {
    val itemContentDescription = stringResource(
        id = R.string.woopos_product_item_content_description,
        item.name,
        item.price
    )
    ItemCard(modifier, itemContentDescription, onItemClicked, item)
}

@Composable
private fun VariableProductItem(
    modifier: Modifier = Modifier,
    item: VariableProduct,
    onItemClicked: (item: WooPosItem) -> Unit
) {
    val itemContentDescription = stringResource(
        id = R.string.woopos_variable_product_item_content_description,
        item.name,
        item.price
    )
    ItemCard(modifier, itemContentDescription, onItemClicked, item)
}

@Composable
private fun VariationItem(
    modifier: Modifier = Modifier,
    item: Variation,
    onItemClicked: (item: WooPosItem) -> Unit
) {
    val itemContentDescription = stringResource(
        id = R.string.woopos_variation_item_content_description,
        item.name,
        item.price
    )
    ItemCard(modifier, itemContentDescription, onItemClicked, item)
}

@Composable
private fun ItemCard(
    modifier: Modifier,
    itemContentDescription: String,
    onItemClicked: (item: WooPosItem) -> Unit,
    item: WooPosItem
) {
    WooPosCard(
        modifier = modifier
            .semantics { contentDescription = itemContentDescription },
        shape = RoundedCornerShape(8.dp),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 6.dp,
        shadowType = ShadowType.Soft,
    ) {
        Row(
            modifier = Modifier
                .clickable { onItemClicked(item) }
                .height(112.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductImage(item)

            Spacer(modifier = Modifier.width(32.dp))

            ProductInfo(item)
        }
    }
}

@Composable
private fun ProductInfo(item: WooPosItem) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = item.name,
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(8.dp))
        when (item) {
            is SimpleProduct -> SimpleProductDetails(item = item)
            is VariableProduct -> VariableProductDetails(item = item)
            is Variation -> VariationProductDetails(item = item)
        }
    }
}

@Composable
private fun ProductImage(item: WooPosItem) {
    val imageUrl = when (item) {
        is SimpleProduct -> item.imageUrl
        is VariableProduct -> item.imageUrl
        is Variation -> item.imageUrl
    }

    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(true)
            .build(),
        fallback = ColorPainter(WooPosTheme.colors.loadingSkeleton),
        error = ColorPainter(WooPosTheme.colors.loadingSkeleton),
        placeholder = ColorPainter(WooPosTheme.colors.loadingSkeleton),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.size(112.dp)
    )
}

@Composable
private fun SimpleProductDetails(item: SimpleProduct) {
    Text(
        text = item.price,
        style = MaterialTheme.typography.h6,
        fontWeight = FontWeight.Normal
    )
}

@Composable
private fun VariableProductDetails(item: VariableProduct) {
    Text(
        text = "${item.numOfVariations} Variations",
        style = MaterialTheme.typography.h6,
        fontWeight = FontWeight.Normal
    )
}

@Composable
fun VariationProductDetails(item: Variation) {
    Text(
        text = item.price,
        style = MaterialTheme.typography.h6,
        fontWeight = FontWeight.Normal
    )
}

@Composable
fun ItemsLoadingIndicator(itemsCount: Int = 10) {
    WooPosLazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(2.dp),
    ) {
        items(itemsCount) {
            ItemsLoadingItem()
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ItemsLoadingItem() {
    WooPosCard(
        shape = RoundedCornerShape(8.dp),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 6.dp,
        shadowType = ShadowType.Soft,
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
private fun InfiniteListHandler(
    listState: LazyListState,
    state: ContentViewState,
    onEndOfProductsListReached: () -> Unit
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

    LaunchedEffect(state.reloadingProductsWithPullToRefresh) {
        snapshotFlow { loadMore.value }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                onEndOfProductsListReached()
            }
    }
}
