package com.woocommerce.android.ui.woopos.home.phone

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosBackButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosBackgroundOverlay
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCircularIconButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInput
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchUIEvent
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartItemViewState
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartState
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartUIEvent
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartViewModel
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbarViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbarViewState.SearchState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsUIEvent
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsScreen
import com.woocommerce.android.ui.woopos.home.items.coupons.search.WooPosCouponsSearchScreen
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsScreen
import com.woocommerce.android.ui.woopos.home.items.search.WooPosItemsSearchScreen
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsScreen
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosHomeFloatingToolbarState.Menu
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosHomeFloatingToolbarUIEvent
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosHomeFloatingToolbarViewModel

@Composable
fun WooPosPhoneProductsScreen(
    cartItemCount: Int,
    onCartClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemsViewModel: WooPosItemsViewModel = hiltViewModel()
    val toolbarViewModel: WooPosHomeFloatingToolbarViewModel = hiltViewModel()
    val cartViewModel: WooPosCartViewModel = hiltViewModel()
    val itemsState by itemsViewModel.viewState.collectAsState()
    val toolbarState by toolbarViewModel.state.collectAsState()
    val cartState by cartViewModel.state.observeAsState()
    val menu = toolbarState.menu

    val productsListState = rememberLazyListState()
    val couponsListState = rememberLazyListState()

    val cartOverlay = remember(cartState) {
        buildPhoneCartOverlay(
            cartState = cartState,
            cartViewModel = cartViewModel,
        )
    }

    CompositionLocalProvider(LocalPhoneCartOverlay provides cartOverlay) {
        Box(modifier = modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                PhoneItemsToolbar(
                    state = itemsState,
                    onMenuClicked = {
                        toolbarViewModel.onUiEvent(
                            WooPosHomeFloatingToolbarUIEvent.OnToolbarMenuClicked
                        )
                    },
                    onTabClicked = { itemsViewModel.onUIEvent(WooPosItemsUIEvent.OnTabClicked(it)) },
                    onSearchEvent = { event ->
                        when (event) {
                            WooPosSearchUIEvent.Clear ->
                                itemsViewModel.onUIEvent(WooPosItemsUIEvent.ClearSearchClicked)
                            WooPosSearchUIEvent.Close ->
                                itemsViewModel.onUIEvent(WooPosItemsUIEvent.CloseSearchClicked)
                            is WooPosSearchUIEvent.Search ->
                                itemsViewModel.onUIEvent(
                                    WooPosItemsUIEvent.SearchChanged(
                                        query = event.query,
                                        cursorPosition = event.cursorPosition,
                                    )
                                )
                            WooPosSearchUIEvent.SearchIconClicked ->
                                itemsViewModel.onUIEvent(WooPosItemsUIEvent.SearchIconClicked)
                        }
                    },
                    onBackClicked = {
                        itemsViewModel.onUIEvent(WooPosItemsUIEvent.BackFromVariationsClicked)
                    },
                    onAddCouponEvent = {
                        itemsViewModel.onUIEvent(WooPosItemsUIEvent.AddCouponIconClicked)
                    },
                )

                Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

                PhoneItemsContent(
                    state = itemsState,
                    productsListState = productsListState,
                    couponsListState = couponsListState,
                    onBackFromVariations = {
                        itemsViewModel.onUIEvent(WooPosItemsUIEvent.BackFromVariationsClicked)
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            // Menu overlay and popup
            WooPosBackgroundOverlay(
                modifier = Modifier.fillMaxSize(),
                isVisible = menu is Menu.Visible,
                onClick = {
                    toolbarViewModel.onUiEvent(
                        WooPosHomeFloatingToolbarUIEvent.OnOutsideOfToolbarMenuClicked
                    )
                }
            )

            if (menu is Menu.Visible) {
                PhonePopUpMenu(
                    menuItems = menu.items,
                    onClick = { menuItem ->
                        toolbarViewModel.onUiEvent(
                            WooPosHomeFloatingToolbarUIEvent.MenuItemClicked(menuItem)
                        )
                    },
                    modifier = Modifier
                        .statusBarsPadding()
                        .align(Alignment.TopStart)
                        .padding(
                            start = WooPosSpacing.Small.value,
                            top = WooPosSpacing.Gigantic.value
                        )
                )
            }

            // Floating cart button
            AnimatedVisibility(
                visible = cartItemCount > 0,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = fadeOut(animationSpec = tween(100)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceBright,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PhoneCartButton(
                        itemCount = cartItemCount,
                        onClick = onCartClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(WooPosSpacing.Medium.value)
                            .navigationBarsPadding()
                    )
                }
            }
        }
    }
}

private fun buildPhoneCartOverlay(
    cartState: WooPosCartState?,
    cartViewModel: WooPosCartViewModel,
): PhoneCartOverlayData {
    val body = cartState?.body
    val cartItems = (body as? WooPosCartState.Body.WithItems)?.itemsInCart ?: emptyList()

    val quantities = buildCartQuantities(cartItems)

    return PhoneCartOverlayData(
        quantities = quantities,
        onIncrement = { itemId ->
            val cartItem = cartItems
                .filterIsInstance<WooPosCartItemViewState.Product>()
                .firstOrNull { cartItemMatchesProductId(it, itemId) }
            if (cartItem != null) {
                val variationId = (cartItem as? WooPosCartItemViewState.Product.Variation)?.variationId
                cartViewModel.onUIEvent(
                    WooPosCartUIEvent.ItemIncrementedFromProductCard(
                        productId = cartItem.id,
                        variationId = variationId,
                    )
                )
            }
        },
        onDecrement = { itemId ->
            val lastCartItem = cartItems
                .filterIsInstance<WooPosCartItemViewState.Product>()
                .lastOrNull { cartItemMatchesProductId(it, itemId) }
            if (lastCartItem != null) {
                cartViewModel.onUIEvent(WooPosCartUIEvent.ItemRemovedFromCart(lastCartItem))
            }
        },
    )
}

private fun buildCartQuantities(
    cartItems: List<WooPosCartItemViewState>
): Map<Long, Int> {
    val quantities = mutableMapOf<Long, Int>()
    for (item in cartItems) {
        when (item) {
            is WooPosCartItemViewState.Product.Simple -> {
                quantities[item.id] = (quantities[item.id] ?: 0) + 1
            }
            is WooPosCartItemViewState.Product.Variation -> {
                quantities[item.variationId] = (quantities[item.variationId] ?: 0) + 1
            }
            else -> Unit
        }
    }
    return quantities
}

private fun cartItemMatchesProductId(
    cartItem: WooPosCartItemViewState.Product,
    productId: Long
): Boolean = when (cartItem) {
    is WooPosCartItemViewState.Product.Simple -> cartItem.id == productId
    is WooPosCartItemViewState.Product.Variation -> cartItem.variationId == productId
}

@Composable
private fun PhoneItemsToolbar(
    state: WooPosItemsToolbarViewState,
    onMenuClicked: () -> Unit,
    onTabClicked: (WooPosItemsToolbarViewState.Tab) -> Unit,
    onSearchEvent: (WooPosSearchUIEvent) -> Unit,
    onBackClicked: () -> Unit,
    onAddCouponEvent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSearchOpen = (state.search as? SearchState.Visible)?.let {
        it.state is WooPosSearchInputState.Open
    } == true

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        if (isSearchOpen) {
            WooPosSearchInput(
                state = (state.search as SearchState.Visible).state,
                onEvent = onSearchEvent,
                modifier = Modifier.padding(end = WooPosSpacing.Medium.value),
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.backNavigation) {
                    WooPosBackButton { onBackClicked() }
                } else {
                    // Menu button
                    IconButton(
                        onClick = onMenuClicked,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(
                                R.drawable.ic_menu_more_vert
                            ),
                            contentDescription = stringResource(
                                R.string.woopos_menu_toolbar_content_description
                            ),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                // Tabs
                state.tabs.forEach { tab ->
                    WooPosText(
                        text = tab.name,
                        style = WooPosTypography.Heading,
                        fontWeight = FontWeight.Bold,
                        color = when (tab.highlightLevel) {
                            WooPosItemsToolbarViewState.Tab.HighlightLevel.Full ->
                                MaterialTheme.colorScheme.onSurface
                            WooPosItemsToolbarViewState.Tab.HighlightLevel.Normal ->
                                WooPosTheme.colors.onSurfaceVariantLowest
                        },
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabClicked(tab) }
                        )
                    )
                    Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))
                }

                Spacer(modifier = Modifier.weight(1f))

                if (state is WooPosItemsToolbarViewState.CouponList) {
                    WooPosCircularIconButton(
                        icon = ImageVector.vectorResource(R.drawable.ic_add),
                        contentDescription = stringResource(
                            id = R.string.woopos_coupons_empty_list_create_coupon_label,
                        ),
                        onClick = { onAddCouponEvent() }
                    )
                    Spacer(modifier = Modifier.width(WooPosSpacing.Small.value))
                }

                // Search icon
                when (val search = state.search) {
                    SearchState.Hidden -> Unit
                    is SearchState.Visible -> {
                        WooPosSearchInput(
                            state = search.state,
                            onEvent = onSearchEvent,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))
            }
        }
    }
}

