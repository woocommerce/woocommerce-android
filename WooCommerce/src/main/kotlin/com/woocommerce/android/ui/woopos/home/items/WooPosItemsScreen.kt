package com.woocommerce.android.ui.woopos.home.items

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
import com.woocommerce.android.ui.woopos.common.composeui.component.toItemsUIEvent
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbarViewState.Tab.Coupons
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbarViewState.Tab.HighlightLevel
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbarViewState.Tab.Products
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsScreen
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsScreenContentPreview
import com.woocommerce.android.ui.woopos.home.items.coupons.search.WooPosCouponsSearchContentPreview
import com.woocommerce.android.ui.woopos.home.items.coupons.search.WooPosCouponsSearchScreen
import com.woocommerce.android.ui.woopos.home.items.customamount.WooPosCustomAmountFormScreen
import com.woocommerce.android.ui.woopos.home.items.products.WooPosItemsScreenPreview
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsScreen
import com.woocommerce.android.ui.woopos.home.items.search.WooPosItemsSearchScreen
import com.woocommerce.android.ui.woopos.home.items.search.WooPosItemsSearchScreenPreview
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WooPosItemsScreen(
    modifier: Modifier = Modifier,
    viewModel: WooPosItemsViewModel = hiltViewModel(),
) {
    val productsViewState = rememberLazyListState()
    val couponsListState = rememberLazyListState()
    WooPosItemsScreen(
        modifier = modifier,
        itemsStateFlow = viewModel.viewState,
        bannerStateFlow = viewModel.bannerState,
        productsViewState = productsViewState,
        couponsListState = couponsListState,
        onUIEvent = { viewModel.onUIEvent(it) },
    )
}

@ExperimentalMaterialApi
@Composable
private fun WooPosItemsScreen(
    modifier: Modifier = Modifier,
    itemsStateFlow: StateFlow<WooPosItemsToolbarViewState>,
    bannerStateFlow: StateFlow<WooPosItemsBannerState>,
    productsViewState: LazyListState,
    couponsListState: LazyListState,
    onUIEvent: (WooPosItemsUIEvent) -> Unit,
    productsContent: (@Composable () -> Unit)? = null,
    couponsContent: (@Composable () -> Unit)? = null,
    productsSearchContent: (@Composable () -> Unit)? = null,
    couponsSearchContent: (@Composable () -> Unit)? = null,
) {
    val state = itemsStateFlow.collectAsState()
    val bannerState = bannerStateFlow.collectAsState()

    MainItemsList(
        modifier = modifier,
        state = state,
        bannerState = bannerState,
        productsViewState = productsViewState,
        couponsListState = couponsListState,
        onSearchEvent = { onUIEvent(it.toItemsUIEvent()) },
        onAddCouponEvent = {
            onUIEvent(WooPosItemsUIEvent.AddCouponIconClicked)
        },
        onTabClicked = { onUIEvent(WooPosItemsUIEvent.OnTabClicked(it)) },
        onBackClicked = { onUIEvent(WooPosItemsUIEvent.BackFromVariationsClicked) },
        onSyncWarningBannerDismissed = { onUIEvent(WooPosItemsUIEvent.SyncOverdueBannerDismissed) },
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
    bannerState: State<WooPosItemsBannerState>,
    productsViewState: LazyListState,
    couponsListState: LazyListState,
    onSearchEvent: (WooPosSearchUIEvent) -> Unit,
    onTabClicked: (WooPosItemsToolbarViewState.Tab) -> Unit,
    onAddCouponEvent: () -> Unit,
    onBackClicked: () -> Unit,
    onSyncWarningBannerDismissed: () -> Unit,
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

            when (bannerState.value) {
                WooPosItemsBannerState.SyncOverdue -> {
                    WooPosCatalogSyncOverdueBanner(
                        onDismiss = onSyncWarningBannerDismissed
                    )
                }
                WooPosItemsBannerState.Hidden -> { }
            }

            Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

            WooPosItemsContent(
                state = state.value,
                productsContent = productsContent,
                couponsContent = couponsContent,
                productsSearchContent = productsSearchContent,
                couponsSearchContent = couponsSearchContent,
                variationsContent = { data ->
                    WooPosVariationsScreen(
                        modifier = Modifier.padding(horizontal = WooPosSpacing.Medium.value),
                        variableProductData = data,
                        onBackClicked = onBackClicked,
                    )
                },
                customAmountFormContent = { editing ->
                    WooPosCustomAmountFormScreen(
                        editing = editing,
                        onBackClick = onBackClicked,
                    )
                },
            )
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
    val bannerState = MutableStateFlow(WooPosItemsBannerState.SyncOverdue)
    WooPosTheme {
        WooPosItemsScreen(
            modifier = modifier,
            itemsStateFlow = productState,
            bannerStateFlow = bannerState,
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
    val bannerState = MutableStateFlow(WooPosItemsBannerState.Hidden)
    WooPosTheme {
        WooPosItemsScreen(
            modifier = modifier,
            itemsStateFlow = productState,
            bannerStateFlow = bannerState,
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
