package com.woocommerce.android.ui.woopos.home.items.coupons.search

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosEmptyScreen
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreen
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreenButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosPaginationErrorIndicator
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.home.items.WooPosItemList
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsLoadingIndicator

@Composable
fun WooPosCouponsSearchScreen(
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<WooPosCouponsSearchViewModel>()
    val state = viewModel.viewState.collectAsState().value
    WooPosCouponsSearchScreen(
        modifier = modifier.imePadding(),
        state = state,
        onUIEvent = viewModel::onUIEvent,
    )
}

@Composable
private fun WooPosCouponsSearchScreen(
    modifier: Modifier = Modifier,
    state: WooPosCouponsSearchViewState,
    onUIEvent: (WooPosCouponsSearchUiEvent) -> Unit = {},
) {
    val listState = rememberLazyListState()
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        val stateClass by remember(state) {
            derivedStateOf {
                state.javaClass
            }
        }

        if (state is WooPosCouponsSearchViewState.Content) {
            LaunchedEffect(state.searchQuery) {
                listState.scrollToItem(0)
            }
        }

        Crossfade(
            targetState = stateClass,
            label = "WooPosCouponsSearchScreenCrossfade"
        ) { currentStateClass ->
            when (currentStateClass) {
                WooPosCouponsSearchViewState.EmptySearchQuery::class.java -> {
                    if (state is WooPosCouponsSearchViewState.EmptySearchQuery) {
                        WooPosCouponsEmptySearchQueryStateScreen(
                            modifier = Modifier.imePadding(),
                            state = state,
                            onUIEvent = onUIEvent
                        )
                    }
                }

                WooPosCouponsSearchViewState.Content::class.java -> {
                    if (state is WooPosCouponsSearchViewState.Content) {
                        WooPosCouponsSearchContent(
                            modifier = Modifier.padding(
                                horizontal = WooPosSpacing.Medium.value,
                            ),
                            listState = listState,
                            state = state,
                            onUIEvent = onUIEvent
                        )
                    }
                }

                WooPosCouponsSearchViewState.Empty::class.java -> {
                    WooPosEmptyScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                horizontal = WooPosSpacing.Medium.value,
                            )
                            .imePadding(),
                        title = stringResource(id = R.string.woopos_search_coupons_empty_title),
                        message = stringResource(id = R.string.woopos_search_empty_coupons_description),
                        contentDescription = stringResource(
                            id = R.string.woopos_search_empty_image_content_description
                        ),
                    )
                }

                WooPosCouponsSearchViewState.Error::class.java -> {
                    if (state is WooPosCouponsSearchViewState.Error) {
                        WooPosErrorScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = WooPosSpacing.Medium.value)
                                .verticalScroll(rememberScrollState())
                                .imePadding(),
                            message = stringResource(id = R.string.woopos_search_coupons_error_title),
                            reason = stringResource(id = R.string.woopos_search_items_error_description),
                            primaryButton = WooPosErrorScreenButtonState(
                                text = stringResource(id = R.string.woopos_products_loading_error_retry_button),
                                click = { onUIEvent(WooPosCouponsSearchUiEvent.LoadingErrorRetryButtonClicked) }
                            )
                        )
                    }
                }

                WooPosCouponsSearchViewState.Loading::class.java -> {
                    WooPosItemsLoadingIndicator(
                        modifier = Modifier
                            .padding(
                                start = WooPosSpacing.Medium.value,
                                end = WooPosSpacing.Medium.value,
                                top = WooPosSpacing.Small.value
                            )
                            .padding(top = WooPosSpacing.XSmall.value),
                        itemsCount = 5
                    )
                }

                else -> error("Unsupported state: $currentStateClass")
            }
        }
    }
}

@Composable
private fun WooPosCouponsSearchContent(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    state: WooPosCouponsSearchViewState.Content,
    onUIEvent: (WooPosCouponsSearchUiEvent) -> Unit
) {
    val focusManager = LocalFocusManager.current

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            focusManager.clearFocus()
        }
    }

    WooPosItemList(
        modifier = modifier.padding(top = WooPosSpacing.XSmall.value),
        state = state,
        listState = listState,
        animateItems = false,
        onItemClicked = {
            if (it is WooPosItemSelectionViewState.Coupon) {
                onUIEvent(WooPosCouponsSearchUiEvent.OnCouponClicked(it))
            }
        },
        onEndOfProductsListReached = { onUIEvent(WooPosCouponsSearchUiEvent.OnNextPageRequested) },
        onErrorWhilePaginating = {
            WooPosPaginationErrorIndicator(
                message = stringResource(id = R.string.woopos_coupons_pagination_error_title),
                description = stringResource(id = R.string.woopos_items_pagination_error_description),
                primaryButton = WooPosErrorScreenButtonState(
                    text = stringResource(id = R.string.woopos_items_pagination_try_again_label),
                    click = { onUIEvent(WooPosCouponsSearchUiEvent.OnNextPageRequested) }
                ),
            )
        }
    )
}

@Composable
@WooPosPreview
fun WooPosCouponsSearchContentPreview() {
    WooPosTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(WooPosSpacing.Medium.value)
        ) {
            WooPosCouponsSearchContent(
                state = WooPosCouponsSearchViewState.Content(
                    searchQuery = "summer",
                    items = listOf(
                        WooPosItemSelectionViewState.Coupon(
                            id = 1,
                            name = "SUMMER10",
                            summary = "10% off, minimum spend $50.00",
                            expiredState = WooPosItemSelectionViewState.Coupon.ExpiredState.NotExpired
                        ),
                        WooPosItemSelectionViewState.Coupon(
                            id = 2,
                            name = "SUMMER20",
                            summary = "20% off, minimum spend $100.00",
                            expiredState = WooPosItemSelectionViewState.Coupon.ExpiredState.NotExpired
                        ),
                    )
                ),
                listState = rememberLazyListState(),
                onUIEvent = {},
            )
        }
    }
}

@Composable
@WooPosPreview
fun WooPosCouponsSearchEmptyPreview() {
    WooPosTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(WooPosSpacing.Medium.value)
        ) {
            WooPosCouponsSearchScreen(
                state = WooPosCouponsSearchViewState.Empty,
                onUIEvent = {}
            )
        }
    }
}
