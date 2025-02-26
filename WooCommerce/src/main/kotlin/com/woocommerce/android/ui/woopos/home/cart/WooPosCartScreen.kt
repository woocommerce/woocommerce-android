package com.woocommerce.android.ui.woopos.home.cart

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
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
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.ShadowType
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosLazyColumn
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButtonSmall
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding

@Composable
fun WooPosCartScreen(modifier: Modifier = Modifier) {
    val viewModel: WooPosCartViewModel = hiltViewModel()

    viewModel.state.observeAsState().value?.let {
        WooPosCartScreen(modifier, it, viewModel::onUIEvent)
    }
}

@Composable
@Suppress("DestructuringDeclarationWithTooManyEntries")
private fun WooPosCartScreen(
    modifier: Modifier = Modifier,
    state: WooPosCartState,
    onUIEvent: (WooPosCartUIEvent) -> Unit,
) {
    ConstraintLayout(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceBright)
    ) {
        val (topMargin, toolbar, body, checkoutButton) = createRefs()

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(WooPosSpacing.XLarge.value.toAdaptivePadding())
                .constrainAs(topMargin) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )

        CartToolbar(
            modifier = Modifier.constrainAs(toolbar) {
                top.linkTo(topMargin.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
            toolbar = state.toolbar,
            onClearAllClicked = { onUIEvent(WooPosCartUIEvent.ClearAllClicked) },
            onBackClicked = { onUIEvent(WooPosCartUIEvent.BackClicked) },
        )

        when (state.body) {
            WooPosCartState.Body.Empty -> {
                CartBodyEmpty(
                    modifier = Modifier.constrainAs(body) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        height = Dimension.fillToConstraints
                    }
                )
            }

            is WooPosCartState.Body.WithItems -> {
                val productsTopMargin = WooPosSpacing.Large.value.toAdaptivePadding()
                CartBodyWithItems(
                    modifier = Modifier.constrainAs(body) {
                        top.linkTo(toolbar.bottom, margin = productsTopMargin)
                        bottom.linkTo(checkoutButton.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        height = Dimension.fillToConstraints
                    },
                    items = state.body.itemsInCart,
                    areItemsRemovable = state.areItemsRemovable,
                    isCheckoutButtonVisible = state.isCheckoutButtonVisible,
                    onUIEvent = onUIEvent
                )
            }
        }

        AnimatedVisibility(
            visible = state.isCheckoutButtonVisible,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(WooPosSpacing.Medium.value.toAdaptivePadding())
                .constrainAs(checkoutButton) {
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        ) {
            WooPosButton(
                text = stringResource(R.string.woopos_checkout_button),
                onClick = { onUIEvent(WooPosCartUIEvent.CheckoutClicked) }
            )
        }
    }
}

@Composable
fun CartBodyEmpty(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(WooPosSpacing.Medium.value.toAdaptivePadding()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            imageVector = ImageVector.vectorResource(R.drawable.ic_woo_pos_empty_cart),
            contentDescription = stringResource(R.string.woopos_cart_empty_content_description),
            modifier = Modifier.size(88.dp),
            colorFilter = ColorFilter.tint(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4F),
                blendMode = BlendMode.SrcAtop,
            )
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value.toAdaptivePadding()))
        WooPosText(
            text = stringResource(R.string.woopos_cart_empty_subtitle),
            style = WooPosTypography.BodyLarge,
            color = WooPosTheme.colors.onSurfaceVariantLowest,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CartBodyWithItems(
    modifier: Modifier = Modifier,
    items: List<WooPosCartState.Body.WithItems.Item>,
    areItemsRemovable: Boolean,
    isCheckoutButtonVisible: Boolean,
    onUIEvent: (WooPosCartUIEvent) -> Unit,
) {
    val listState = rememberLazyListState()
    ScrollToTopHandler(items, listState)

    val spacerHeight by animateDpAsState(
        targetValue = if (!isCheckoutButtonVisible) 182.dp else 0.dp,
        label = "cart list height animation"
    )

    WooPosLazyColumn(
        modifier = modifier
            .padding(horizontal = WooPosSpacing.Medium.value.toAdaptivePadding()),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value.toAdaptivePadding()),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(
            top = WooPosSpacing.XSmall.value.toAdaptivePadding(),
            bottom = WooPosSpacing.Small.value.toAdaptivePadding()
        ),
        withBottomShadow = true,
    ) {
        items(
            items,
            key = { item -> item.id.itemNumber }
        ) { item ->
            ProductItem(
                modifier = Modifier.animateItem(),
                item = item,
                canRemoveItems = areItemsRemovable,
                onUIEvent = onUIEvent,
            )
        }
        item {
            Spacer(modifier = Modifier.height(spacerHeight))
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
    modifier: Modifier = Modifier,
    toolbar: WooPosCartState.Toolbar,
    onClearAllClicked: () -> Unit,
    onBackClicked: () -> Unit
) {
    val iconSize = 28.dp
    val iconTitlePadding = WooPosSpacing.Medium.value.toAdaptivePadding()
    val titleOffset by animateDpAsState(
        targetValue = if (toolbar.backIconVisible) iconSize + iconTitlePadding else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "titleOffset"
    )

    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
    ) {
        val (backButton, title, spacer, itemsCount, clearAllButton) = createRefs()

        AnimatedVisibility(
            visible = toolbar.backIconVisible,
            enter = fadeIn(animationSpec = tween(300)) + expandHorizontally(),
            exit = fadeOut(animationSpec = tween(300)) + shrinkHorizontally()
        ) {
            IconButton(
                onClick = { onBackClicked() },
                modifier = Modifier
                    .constrainAs(backButton) {
                        start.linkTo(parent.start)
                        centerVerticallyTo(parent)
                    }
                    .padding(start = WooPosSpacing.Small.value.toAdaptivePadding())
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_back_24dp),
                    contentDescription = stringResource(R.string.woopos_cart_back_content_description),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        WooPosText(
            text = stringResource(R.string.woopos_cart_title),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier
                .constrainAs(title) {
                    start.linkTo(parent.start, margin = titleOffset)
                    centerVerticallyTo(parent)
                }
                .padding(
                    start = WooPosSpacing.Medium.value.toAdaptivePadding(),
                    end = WooPosSpacing.XSmall.value,
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
            val itemsEndMargin = WooPosSpacing.Medium.value.toAdaptivePadding()
            WooPosText(
                text = it,
                style = WooPosTypography.BodySmall,
                color = WooPosTheme.colors.onSurfaceVariantLowest,
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
            WooPosOutlinedButtonSmall(
                onClick = { onClearAllClicked() },
                modifier = Modifier
                    .constrainAs(clearAllButton) {
                        end.linkTo(parent.end)
                        centerVerticallyTo(parent)
                    }
                    .padding(end = WooPosSpacing.Medium.value.toAdaptivePadding()),
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
    val itemContentDescription = stringResource(
        id = R.string.woopos_cart_item_content_description,
        item.name,
        item.price
    )

    WooPosCard(
        modifier = modifier
            .height(96.dp)
            .semantics { contentDescription = itemContentDescription },
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        elevation = WooPosElevation.Medium,
        shadowType = ShadowType.Soft,
        shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceDim),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_box),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(WooPosTheme.colors.onSurfaceVariantLowest),
                    modifier = Modifier.size(38.dp, 32.dp)
                )
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(96.dp)
                )
            }

            Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value.toAdaptivePadding()))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                WooPosText(
                    text = item.name,
                    maxLines = 1,
                    style = WooPosTypography.BodyLarge,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clearAndSetSemantics { }
                )
                Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value.toAdaptivePadding()))
                if (item.description.isNotNullOrEmpty()) {
                    WooPosText(
                        text = item.description,
                        style = WooPosTypography.BodySmall,
                        color = WooPosTheme.colors.onSurfaceVariantHighest,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clearAndSetSemantics { }
                    )
                    Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value.toAdaptivePadding()))
                }
                WooPosText(
                    text = item.price,
                    style = WooPosTypography.BodySmall,
                    color = WooPosTheme.colors.onSurfaceVariantHighest,
                    modifier = Modifier.clearAndSetSemantics { }
                )
            }

            if (canRemoveItems) {
                Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value.toAdaptivePadding()))

                val removeButtonContentDescription = stringResource(
                    id = R.string.woopos_remove_item_button_from_cart_content_description,
                    item.name
                )
                IconButton(
                    onClick = { onUIEvent(WooPosCartUIEvent.ItemRemovedFromCart(item)) },
                    modifier = Modifier
                        .size(32.dp)
                        .semantics { contentDescription = removeButtonContentDescription }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_pos_remove_cart_item),
                        tint = WooPosTheme.colors.onSurfaceVariantLowest,
                        contentDescription = null,
                    )
                }
            }
            Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value.toAdaptivePadding()))
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
                    backIconVisible = false,
                    itemsCount = "3 items",
                    isClearAllButtonVisible = true
                ),
                body = WooPosCartState.Body.WithItems(
                    itemsInCart = listOf(
                        WooPosCartState.Body.WithItems.Item(
                            id = WooPosCartState.Body.WithItems.Item.Id(
                                productId = 1L,
                                variationId = 0L,
                                itemNumber = 1
                            ),
                            imageUrl = "",
                            name = "VW California, VW California VW California, VW California VW California, " +
                                "VW California VW California, VW California,VW California",
                            description = "test description",
                            price = "€50,000",
                            productType = ProductType.Simple,
                        ),
                        WooPosCartState.Body.WithItems.Item(
                            id = WooPosCartState.Body.WithItems.Item.Id(
                                productId = 2L,
                                variationId = 0L,
                                itemNumber = 2
                            ),
                            imageUrl = "",
                            name = "VW California",
                            description = "test description test description test description test description" +
                                " test description test description test description test description test description",
                            price = "$150,000",
                            productType = ProductType.Simple,
                        ),
                        WooPosCartState.Body.WithItems.Item(
                            id = WooPosCartState.Body.WithItems.Item.Id(
                                productId = 3L,
                                variationId = 0L,
                                itemNumber = 3
                            ),
                            imageUrl = "",
                            name = "VW California",
                            description = "",
                            price = "€250,000",
                            productType = ProductType.Simple,
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
                    backIconVisible = true,
                    itemsCount = "3 items",
                    isClearAllButtonVisible = true
                ),
                body = WooPosCartState.Body.WithItems(
                    itemsInCart = listOf(
                        WooPosCartState.Body.WithItems.Item(
                            id = WooPosCartState.Body.WithItems.Item.Id(
                                productId = 1L,
                                variationId = 0L,
                                itemNumber = 1
                            ),
                            imageUrl = "",
                            name = "VW California",
                            description = null,
                            price = "€50,000",
                            productType = ProductType.Simple,
                        ),
                        WooPosCartState.Body.WithItems.Item(
                            id = WooPosCartState.Body.WithItems.Item.Id(
                                productId = 2L,
                                variationId = 0L,
                                itemNumber = 2
                            ),
                            imageUrl = "",
                            name = "VW California",
                            description = null,
                            price = "$150,000",
                            productType = ProductType.Simple,
                        ),
                        WooPosCartState.Body.WithItems.Item(
                            id = WooPosCartState.Body.WithItems.Item.Id(
                                productId = 3L,
                                variationId = 0L,
                                itemNumber = 3
                            ),
                            imageUrl = "",
                            name = "VW California",
                            description = null,
                            price = "€250,000",
                            productType = ProductType.Simple,
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
                    backIconVisible = false,
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
