@file:OptIn(ExperimentalFoundationApi::class)

package com.woocommerce.android.ui.woopos.home.cart

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosLazyColumn
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
    onUIEvent: (WooPosCartUIEvent) -> Unit,
) {
    Box(
        modifier = modifier
            .padding(
                top = 40.dp.toAdaptivePadding(),
                bottom = 16.dp.toAdaptivePadding()
            )
            .fillMaxSize()
            .background(MaterialTheme.colors.surface)
    ) {
        Column {
            CartToolbar(
                toolbar = state.toolbar,
                onClearAllClicked = { onUIEvent(WooPosCartUIEvent.ClearAllClicked) },
                onBackClicked = { onUIEvent(WooPosCartUIEvent.BackClicked) }
            )

            when (state.body) {
                WooPosCartState.Body.Empty -> {
                    CartBodyEmpty()
                }

                is WooPosCartState.Body.WithItems -> {
                    CartBodyWithItems(
                        items = state.body.itemsInCart,
                        areItemsRemovable = state.areItemsRemovable,
                        onUIEvent = onUIEvent,
                    )
                }
            }
        }

        if (state.isCheckoutButtonVisible) {
            WooPosButton(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp.toAdaptivePadding()),
                text = stringResource(R.string.woopos_checkout_button),
                onClick = { onUIEvent(WooPosCartUIEvent.CheckoutClicked) }
            )
        }
    }
    val cartOverlayIntensityAnimated by animateFloatAsState(
        when (state.body) {
            WooPosCartState.Body.Empty -> .6f
            is WooPosCartState.Body.WithItems -> 0f
        },
        label = "cartOverlayAnimated"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = MaterialTheme.colors.background.copy(alpha = cartOverlayIntensityAnimated),
            )
    )
}

