package com.woocommerce.android.ui.woopos.home.items

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchUIEvent
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbarViewState.Tab.Coupons
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbarViewState.Tab.HighlightLevel
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbarViewState.Tab.Products
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsUIEvent.SearchChanged
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel.WooCommerceVersionSunsetBannerState
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsScreen
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsScreenContentPreview
import com.woocommerce.android.ui.woopos.home.items.coupons.search.WooPosCouponsSearchContentPreview
import com.woocommerce.android.ui.woopos.home.items.coupons.search.WooPosCouponsSearchScreen
import com.woocommerce.android.ui.woopos.home.items.products.WooPosItemsScreenPreview
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsScreen
import com.woocommerce.android.ui.woopos.home.items.search.WooPosItemsSearchScreen
import com.woocommerce.android.ui.woopos.home.items.search.WooPosItemsSearchScreenPreview
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
        catalogSyncOverdueBannerStateFlow = productsViewModel.catalogSyncOverdueBannerState,
        wooCommerceVersionSunsetBannerStateFlow = productsViewModel.wooCommerceVersionSunsetBannerState,
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
    catalogSyncOverdueBannerStateFlow: StateFlow<WooPosItemsViewModel.CatalogSyncOverdueBannerState>,
    wooCommerceVersionSunsetBannerStateFlow: StateFlow<WooCommerceVersionSunsetBannerState>,
    productsViewState: LazyListState,
    couponsListState: LazyListState,
    onUIEvent: (WooPosItemsUIEvent) -> Unit,
    productsContent: (@Composable () -> Unit)? = null,
    couponsContent: (@Composable () -> Unit)? = null,
    productsSearchContent: (@Composable () -> Unit)? = null,
    couponsSearchContent: (@Composable () -> Unit)? = null,
) {
    val state = itemsStateFlow.collectAsState()
    val catalogSyncOverdueBannerState = catalogSyncOverdueBannerStateFlow.collectAsState()
    val woocommerceVersionSunsetBannerState = wooCommerceVersionSunsetBannerStateFlow.collectAsState()

    MainItemsList(
        modifier = modifier,
        state = state,
        bannerState = catalogSyncOverdueBannerState,
        wooCommerceVersionSunsetBannerState = woocommerceVersionSunsetBannerState,
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

                WooPosSearchUIEvent.SearchIconClicked -> onUIEvent(WooPosItemsUIEvent.SearchIconClicked)
            }
        },
        onAddCouponEvent = {
            onUIEvent(WooPosItemsUIEvent.AddCouponIconClicked)
        },
        onTabClicked = { onUIEvent(WooPosItemsUIEvent.OnTabClicked(it)) },
        onBackClicked = { onUIEvent(WooPosItemsUIEvent.BackFromVariationsClicked) },
        onSyncWarningBannerDismissed = { onUIEvent(WooPosItemsUIEvent.SyncOverdueBannerDismissed) },
        onWooVersionSunsetBannerDismissed = { onUIEvent(WooPosItemsUIEvent.WooCommerceVersionSunsetBannerDismissed) },
        productsContent = productsContent ?: {
            WooPosProductsScreen(
                modifier = Modifier.padding(horizontal = WooPosSpacing.Medium.value),
                listState = productsViewState
            )
        },
        couponsContent = couponsContent ?: {
            WooPosCouponsScreen(
                modifier = Modifier.padding(horizontal = WooPosSpacing.Medium.value),
                listState = couponsListState,
            )
        },
        productsSearchContent = productsSearchContent ?: { WooPosItemsSearchScreen() },
        couponsSearchContent = couponsSearchContent ?: { WooPosCouponsSearchScreen() },
    )
}