@Composable
private fun PhoneItemsContent(
    state: WooPosItemsToolbarViewState,
    productsListState: androidx.compose.foundation.lazy.LazyListState,
    couponsListState: androidx.compose.foundation.lazy.LazyListState,
    onBackFromVariations: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Crossfade(
        targetState = getPhoneScreenState(state),
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        modifier = modifier,
    ) { screenState ->
        when (screenState) {
            PhoneScreenState.Products -> WooPosProductsScreen(
                modifier = Modifier.padding(horizontal = WooPosSpacing.Medium.value),
                listState = productsListState,
            )
            PhoneScreenState.Coupons -> WooPosCouponsScreen(
                modifier = Modifier.padding(horizontal = WooPosSpacing.Medium.value),
                listState = couponsListState,
            )
            PhoneScreenState.ProductsSearch -> WooPosItemsSearchScreen()
            PhoneScreenState.CouponsSearch -> WooPosCouponsSearchScreen()
            is PhoneScreenState.Variations -> WooPosVariationsScreen(
                modifier = Modifier.padding(horizontal = WooPosSpacing.Medium.value),
                variableProductData = screenState.variableProductData,
                onBackClicked = onBackFromVariations,
            )
        }
    }
}

private sealed class PhoneScreenState {
    data object Products : PhoneScreenState()
    data object Coupons : PhoneScreenState()
    data object ProductsSearch : PhoneScreenState()
    data object CouponsSearch : PhoneScreenState()
    data class Variations(
        val variableProductData:
        com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsNavigationData
    ) : PhoneScreenState()
}

