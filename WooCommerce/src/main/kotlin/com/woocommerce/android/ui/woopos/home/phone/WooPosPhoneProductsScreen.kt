package com.woocommerce.android.ui.woopos.home.phone

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosBackgroundOverlay
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.common.composeui.component.toItemsUIEvent
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.home.items.WOO_POS_ITEMS_TOOLBAR_HEIGHT
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsContent
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbar
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbarViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbarViewState.SearchState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsUIEvent
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsScreen
import com.woocommerce.android.ui.woopos.home.items.coupons.search.WooPosCouponsSearchScreen
import com.woocommerce.android.ui.woopos.home.items.customamount.WooPosCustomAmountFormScreen
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsScreen
import com.woocommerce.android.ui.woopos.home.items.search.WooPosItemsSearchScreen
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsScreen
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosHomeFloatingToolbarState.Menu
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosHomeFloatingToolbarUIEvent
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosHomeFloatingToolbarViewModel
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosToolbarPopUpMenu

@Composable
fun WooPosPhoneProductsScreen(
    modifier: Modifier = Modifier,
    itemsViewModel: WooPosItemsViewModel = hiltViewModel(),
    toolbarViewModel: WooPosHomeFloatingToolbarViewModel = hiltViewModel(),
) {
    val itemsState by itemsViewModel.viewState.collectAsState()
    val toolbarState by toolbarViewModel.state.collectAsState()

    WooPosPhoneProductsContent(
        modifier = modifier,
        itemsState = itemsState,
        menu = toolbarState.menu,
        onMenuClicked = {
            toolbarViewModel.onUiEvent(WooPosHomeFloatingToolbarUIEvent.OnToolbarMenuClicked)
        },
        onMenuDismissed = {
            toolbarViewModel.onUiEvent(WooPosHomeFloatingToolbarUIEvent.OnOutsideOfToolbarMenuClicked)
        },
        onMenuItemClicked = { menuItem ->
            toolbarViewModel.onUiEvent(WooPosHomeFloatingToolbarUIEvent.MenuItemClicked(menuItem))
        },
        onItemsUIEvent = { itemsViewModel.onUIEvent(it) },
    )
}

@Composable
private fun WooPosPhoneProductsContent(
    itemsState: WooPosItemsToolbarViewState,
    menu: Menu,
    onMenuClicked: () -> Unit,
    onMenuDismissed: () -> Unit,
    onMenuItemClicked: (Menu.MenuItem) -> Unit,
    onItemsUIEvent: (WooPosItemsUIEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            WooPosItemsToolbar(
                modifier = Modifier.padding(end = WooPosSpacing.Medium.value),
                state = itemsState,
                onTabClicked = { onItemsUIEvent(WooPosItemsUIEvent.OnTabClicked(it)) },
                onSearchEvent = { onItemsUIEvent(it.toItemsUIEvent()) },
                onBackClicked = { onItemsUIEvent(WooPosItemsUIEvent.BackFromVariationsClicked) },
                onAddCouponEvent = { onItemsUIEvent(WooPosItemsUIEvent.AddCouponIconClicked) },
                leadingContent = if (itemsState.backNavigation) {
                    null
                } else {
                    { PhoneMenuButton(onMenuClicked) }
                },
                showAddCouponButton = false,
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

            val productsListState = rememberLazyListState()
            val couponsListState = rememberLazyListState()
            WooPosItemsContent(
                state = itemsState,
                productsContent = {
                    WooPosProductsScreen(
                        modifier = Modifier.padding(horizontal = WooPosSpacing.Medium.value),
                        listState = productsListState,
                    )
                },
                couponsContent = {
                    WooPosCouponsScreen(
                        modifier = Modifier.padding(horizontal = WooPosSpacing.Medium.value),
                        listState = couponsListState,
                    )
                },
                productsSearchContent = { WooPosItemsSearchScreen() },
                couponsSearchContent = { WooPosCouponsSearchScreen() },
                variationsContent = { data ->
                    WooPosVariationsScreen(
                        modifier = Modifier.padding(horizontal = WooPosSpacing.Medium.value),
                        variableProductData = data,
                        onBackClicked = {
                            onItemsUIEvent(WooPosItemsUIEvent.BackFromVariationsClicked)
                        },
                    )
                },
                customAmountFormContent = { editing ->
                    WooPosCustomAmountFormScreen(
                        editing = editing,
                        onBackClick = {
                            onItemsUIEvent(WooPosItemsUIEvent.BackFromVariationsClicked)
                        },
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }

        AnimatedVisibility(
            visible = itemsState is WooPosItemsToolbarViewState.CouponList,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .imePadding()
                .padding(WooPosSpacing.Medium.value),
        ) {
            FloatingActionButton(
                onClick = { onItemsUIEvent(WooPosItemsUIEvent.AddCouponIconClicked) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_add),
                    contentDescription = stringResource(
                        id = R.string.woopos_phone_items_add_coupon_fab_accessibility_label,
                    ),
                )
            }
        }

        WooPosBackgroundOverlay(
            modifier = Modifier.fillMaxSize(),
            isVisible = menu is Menu.Visible,
            onClick = onMenuDismissed,
        )

        if (menu is Menu.Visible) {
            WooPosToolbarPopUpMenu(
                menuItems = menu.items,
                onClick = onMenuItemClicked,
                modifier = Modifier
                    .statusBarsPadding()
                    .align(Alignment.TopStart)
                    .padding(
                        start = WooPosSpacing.Small.value,
                        top = WOO_POS_ITEMS_TOOLBAR_HEIGHT,
                    )
            )
        }
    }
}

@Composable
private fun PhoneMenuButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .padding(horizontal = WooPosSpacing.Small.value)
            .size(WooPosIconSize.Large.value),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_menu_more_vert),
            contentDescription = stringResource(R.string.woopos_menu_toolbar_content_description),
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(WooPosIconSize.Small.value),
        )
    }
}

@Composable
@WooPosPreview
fun WooPosPhoneProductsScreenPreview() {
    val tabs = listOf(
        WooPosItemsToolbarViewState.Tab.Products(
            name = "Products",
            highlightLevel = WooPosItemsToolbarViewState.Tab.HighlightLevel.Full,
        ),
        WooPosItemsToolbarViewState.Tab.Coupons(
            name = "Coupons",
            highlightLevel = WooPosItemsToolbarViewState.Tab.HighlightLevel.Normal,
        ),
    )
    WooPosTheme {
        WooPosPhoneProductsContent(
            itemsState = WooPosItemsToolbarViewState.ProductList(
                tabs = tabs,
                search = SearchState.Visible(WooPosSearchInputState.Closed),
            ),
            menu = Menu.Hidden,
            onMenuClicked = {},
            onMenuDismissed = {},
            onMenuItemClicked = {},
            onItemsUIEvent = {},
        )
    }
}