@ExperimentalMaterialApi
@Composable
private fun MainItemsList(
    modifier: Modifier,
    state: State<WooPosItemsToolbarViewState>,
    bannerState: State<WooPosItemsViewModel.CatalogSyncOverdueBannerState>,
    wooCommerceVersionSunsetBannerState: State<WooCommerceVersionSunsetBannerState>,
    productsViewState: LazyListState,
    couponsListState: LazyListState,
    onSearchEvent: (WooPosSearchUIEvent) -> Unit,
    onTabClicked: (WooPosItemsToolbarViewState.Tab) -> Unit,
    onAddCouponEvent: () -> Unit,
    onBackClicked: () -> Unit,
    onSyncWarningBannerDismissed: () -> Unit,
    onWooVersionSunsetBannerDismissed: () -> Unit,
    productsContent: @Composable () -> Unit = {
        WooPosProductsScreen(
            modifier = Modifier.padding(horizontal = WooPosSpacing.Medium.value),
            listState = productsViewState
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
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier.fillMaxHeight()) {
            WooPosItemsToolbar(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(
                        end = WooPosSpacing.Medium.value,
                    ),
                state = state.value,
                onTabClicked = onTabClicked,
                onSearchEvent = onSearchEvent,
                onBackClicked = onBackClicked,
                onAddCouponEvent = onAddCouponEvent,
            )

            Spacer(
                modifier =
                Modifier
                    .height(WooPosSpacing.Small.value)
                    .padding(horizontal = WooPosSpacing.Medium.value)
            )

            WooPosWooCommerceVersionSunsetBanner(
                state = wooCommerceVersionSunsetBannerState.value,
                onDismiss = onWooVersionSunsetBannerDismissed
            )

            WooPosCatalogSyncOverdueBanner(
                state = bannerState.value,
                onDismiss = onSyncWarningBannerDismissed
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

            val currentState = state.value

            Crossfade(
                targetState = getScreenState(currentState),
                animationSpec = tween(
                    durationMillis = 300,
                    easing = LinearEasing,
                ),
            ) { screenState ->
                when (screenState) {
                    ScreenState.Products -> productsContent()
                    ScreenState.ProductsSearch -> productsSearchContent()
                    ScreenState.Coupons -> couponsContent()
                    is ScreenState.Variations -> {
                        WooPosVariationsScreen(
                            modifier = Modifier.padding(
                                horizontal = WooPosSpacing.Medium.value,
                            ),
                            variableProductData = screenState.variableProductData,
                            onBackClicked = { onBackClicked() },
                        )
                    }
                    ScreenState.CouponsSearch -> couponsSearchContent()
                }
            }
        }
    }
}

private sealed class ScreenState {
    object Products : ScreenState()
    object Coupons : ScreenState()
    object ProductsSearch : ScreenState()
    object CouponsSearch : ScreenState()
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

        is WooPosItemsToolbarViewState.VariationList -> ScreenState.Variations(
            variableProductData = state.variableProductData
        )
        is WooPosItemsToolbarViewState.CouponList -> {
            when (val searchState = state.search) {
                WooPosItemsToolbarViewState.SearchState.Hidden -> ScreenState.Coupons
                is WooPosItemsToolbarViewState.SearchState.Visible -> {
                    when (searchState.state) {
                        WooPosSearchInputState.Closed -> ScreenState.Coupons
                        is WooPosSearchInputState.Open -> ScreenState.CouponsSearch
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@WooPosPreview
fun WooPosItemsScreenSearchVisiblePreview(modifier: Modifier = Modifier) {
    val productState = MutableStateFlow(
        WooPosItemsToolbarViewState.ProductList(
            search = WooPosItemsToolbarViewState.SearchState.Visible(
                state = WooPosSearchInputState.Closed
            ),
            tabs = tabs()
        )
    )
    val bannerState = MutableStateFlow(WooPosItemsViewModel.CatalogSyncOverdueBannerState.Visible)
    val wooCommerceVersionSunsetBannerState = MutableStateFlow(WooCommerceVersionSunsetBannerState.Hidden)
    WooPosTheme {
        WooPosItemsScreen(
            modifier = modifier,
            itemsStateFlow = productState,
            catalogSyncOverdueBannerStateFlow = bannerState,
            wooCommerceVersionSunsetBannerStateFlow = wooCommerceVersionSunsetBannerState,
            productsViewState = rememberLazyListState(),
            couponsListState = rememberLazyListState(),
            onUIEvent = {},
            productsContent = { WooPosItemsScreenPreview() },
            couponsContent = { WooPosCouponsScreenContentPreview() },
            productsSearchContent = { WooPosItemsSearchScreenPreview() },
            couponsSearchContent = { WooPosCouponsSearchContentPreview() },
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@WooPosPreview
fun WooPosItemsScreenSearchHiddenPreview(modifier: Modifier = Modifier) {
    val productState = MutableStateFlow(
        WooPosItemsToolbarViewState.ProductList(
            search = WooPosItemsToolbarViewState.SearchState.Hidden,
            tabs = tabs()
        )
    )
    val bannerState = MutableStateFlow(WooPosItemsViewModel.CatalogSyncOverdueBannerState.Hidden)
    val wooCommerceVersionSunsetBannerState = MutableStateFlow(WooCommerceVersionSunsetBannerState.Visible)
    WooPosTheme {
        WooPosItemsScreen(
            modifier = modifier,
            itemsStateFlow = productState,
            catalogSyncOverdueBannerStateFlow = bannerState,
            wooCommerceVersionSunsetBannerStateFlow = wooCommerceVersionSunsetBannerState,
            productsViewState = rememberLazyListState(),
            couponsListState = rememberLazyListState(),
            onUIEvent = {},
            productsContent = { WooPosItemsScreenPreview() },
            couponsContent = { WooPosCouponsScreenContentPreview() },
            productsSearchContent = { WooPosItemsSearchScreenPreview() },
            couponsSearchContent = { WooPosCouponsSearchContentPreview() },
        )
    }
}

@Composable
private fun tabs(): List<WooPosItemsToolbarViewState.Tab> = listOf(
    Products(
        name = "Products",
        highlightLevel = HighlightLevel.Full
    ),
    Coupons(
        name = "Coupons",
        highlightLevel = HighlightLevel.Normal
    ),
)