private fun getPhoneScreenState(state: WooPosItemsToolbarViewState): PhoneScreenState {
    return when (state) {
        is WooPosItemsToolbarViewState.ProductList -> {
            when (val searchState = state.search) {
                SearchState.Hidden -> PhoneScreenState.Products
                is SearchState.Visible -> {
                    when (searchState.state) {
                        WooPosSearchInputState.Closed -> PhoneScreenState.Products
                        is WooPosSearchInputState.Open -> PhoneScreenState.ProductsSearch
                    }
                }
            }
        }
        is WooPosItemsToolbarViewState.VariationList -> PhoneScreenState.Variations(
            variableProductData = state.variableProductData
        )
        is WooPosItemsToolbarViewState.CouponList -> {
            when (val searchState = state.search) {
                SearchState.Hidden -> PhoneScreenState.Coupons
                is SearchState.Visible -> {
                    when (searchState.state) {
                        WooPosSearchInputState.Closed -> PhoneScreenState.Coupons
                        is WooPosSearchInputState.Open -> PhoneScreenState.CouponsSearch
                    }
                }
            }
        }
    }
}

@Composable
private fun PhonePopUpMenu(
    menuItems: List<Menu.MenuItem>,
    onClick: (Menu.MenuItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    WooPosCard(
        modifier = modifier
            .widthIn(max = 280.dp)
            .width(IntrinsicSize.Max),
        elevation = WooPosElevation.Medium,
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
            menuItems.forEach { menuItem ->
                PhonePopUpMenuItem(menuItem, onClick)
            }
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
        }
    }
}

@Composable
private fun PhonePopUpMenuItem(
    menuItem: Menu.MenuItem,
    onClick: (Menu.MenuItem) -> Unit,
) {
    TextButton(onClick = { onClick(menuItem) }) {
        Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))
        Icon(
            imageVector = ImageVector.vectorResource(menuItem.icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))
        WooPosText(
            modifier = Modifier
                .padding(vertical = WooPosSpacing.Small.value)
                .weight(1f),
            text = stringResource(id = menuItem.title),
            style = WooPosTypography.BodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))
    }
}

@Composable
private fun PhoneCartButton(
    itemCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WooPosButton(
        modifier = modifier.height(56.dp),
        onClick = onClick,
        text = stringResource(R.string.woopos_cart_title) + " ($itemCount)"
    )
}
