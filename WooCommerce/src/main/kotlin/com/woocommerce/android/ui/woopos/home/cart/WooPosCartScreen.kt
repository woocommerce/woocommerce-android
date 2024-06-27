@file:OptIn(ExperimentalFoundationApi::class)

package com.woocommerce.android.ui.woopos.home.cart

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton

@Composable
fun WooPosCartScreen() {
    val viewModel: WooPosCartViewModel = hiltViewModel()

    viewModel.state.observeAsState().value?.let {
        WooPosCartScreen(it, viewModel::onUIEvent)
    }
}

@Composable
private fun WooPosCartScreen(
    state: WooPosCartState,
    onUIEvent: (WooPosCartUIEvent) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 4.dp,
        modifier = Modifier.padding(16.dp)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Column {
                CartToolbar(
                    toolbar = state.toolbar,
                    onClearAllClicked = { onUIEvent(WooPosCartUIEvent.ClearAllClicked) },
                    onBackClicked = { onUIEvent(WooPosCartUIEvent.BackClicked) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                val listState = rememberLazyListState()
                ScrollToBottomHandler(state, listState)

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    items(
                        state.itemsInCart,
                        key = { item -> item.id.itemNumber }
                    ) { item ->
                        ProductItem(
                            modifier = Modifier.animateItemPlacement(),
                            item,
                            state.areItemsRemovable
                        ) { onUIEvent(WooPosCartUIEvent.ItemRemovedFromCart(item)) }
                    }
                    if (state.isCheckoutButtonVisible) {
                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
            }

            if (state.isCheckoutButtonVisible) {
                WooPosButton(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    enabled = state.itemsInCart.isNotEmpty() && !state.isOrderCreationInProgress,
                    text = stringResource(R.string.woo_pos_checkout_button),
                    onClick = { onUIEvent(WooPosCartUIEvent.CheckoutClicked) }
                )
            }
        }
    }
}

@Composable
private fun ScrollToBottomHandler(
    state: WooPosCartState,
    listState: LazyListState
) {
    val previousItemsCount = remember { mutableIntStateOf(0) }
    val itemsInCartSize = state.itemsInCart.size
    LaunchedEffect(itemsInCartSize) {
        if (itemsInCartSize > previousItemsCount.intValue) {
            listState.animateScrollToItem(itemsInCartSize - 1)
        }
        previousItemsCount.intValue = itemsInCartSize
    }
}

@Composable
private fun CartToolbar(
    toolbar: WooPosCartToolbar,
    onClearAllClicked: () -> Unit,
    onBackClicked: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onBackClicked() }) {
            Icon(
                imageVector = ImageVector.vectorResource(toolbar.icon),
                contentDescription = stringResource(R.string.woo_pos_cart_back_content_description),
                tint = MaterialTheme.colors.onBackground,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = stringResource(R.string.woo_pos_car_pane_title),
            style = MaterialTheme.typography.h4,
            color = MaterialTheme.colors.onBackground,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = toolbar.itemsCount,
            style = MaterialTheme.typography.h6,
            color = MaterialTheme.colors.secondaryVariant,
            fontWeight = FontWeight.SemiBold,
        )

        if (toolbar.isClearAllButtonVisible) {
            Spacer(modifier = Modifier.width(16.dp))

            TextButton(onClick = { onClearAllClicked() }) {
                Text(
                    text = stringResource(R.string.woo_pos_clear_cart_button),
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun ProductItem(
    modifier: Modifier = Modifier,
    item: WooPosCartListItem,
    canRemoveItems: Boolean,
    onRemoveClicked: (item: WooPosCartListItem) -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colors.background)
            .fillMaxWidth()
            .height(64.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val fallbackColor = Color.LightGray
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.imageUrl)
                .crossfade(true)
                .build(),
            fallback = ColorPainter(color = fallbackColor),
            error = ColorPainter(color = fallbackColor),
            placeholder = ColorPainter(color = fallbackColor),
            contentDescription = "Product Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = item.price, style = MaterialTheme.typography.body1)
        }

        if (canRemoveItems) {
            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { onRemoveClicked(item) },
                modifier = Modifier
                    .size(24.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_pos_remove_cart_item),
                    tint = MaterialTheme.colors.onBackground,
                    contentDescription = "Remove item",
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
    }
}

@Composable
@WooPosPreview
fun WooPosCartScreenPreview() {
    WooPosTheme {
        WooPosCartScreen(
            state = WooPosCartState(
                toolbar = WooPosCartToolbar(
                    icon = R.drawable.ic_shopping_cart,
                    itemsCount = "3 items",
                    isClearAllButtonVisible = true
                ),
                itemsInCart = listOf(
                    WooPosCartListItem(
                        id = WooPosCartListItem.Id(productId = 1L, itemNumber = 1),
                        imageUrl = "",
                        name = "VW California, VW California VW California, VW California VW California, " +
                            "VW California VW California, VW California,VW California",
                        price = "€50,000"
                    ),
                    WooPosCartListItem(
                        id = WooPosCartListItem.Id(productId = 2L, itemNumber = 2),
                        imageUrl = "",
                        name = "VW California",
                        price = "$150,000"
                    ),
                    WooPosCartListItem(
                        id = WooPosCartListItem.Id(productId = 3L, itemNumber = 3),
                        imageUrl = "",
                        name = "VW California",
                        price = "€250,000"
                    )
                ),
                areItemsRemovable = true,
                isOrderCreationInProgress = true,
                isCheckoutButtonVisible = true
            )
        ) {}
    }
}
