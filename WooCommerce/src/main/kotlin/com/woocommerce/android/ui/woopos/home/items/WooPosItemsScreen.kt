package com.woocommerce.android.ui.woopos.home.items

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosFloatingKeyboardHint
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosKeyboardStatus
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchUIEvent
import com.woocommerce.android.ui.woopos.common.composeui.component.rememberKeyboardStatus
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
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
import kotlinx.coroutines.delay
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
    onSearchEvent: (WooPosSearchUIEvent) -> Unit,
    onTabClicked: (WooPosItemsToolbarViewState.Tab) -> Unit,
    onAddCouponEvent: () -> Unit,
    onBackClicked: () -> Unit,
) {
    val keyboardStatus = rememberKeyboardStatus()
    var showKeyboardHint by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val currentState = state.value
    val isSearchOpen = (currentState.search as? WooPosItemsToolbarViewState.SearchState.Visible)?.let {
        it.state is WooPosSearchInputState.Open
    } == true

    LaunchedEffect(isSearchOpen, keyboardStatus) {
        if (isSearchOpen && keyboardStatus == WooPosKeyboardStatus.HardwareKeyboardConnected) {
            delay(1000)
            showKeyboardHint = true
        } else {
            showKeyboardHint = false
        }
    }

    ConstraintLayout(
        modifier = modifier
            .fillMaxSize()
            .animateContentSize(animationSpec = spring())
    ) {
        val (toolbar, keyboardHint, mainContent) = createRefs()
        WooPosItemsToolbar(
            modifier = Modifier
                .statusBarsPadding()
                .padding(
                    end = WooPosSpacing.Medium.value.toAdaptivePadding(),
                )
                .constrainAs(toolbar) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            state = state.value,
            onTabClicked = onTabClicked,
            onSearchEvent = onSearchEvent,
            onBackClicked = onBackClicked,
            onAddCouponEvent = onAddCouponEvent,
        )

        AnimatedVisibility(
            visible = showKeyboardHint,
            enter = expandVertically(
                animationSpec = spring(),
                expandFrom = Alignment.Top
            ),
            exit = shrinkVertically(
                animationSpec = spring(),
                shrinkTowards = Alignment.Top
            ),
            modifier = Modifier.constrainAs(keyboardHint) {
                top.linkTo(toolbar.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        ) {
            WooPosFloatingKeyboardHint(
                title = stringResource(R.string.woopos_keyboard_hint_title),
                message = stringResource(R.string.woopos_keyboard_hint_message),
                actionText = stringResource(R.string.woopos_keyboard_hint_action),
                onDismiss = { showKeyboardHint = false },
                onOpenSettings = { openKeyboardSettings(context) },
                modifier = Modifier.padding(
                    start = WooPosSpacing.Medium.value.toAdaptivePadding(),
                    end = WooPosSpacing.Medium.value.toAdaptivePadding(),
                    top = WooPosSpacing.Small.value
                )
            )
        }

        Crossfade(
            targetState = getScreenState(currentState),
            animationSpec = tween(
                durationMillis = 300,
                easing = LinearEasing,
            ),
            modifier = Modifier.constrainAs(mainContent) {
                top.linkTo(if (showKeyboardHint) keyboardHint.bottom else toolbar.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(parent.bottom)
                height = Dimension.fillToConstraints
                width = Dimension.fillToConstraints
            }
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
    Products(
        name = "Products",
        highlightLevel = HighlightLevel.Full
    ),
    Coupons(
        name = "Coupons",
        highlightLevel = HighlightLevel.Normal
    ),
)

private fun openKeyboardSettings(context: Context) {
    try {
        openInputMethodSettings(context)
    } catch (e: ActivityNotFoundException) {
        Log.e("WooPosItemsScreen", "Failed to open hard keyboard settings", e)
        openHardKeyboardSettings(context)
    }
}

private fun openHardKeyboardSettings(context: Context) {
    val intent = Intent(Settings.ACTION_HARD_KEYBOARD_SETTINGS)
    context.startActivity(intent)
}

private fun openInputMethodSettings(context: Context) {
    val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
    context.startActivity(intent)
}
