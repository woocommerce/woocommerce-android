@file:OptIn(ExperimentalFoundationApi::class)

package com.woocommerce.android.ui.woopos.home.cart

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.toAdaptivePadding

@Composable
fun WooPosCartScreen(modifier: Modifier = Modifier) {
    val viewModel: WooPosCartViewModel = hiltViewModel()

    viewModel.state.observeAsState().value?.let {
        WooPosCartScreen(modifier, it, viewModel::onUIEvent)
    }
}

@Composable
private fun WooPosCartScreen(
    modifier: Modifier = Modifier,
    state: WooPosCartState,
    onUIEvent: (WooPosCartUIEvent) -> Unit
) {
    Box(
        modifier = modifier
            .padding(24.dp.toAdaptivePadding())
            .background(MaterialTheme.colors.surface)
    ) {
        Column {
            CartToolbar(
                toolbar = state.toolbar,
                onClearAllClicked = { onUIEvent(WooPosCartUIEvent.ClearAllClicked) },
                onBackClicked = { onUIEvent(WooPosCartUIEvent.BackClicked) }
            )

            Spacer(modifier = Modifier.height(20.dp.toAdaptivePadding()))

            val listState = rememberLazyListState()
            ScrollToBottomHandler(state, listState)

            LazyColumn(
                modifier = Modifier
                    .weight(1f),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp.toAdaptivePadding()),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(top = 4.dp.toAdaptivePadding()),
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
                text = stringResource(R.string.woopos_checkout_button),
                onClick = { onUIEvent(WooPosCartUIEvent.CheckoutClicked) }
            )
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
@Suppress("DestructuringDeclarationWithTooManyEntries")
private fun CartToolbar(
    toolbar: WooPosCartToolbar,
    onClearAllClicked: () -> Unit,
    onBackClicked: () -> Unit
) {
    ConstraintLayout(modifier = Modifier.fillMaxWidth()) {
        val (backButton, title, spacer, itemsCount, clearAllButton) = createRefs()

        toolbar.icon?.let {
            IconButton(
                onClick = { onBackClicked() },
                modifier = Modifier.constrainAs(backButton) {
                    start.linkTo(parent.start)
                    centerVerticallyTo(parent)
                }
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(toolbar.icon),
                    contentDescription = stringResource(R.string.woopos_cart_back_content_description),
                    tint = MaterialTheme.colors.onBackground,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        val cartTitleEndMargin = 16.dp.toAdaptivePadding()
        Text(
            text = stringResource(R.string.woopos_cart_title),
            style = MaterialTheme.typography.h4,
            color = MaterialTheme.colors.onBackground,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.constrainAs(title) {
                start.linkTo(backButton.end, margin = cartTitleEndMargin)
                centerVerticallyTo(parent)
            }
        )

        Spacer(
            modifier = Modifier
                .constrainAs(spacer) {
                    start.linkTo(title.end)
                    end.linkTo(itemsCount.start)
                    width = Dimension.fillToConstraints
                }
        )

        val itemsEndMargin = 16.dp.toAdaptivePadding()
        Text(
            text = toolbar.itemsCount,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.secondaryVariant.copy(alpha = 0.6f),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier
                .constrainAs(itemsCount) {
                    end.linkTo(
                        if (toolbar.isClearAllButtonVisible) {
                            clearAllButton.start
                        } else {
                            parent.end
                        },
                        margin = itemsEndMargin,
                    )
                    centerVerticallyTo(parent)
                }
        )

        if (toolbar.isClearAllButtonVisible) {
            WooPosOutlinedButton(
                onClick = { onClearAllClicked() },
                modifier = Modifier.constrainAs(clearAllButton) {
                    end.linkTo(parent.end)
                    centerVerticallyTo(parent)
                },
                text = stringResource(R.string.woopos_clear_cart_button)
            )
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
    Card(
        modifier = modifier
            .height(64.dp),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
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
                contentDescription = stringResource(R.string.woopos_product_image_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.width(16.dp.toAdaptivePadding()))

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
                Spacer(modifier = Modifier.height(4.dp.toAdaptivePadding()))
                Text(text = item.price, style = MaterialTheme.typography.body1)
            }

            if (canRemoveItems) {
                Spacer(modifier = Modifier.width(8.dp.toAdaptivePadding()))

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
            Spacer(modifier = Modifier.width(16.dp.toAdaptivePadding()))
        }
    }
}

@Composable
@WooPosPreview
fun WooPosCartScreenProductsPreview(modifier: Modifier = Modifier) {
    WooPosTheme {
        WooPosCartScreen(
            modifier = modifier,
            state = WooPosCartState(
                toolbar = WooPosCartToolbar(
                    icon = null,
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
                isCheckoutButtonVisible = true
            )
        ) {}
    }
}

@Composable
@WooPosPreview
fun WooPosCartScreenCheckoutPreview(modifier: Modifier = Modifier) {
    WooPosTheme {
        WooPosCartScreen(
            modifier = modifier,
            state = WooPosCartState(
                toolbar = WooPosCartToolbar(
                    icon = R.drawable.ic_back_24dp,
                    itemsCount = "3 items",
                    isClearAllButtonVisible = true
                ),
                itemsInCart = listOf(
                    WooPosCartListItem(
                        id = WooPosCartListItem.Id(productId = 1L, itemNumber = 1),
                        imageUrl = "",
                        name = "VW California",
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
                areItemsRemovable = false,
                isCheckoutButtonVisible = true
            )
        ) {}
    }
}
