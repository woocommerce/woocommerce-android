package com.woocommerce.android.ui.woopos.home.items

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchUIEvent
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbarViewState.Tab.CouponTab
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbarViewState.Tab.HighlightLevel
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbarViewState.Tab.ProductTab
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsUIEvent.SearchChanged
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsScreen
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsScreen
import com.woocommerce.android.ui.woopos.home.items.search.WooPosItemsSearchScreen
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsNavigationData
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WooPosItemsScreen(modifier: Modifier = Modifier) {
    val productsViewState = rememberLazyListState()
    val couponsListState = rememberLazyListState()
    val productsViewModel: WooPosItemsViewModel = hiltViewModel()
    WooPosItemsScreen(
        modifier = modifier,
        itemsStateFlow = productsViewModel.viewState,
        productsViewState = productsViewState,
        couponsListState = couponsListState,
        onUIEvent = { productsViewModel.onUIEvent(it) },
    )
}

@ExperimentalMaterialApi
@Composable
private fun WooPosItemsScreen(
    modifier: Modifier = Modifier,
    itemsStateFlow: StateFlow<WooPosItemsToolbarViewState>,
    productsViewState: LazyListState,
    couponsListState: LazyListState,
    onUIEvent: (WooPosItemsUIEvent) -> Unit,
) {
    val state = itemsStateFlow.collectAsState()

    MainItemsList(
        modifier = modifier,
        state = state,
        productsViewState = productsViewState,
        couponsListState = couponsListState,
        onSearchEvent = {
            when (it) {
                WooPosSearchUIEvent.Clear -> onUIEvent(WooPosItemsUIEvent.ClearSearchClicked)
                WooPosSearchUIEvent.Close -> onUIEvent(WooPosItemsUIEvent.CloseSearchClicked)
                is WooPosSearchUIEvent.Search -> onUIEvent(
                    SearchChanged(
                        query = it.query,
                        cursorPosition = it.cursorPosition,
                    )
                )

                is WooPosSearchUIEvent.AnimationComplete -> {
                    onUIEvent(WooPosItemsUIEvent.SearchAnimationComplete)
                }

                WooPosSearchUIEvent.SearchIconClicked -> onUIEvent(WooPosItemsUIEvent.SearchIconClicked)
            }
        },
        onAddCouponEvent = {
            onUIEvent(WooPosItemsUIEvent.AddCouponIconClicked)
        },
        onTabClicked = { onUIEvent(WooPosItemsUIEvent.OnTabClicked(it)) },
        onBackClicked = { onUIEvent(WooPosItemsUIEvent.BackButtonClicked) },
    )
}

@ExperimentalMaterialApi
@Composable
private fun MainItemsList(
    modifier: Modifier,
    state: State<WooPosItemsToolbarViewState>,
    productsViewState: LazyListState,
    couponsListState: LazyListState,
    onSearchEvent: (WooPosSearchUIEvent) -> Unit,
    onTabClicked: (WooPosItemsToolbarViewState.Tab) -> Unit,
    onAddCouponEvent: () -> Unit,
    onBackClicked: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(
            modifier.fillMaxHeight()
        ) {
            WooPosItemsToolbar(
                modifier = Modifier
                    .padding(
                        top = WooPosSpacing.XLarge.value.toAdaptivePadding(),
                        end = WooPosSpacing.Medium.value.toAdaptivePadding(),
                    ),
                state = state.value,
                onTabClicked = onTabClicked,
                onSearchEvent = onSearchEvent,
                onAddCouponEvent = onAddCouponEvent,
            )

            val currentState = state.value

            Crossfade(
                targetState = getScreenState(currentState),
                animationSpec = tween(
                    durationMillis = 300,
                    easing = LinearEasing,
                ),
            ) { screenState ->
                when (screenState) {
                    ScreenState.Products -> WooPosProductsScreen(
                        modifier = Modifier.padding(
                            horizontal = WooPosSpacing.Medium.value.toAdaptivePadding(),
                        ),
                        listState = productsViewState
                    )

                    ScreenState.ProductsSearch -> WooPosItemsSearchScreen()
                    ScreenState.Coupons -> WooPosCouponsScreen(
                        modifier = Modifier.padding(
                            horizontal = WooPosSpacing.Medium.value.toAdaptivePadding(),
                        ),
                        listState = couponsListState,
                    )

                    is ScreenState.Variations -> {
                        WooPosVariationsScreen(
                            modifier = Modifier.padding(
                                horizontal = WooPosSpacing.Medium.value.toAdaptivePadding(),
                            ),
                            variableProductData = screenState.variableProductData,
                            onBackClicked = { onBackClicked() },
                        )
                    }
                }
            }
        }
    }
}

private sealed class ScreenState {
    object Products : ScreenState()
    object Coupons : ScreenState()
    object ProductsSearch : ScreenState()
    data class Variations(val variableProductData: WooPosVariationsNavigationData) : ScreenState()
}

private fun getScreenState(state: WooPosItemsToolbarViewState): ScreenState {
    return when (state) {
        is WooPosItemsToolbarViewState.ProductList -> {
            when (val searchState = state.search) {
                WooPosItemsToolbarViewState.SearchState.Hidden -> ScreenState.Products
                is WooPosItemsToolbarViewState.SearchState.Visible -> {
                    when (searchState.state) {
                        WooPosSearchInputState.Closed -> ScreenState.Products
                        is WooPosSearchInputState.Open -> ScreenState.ProductsSearch
                    }
                }
            }
        }

        is WooPosItemsToolbarViewState.CouponList -> ScreenState.Coupons
        is WooPosItemsToolbarViewState.VariationList -> ScreenState.Variations(
            variableProductData = state.variableProductData
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@WooPosPreview
fun WooPosItemsScreenSearchVisiblePreview(modifier: Modifier = Modifier) {
    val productState = MutableStateFlow(
        WooPosItemsToolbarViewState.ProductList(
            search = WooPosItemsToolbarViewState.SearchState.Visible(
                state = WooPosSearchInputState.Open(
                    input = WooPosSearchInputState.Open.Input.Query("", 0),
                    isLoading = false,
                )
            ),
            tabs = tabs()
        )
    )
    WooPosTheme {
        WooPosItemsScreen(
            modifier = modifier,
            itemsStateFlow = productState,
            productsViewState = rememberLazyListState(),
            couponsListState = rememberLazyListState(),
            onUIEvent = {},
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@WooPosPreview
fun WooPosItemsScreenSearchHiddenPreview(modifier: Modifier = Modifier) {
    val productState = MutableStateFlow(
        WooPosItemsToolbarViewState.ProductList(
            search = WooPosItemsToolbarViewState.SearchState.Visible(
                state = WooPosSearchInputState.Open(
                    input = WooPosSearchInputState.Open.Input.Query("", 0),
                    isLoading = false,
                )
            ),
            tabs = tabs()
        )
    )
    WooPosTheme {
        WooPosItemsScreen(
            modifier = modifier,
            itemsStateFlow = productState,
            productsViewState = rememberLazyListState(),
            couponsListState = rememberLazyListState(),
            onUIEvent = {},
        )
    }
}

@Composable
private fun tabs(): List<WooPosItemsToolbarViewState.Tab> = listOf(
    ProductTab(
        name = "Products",
        highlightLevel = HighlightLevel.Full
    ),
    CouponTab(
        name = "Coupons",
        highlightLevel = HighlightLevel.Normal
    ),
)
