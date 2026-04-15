package com.woocommerce.android.ui.woopos.home.items

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbarViewState.SearchState
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsScreen
import com.woocommerce.android.ui.woopos.home.items.coupons.search.WooPosCouponsSearchScreen
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsScreen
import com.woocommerce.android.ui.woopos.home.items.search.WooPosItemsSearchScreen
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsNavigationData
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsScreen

@Composable
fun WooPosItemsContent(
    state: WooPosItemsToolbarViewState,
    onBackFromVariations: () -> Unit,
    modifier: Modifier = Modifier,
    productsListState: LazyListState = rememberLazyListState(),
    couponsListState: LazyListState = rememberLazyListState(),
    productsContent: @Composable () -> Unit = {
        WooPosProductsScreen(
            modifier = Modifier.padding(horizontal = WooPosSpacing.Medium.value),
            listState = productsListState,
        )
    },
    couponsContent: @Composable () -> Unit = {
        WooPosCouponsScreen(
            modifier = Modifier.padding(horizontal = WooPosSpacing.Medium.value),
            listState = couponsListState,
        )
    },
    productsSearchContent: @Composable () -> Unit = { WooPosItemsSearchScreen() },
    couponsSearchContent: @Composable () -> Unit = { WooPosCouponsSearchScreen() },
    variationsContent: @Composable (WooPosVariationsNavigationData) -> Unit = { data ->
        WooPosVariationsScreen(
            modifier = Modifier.padding(horizontal = WooPosSpacing.Medium.value),
            variableProductData = data,
            onBackClicked = onBackFromVariations,
        )
    },
) {
    Crossfade(
        targetState = state.toScreenState(),
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        modifier = modifier,
    ) { screenState ->
        when (screenState) {
            ItemsScreenState.Products -> productsContent()
            ItemsScreenState.Coupons -> couponsContent()
            ItemsScreenState.ProductsSearch -> productsSearchContent()
            ItemsScreenState.CouponsSearch -> couponsSearchContent()
            is ItemsScreenState.Variations -> variationsContent(screenState.variableProductData)
        }
    }
}

internal sealed class ItemsScreenState {
    data object Products : ItemsScreenState()
    data object Coupons : ItemsScreenState()
    data object ProductsSearch : ItemsScreenState()
    data object CouponsSearch : ItemsScreenState()
    data class Variations(val variableProductData: WooPosVariationsNavigationData) : ItemsScreenState()
}

internal fun WooPosItemsToolbarViewState.toScreenState(): ItemsScreenState = when (this) {
    is WooPosItemsToolbarViewState.ProductList -> when (val s = search) {
        SearchState.Hidden -> ItemsScreenState.Products
        is SearchState.Visible -> when (s.state) {
            WooPosSearchInputState.Closed -> ItemsScreenState.Products
            is WooPosSearchInputState.Open -> ItemsScreenState.ProductsSearch
        }
    }
    is WooPosItemsToolbarViewState.VariationList -> ItemsScreenState.Variations(variableProductData)
    is WooPosItemsToolbarViewState.CouponList -> when (val s = search) {
        SearchState.Hidden -> ItemsScreenState.Coupons
        is SearchState.Visible -> when (s.state) {
            WooPosSearchInputState.Closed -> ItemsScreenState.Coupons
            is WooPosSearchInputState.Open -> ItemsScreenState.CouponsSearch
        }
    }
}
