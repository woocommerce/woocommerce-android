package com.woocommerce.android.ui.woopos.home.items

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCircularLoadingIndicator
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreen
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreenButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchUIEvent
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
import com.woocommerce.android.ui.woopos.home.WooPosHomeState.CatalogSyncState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbarViewState.Tab.Coupons
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbarViewState.Tab.HighlightLevel
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbarViewState.Tab.Products
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsUIEvent.SearchChanged
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsScreen
import com.woocommerce.android.ui.woopos.home.items.coupons.search.WooPosCouponsSearchScreen
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsScreen
import com.woocommerce.android.ui.woopos.home.items.search.WooPosItemsSearchScreen
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsNavigationData
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WooPosItemsScreen(
    modifier: Modifier = Modifier,
    catalogSyncState: CatalogSyncState = CatalogSyncState.Idle,
    onRetryCatalogSyncClicked: () -> Unit = {}
) {
    val productsViewState = rememberLazyListState()
    val couponsListState = rememberLazyListState()
    val productsViewModel: WooPosItemsViewModel = hiltViewModel()
    WooPosItemsScreen(
        modifier = modifier,
        itemsStateFlow = productsViewModel.viewState,
        productsViewState = productsViewState,
        couponsListState = couponsListState,
        catalogSyncState = catalogSyncState,
        onRetryCatalogSync = onRetryCatalogSyncClicked,
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
    catalogSyncState: CatalogSyncState,
    onRetryCatalogSync: () -> Unit,
    onUIEvent: (WooPosItemsUIEvent) -> Unit,
) {
    val state = itemsStateFlow.collectAsState()

    MainItemsList(
        modifier = modifier,
        state = state,
        productsViewState = productsViewState,
        couponsListState = couponsListState,
        catalogSyncState = catalogSyncState,
        onRetryCatalogSync = onRetryCatalogSync,
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
    )
}

@ExperimentalMaterialApi
@Composable
private fun MainItemsList(
    modifier: Modifier,
    state: State<WooPosItemsToolbarViewState>,
    productsViewState: LazyListState,
    couponsListState: LazyListState,
    catalogSyncState: CatalogSyncState,
    onSearchEvent: (WooPosSearchUIEvent) -> Unit,
    onTabClicked: (WooPosItemsToolbarViewState.Tab) -> Unit,
    onAddCouponEvent: () -> Unit,
    onBackClicked: () -> Unit,
    onRetryCatalogSync: () -> Unit = {},
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
                    .statusBarsPadding()
                    .padding(
                        end = WooPosSpacing.Medium.value.toAdaptivePadding(),
                    ),
                state = state.value,
                onTabClicked = onTabClicked,
                onSearchEvent = onSearchEvent,
                onBackClicked = onBackClicked,
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
                    is ScreenState.CouponsSearch -> WooPosCouponsSearchScreen()
                }
            }
        }

        if (catalogSyncState is CatalogSyncState.Syncing ||
            catalogSyncState is CatalogSyncState.Failed
        ) {
            CatalogSyncOverlay(
                catalogSyncState = catalogSyncState,
                onRetryClicked = onRetryCatalogSync
            )
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

@Composable
private fun CatalogSyncOverlay(
    catalogSyncState: CatalogSyncState,
    onRetryClicked: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        when (catalogSyncState) {
            is CatalogSyncState.Syncing -> {
                SyncingCatalogContent()
            }
            is CatalogSyncState.Failed -> {
                SyncFailedContent(onRetryClicked = onRetryClicked)
            }
            else -> {
                // Should not happen, but handle gracefully
            }
        }
    }
}

@Suppress("WooPosDesignSystemSpacingUsageRule", "WooPosDesignSystemTextUsageRule")
@Composable
private fun SyncingCatalogContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WooPosCircularLoadingIndicator(modifier = Modifier.size(160.dp))
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.woopos_home_syncing_catalog_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SyncFailedContent(onRetryClicked: () -> Unit) {
    WooPosErrorScreen(
        message = stringResource(R.string.woopos_home_sync_failed_title),
        reason = stringResource(R.string.woopos_home_sync_failed_message),
        primaryButton = WooPosErrorScreenButtonState(
            text = stringResource(R.string.woopos_home_sync_failed_retry_button),
            click = onRetryClicked
        )
    )
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
            catalogSyncState = CatalogSyncState.Idle,
            onRetryCatalogSync = {},
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
            catalogSyncState = CatalogSyncState.Idle,
            onRetryCatalogSync = {},
            onUIEvent = {},
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
