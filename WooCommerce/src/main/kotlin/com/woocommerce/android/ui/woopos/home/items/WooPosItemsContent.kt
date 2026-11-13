package com.woocommerce.android.ui.woopos.home.items

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbarViewState.SearchState
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsNavigationData

@Composable
fun WooPosItemsContent(
    state: WooPosItemsToolbarViewState,
    productsContent: @Composable () -> Unit,
    couponsContent: @Composable () -> Unit,
    productsSearchContent: @Composable () -> Unit,
    couponsSearchContent: @Composable () -> Unit,
    variationsContent: @Composable (WooPosVariationsNavigationData) -> Unit,
    modifier: Modifier = Modifier,
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