@Composable
fun CartBodyEmpty() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp.toAdaptivePadding()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            imageVector = ImageVector.vectorResource(R.drawable.woo_pos_ic_empty_cart),
            contentDescription = stringResource(R.string.woopos_cart_empty_content_description),
        )
        Spacer(modifier = Modifier.height(40.dp.toAdaptivePadding()))
        Text(
            text = stringResource(R.string.woopos_cart_empty_subtitle),
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.secondaryVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CartBodyWithItems(
    items: List<WooPosCartState.Body.WithItems.Item>,
    areItemsRemovable: Boolean,
    onUIEvent: (WooPosCartUIEvent) -> Unit,
) {
    Spacer(modifier = Modifier.height(20.dp.toAdaptivePadding()))

    val listState = rememberLazyListState()
    ScrollToTopHandler(items, listState)

    WooPosLazyColumn(
        modifier = Modifier
            .padding(horizontal = 16.dp.toAdaptivePadding()),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp.toAdaptivePadding()),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(2.dp),
    ) {
        items(
            items,
            key = { item -> item.id.itemNumber }
        ) { item ->
            ProductItem(
                modifier = Modifier.animateItemPlacement(),
                item = item,
                canRemoveItems = areItemsRemovable,
                onUIEvent = onUIEvent,
            )
        }
        item {
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun ScrollToTopHandler(
    items: List<WooPosCartState.Body.WithItems.Item>,
    listState: LazyListState
) {
    val previousItemsCount = remember { mutableIntStateOf(0) }
    val itemsInCartSize = items.size
    LaunchedEffect(itemsInCartSize) {
        if (itemsInCartSize > previousItemsCount.intValue) {
            listState.animateScrollToItem(0)
        }
        previousItemsCount.intValue = itemsInCartSize
    }
}

@Composable
@Suppress("DestructuringDeclarationWithTooManyEntries")
private fun CartToolbar(
    toolbar: WooPosCartState.Toolbar,
    onClearAllClicked: () -> Unit,
    onBackClicked: () -> Unit
) {
    val iconSize = 28.dp
    val iconTitlePadding = 16.dp.toAdaptivePadding()
    val titleOffset by animateDpAsState(
        targetValue = if (toolbar.icon != null) iconSize + iconTitlePadding else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "titleOffset"
    )

    ConstraintLayout(modifier = Modifier.fillMaxWidth()) {
        val (backButton, title, spacer, itemsCount, clearAllButton) = createRefs()

        toolbar.icon?.let {
            IconButton(
                onClick = { onBackClicked() },
                modifier = Modifier
                    .constrainAs(backButton) {
                        start.linkTo(parent.start)
                        centerVerticallyTo(parent)
                    }
                    .padding(start = 8.dp.toAdaptivePadding())
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(it),
                    contentDescription = stringResource(R.string.woopos_cart_back_content_description),
                    tint = MaterialTheme.colors.onBackground,
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        Text(
            text = stringResource(R.string.woopos_cart_title),
            style = MaterialTheme.typography.h4,
            color = MaterialTheme.colors.onBackground,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier
                .constrainAs(title) {
                    start.linkTo(parent.start, margin = titleOffset)
                    centerVerticallyTo(parent)
                }
                .padding(
                    start = 16.dp.toAdaptivePadding(),
                    end = 4.dp,
                    top = 4.dp,
                    bottom = 4.dp
                )
        )

        Spacer(
            modifier = Modifier
                .constrainAs(spacer) {
                    start.linkTo(title.end)
                    end.linkTo(itemsCount.start)
                    width = Dimension.fillToConstraints
                }
        )

        toolbar.itemsCount?.let {
            val itemsEndMargin = 16.dp.toAdaptivePadding()
            Text(
                text = it,
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
        }

        if (toolbar.isClearAllButtonVisible) {
            WooPosOutlinedButton(
                onClick = { onClearAllClicked() },
                modifier = Modifier
                    .constrainAs(clearAllButton) {
                        end.linkTo(parent.end)
                        centerVerticallyTo(parent)
                    }
                    .padding(end = 16.dp.toAdaptivePadding()),
                text = stringResource(R.string.woopos_clear_cart_button)
            )
        }
    }
}

@Composable
private fun ProductItem(
    modifier: Modifier = Modifier,
    item: WooPosCartState.Body.WithItems.Item,
    canRemoveItems: Boolean,
    onUIEvent: (WooPosCartUIEvent) -> Unit,
) {
    var hasAnimationStarted by remember { mutableStateOf(item.isAppearanceAnimationPlayed) }
    LaunchedEffect(Unit) {
        hasAnimationStarted = true
    }

    val elevation by animateDpAsState(
        targetValue = if (hasAnimationStarted) 4.dp else 0.dp,
        animationSpec = tween(durationMillis = 150, delayMillis = 100),
        label = "elevation"
    )

    val itemHeight = 64.dp
    val offsetY by animateDpAsState(
        targetValue = if (hasAnimationStarted) 0.dp else -itemHeight,
        animationSpec = tween(durationMillis = 70),
        label = "offsetY"
    )

    val alpha by animateFloatAsState(
        targetValue = if (hasAnimationStarted) 1f else 0f,
        animationSpec = tween(
            durationMillis = 200,
            easing = LinearEasing
        ),
        label = "alpha"
    )

    val itemContentDescription = stringResource(
        id = R.string.woopos_cart_item_content_description,
        item.name,
        item.price
    )

    LaunchedEffect(alpha) {
        if (alpha == 1f) {
            onUIEvent(WooPosCartUIEvent.OnCartItemAppearanceAnimationPlayed(item))
        }
    }

    Card(
        modifier = modifier
            .offset(y = offsetY)
            .height(itemHeight)
            .semantics { contentDescription = itemContentDescription }
            .graphicsLayer(alpha = alpha),
        elevation = elevation,
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
                contentDescription = null,
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
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clearAndSetSemantics { }
                )
                Spacer(modifier = Modifier.height(4.dp.toAdaptivePadding()))
                Text(
                    text = item.price,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier.clearAndSetSemantics { }
                )
            }

            if (canRemoveItems) {
                Spacer(modifier = Modifier.width(8.dp.toAdaptivePadding()))

                val removeButtonContentDescription = stringResource(
                    id = R.string.woopos_remove_item_button_from_cart_content_description,
                    item.name
                )
                IconButton(
                    onClick = { onUIEvent(WooPosCartUIEvent.ItemRemovedFromCart(item)) },
                    modifier = Modifier
                        .size(24.dp)
                        .semantics { contentDescription = removeButtonContentDescription }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_pos_remove_cart_item),
                        tint = MaterialTheme.colors.onBackground,
                        contentDescription = null,
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
                toolbar = WooPosCartState.Toolbar(
                    icon = null,
                    itemsCount = "3 items",
                    isClearAllButtonVisible = true
                ),
                body = WooPosCartState.Body.WithItems(
                    itemsInCart = listOf(
                        WooPosCartState.Body.WithItems.Item(
                            id = WooPosCartState.Body.WithItems.Item.Id(productId = 1L, itemNumber = 1),
                            imageUrl = "",
                            name = "VW California, VW California VW California, VW California VW California, " +
                                "VW California VW California, VW California,VW California",
                            price = "€50,000",
                            isAppearanceAnimationPlayed = true
                        ),
                        WooPosCartState.Body.WithItems.Item(
                            id = WooPosCartState.Body.WithItems.Item.Id(productId = 2L, itemNumber = 2),
                            imageUrl = "",
                            name = "VW California",
                            price = "$150,000",
                            isAppearanceAnimationPlayed = true
                        ),
                        WooPosCartState.Body.WithItems.Item(
                            id = WooPosCartState.Body.WithItems.Item.Id(productId = 3L, itemNumber = 3),
                            imageUrl = "",
                            name = "VW California",
                            price = "€250,000",
                            isAppearanceAnimationPlayed = true
                        )
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
                toolbar = WooPosCartState.Toolbar(
                    icon = R.drawable.ic_back_24dp,
                    itemsCount = "3 items",
                    isClearAllButtonVisible = true
                ),
                body = WooPosCartState.Body.WithItems(
                    itemsInCart = listOf(
                        WooPosCartState.Body.WithItems.Item(
                            id = WooPosCartState.Body.WithItems.Item.Id(productId = 1L, itemNumber = 1),
                            imageUrl = "",
                            name = "VW California",
                            price = "€50,000",
                            isAppearanceAnimationPlayed = true
                        ),
                        WooPosCartState.Body.WithItems.Item(
                            id = WooPosCartState.Body.WithItems.Item.Id(productId = 2L, itemNumber = 2),
                            imageUrl = "",
                            name = "VW California",
                            price = "$150,000",
                            isAppearanceAnimationPlayed = true
                        ),
                        WooPosCartState.Body.WithItems.Item(
                            id = WooPosCartState.Body.WithItems.Item.Id(productId = 3L, itemNumber = 3),
                            imageUrl = "",
                            name = "VW California",
                            price = "€250,000",
                            isAppearanceAnimationPlayed = true
                        )
                    )
                ),
                areItemsRemovable = false,
                isCheckoutButtonVisible = true
            )
        ) {}
    }
}

@Composable
@WooPosPreview
fun WooPosCartScreenEmptyPreview(modifier: Modifier = Modifier) {
    WooPosTheme {
        WooPosCartScreen(
            modifier = modifier,
            state = WooPosCartState(
                toolbar = WooPosCartState.Toolbar(
                    icon = null,
                    itemsCount = null,
                    isClearAllButtonVisible = false
                ),
                body = WooPosCartState.Body.Empty,
                areItemsRemovable = false,
                isCheckoutButtonVisible = false
            )
        ) {}
    }
}
