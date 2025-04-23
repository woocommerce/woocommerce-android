package com.woocommerce.android.ui.woopos.home.items

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchUIEvent
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsUIEvent.SearchChanged
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsScreen
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsScreen
import com.woocommerce.android.ui.woopos.home.items.search.WooPosItemsSearchScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WooPosItemsScreen(
    modifier: Modifier = Modifier,
    listState: LazyListState,
) {
    val productsViewModel: WooPosItemsViewModel = hiltViewModel()
    WooPosItemsScreen(
        modifier = modifier,
        itemsStateFlow = productsViewModel.viewState,
        listState = listState,
        onUIEvent = { productsViewModel.onUIEvent(it) },
    )
}

@ExperimentalMaterialApi
@Composable
private fun WooPosItemsScreen(
    modifier: Modifier = Modifier,
    itemsStateFlow: StateFlow<WooPosItemsViewState>,
    listState: LazyListState,
    onUIEvent: (WooPosItemsUIEvent) -> Unit,
) {
    val state = itemsStateFlow.collectAsState()

    MainItemsList(
        modifier = modifier,
        state = state,
        listState = listState,
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
            }
        },
        onTabClicked = { onUIEvent(WooPosItemsUIEvent.OnTabClicked(it)) },
    )
}

@ExperimentalMaterialApi
@Composable
private fun MainItemsList(
    modifier: Modifier,
    state: State<WooPosItemsViewState>,
    listState: LazyListState,
    onSearchEvent: (WooPosSearchUIEvent) -> Unit,
    onTabClicked: (WooPosItemsViewState.Tab) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = WooPosSpacing.Medium.value.toAdaptivePadding(),
                end = WooPosSpacing.Medium.value.toAdaptivePadding(),
                top = WooPosSpacing.XLarge.value.toAdaptivePadding(),
                bottom = WooPosSpacing.None.value.toAdaptivePadding(),
            )
    ) {
        Column(
            modifier.fillMaxHeight()
        ) {
            WooPosItemsToolbar(
                state = state.value,
                onTabClicked = onTabClicked,
                onSearchEvent = onSearchEvent,
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

            when (val itemsState = state.value) {
                is WooPosItemsViewState.ProductList -> {
                    Column {
                        when (val searchState = itemsState.search) {
                            WooPosItemsViewState.SearchState.Hidden -> {
                                WooPosProductsScreen(modifier = Modifier, listState = listState)
                            }
                            is WooPosItemsViewState.SearchState.Visible -> {
                                when (searchState.state) {
                                    WooPosSearchInputState.Closed -> {
                                        WooPosProductsScreen(modifier = Modifier, listState = listState)
                                    }

                                    is WooPosSearchInputState.Open -> WooPosItemsSearchScreen()
                                }
                            }
                        }
                    }
                }
                is WooPosItemsViewState.CouponList -> WooPosCouponsScreen(modifier = Modifier)
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@WooPosPreview
fun WooPosItemsScreenSearchVisiblePreview(modifier: Modifier = Modifier) {
    val productState = MutableStateFlow(
        WooPosItemsViewState.ProductList(
            banner = WooPosItemsViewState.BannerState.Hidden,
            search = WooPosItemsViewState.SearchState.Visible(
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
            listState = rememberLazyListState(),
            onUIEvent = {},
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@WooPosPreview
fun WooPosItemsScreenSearchHiddenPreview(modifier: Modifier = Modifier) {
    val productState = MutableStateFlow(
        WooPosItemsViewState.ProductList(
            banner = WooPosItemsViewState.BannerState.Hidden,
            search = WooPosItemsViewState.SearchState.Visible(
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
            listState = rememberLazyListState(),
            onUIEvent = {},
        )
    }
}

@Composable
private fun tabs(): List<WooPosItemsViewState.Tab> = listOf(
    WooPosItemsViewState.Tab(
        stringId = R.string.woopos_products_screen_title,
        highlightLevel = WooPosItemsViewState.Tab.HighlightLevel.Full
    ),
    WooPosItemsViewState.Tab(
        stringId = R.string.woopos_coupons_screen_title,
        highlightLevel = WooPosItemsViewState.Tab.HighlightLevel.Normal
    )
)
