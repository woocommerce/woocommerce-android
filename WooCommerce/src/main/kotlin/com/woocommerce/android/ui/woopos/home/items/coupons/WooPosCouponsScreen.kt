package com.woocommerce.android.ui.woopos.home.items.coupons

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosEmptyScreen
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreen
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreenButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosPaginationErrorIndicator
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIcons
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.home.items.WooPosCouponsViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemList
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsLoadingIndicator
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WooPosCouponsScreen(
    modifier: Modifier = Modifier,
    listState: LazyListState,
) {
    val vm: WooPosCouponsViewModel = hiltViewModel()
    WooPosCouponsScreen(
        modifier = modifier,
        listState = listState,
        viewStateFlow = vm.viewState,
        onUIEvent = { vm.onUIEvent(it) },
    )
}

@ExperimentalMaterialApi
@Composable
private fun WooPosCouponsScreen(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    viewStateFlow: StateFlow<WooPosCouponsViewState>,
    onUIEvent: (WooPosCouponsUIEvent) -> Unit,
) {
    val state = viewStateFlow.collectAsState()

    val pullToRefreshState = rememberPullRefreshState(
        refreshing = state.value.pullToRefreshState == WooPosPullToRefreshState.Refreshing,
        onRefresh = { onUIEvent(WooPosCouponsUIEvent.PullToRefreshTriggered) },
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .pullRefresh(
                state = pullToRefreshState,
                enabled = state.value.pullToRefreshState == WooPosPullToRefreshState.Enabled,
            )
    ) {
        when (val itemsState = state.value) {
            is WooPosCouponsViewState.Content -> {
                WooPosItemList(
                    modifier = Modifier.padding(top = WooPosSpacing.XSmall.value),
                    state = itemsState,
                    listState = listState,
                    onItemClicked = { item -> onUIEvent(WooPosCouponsUIEvent.CouponClicked(item.id, item.name)) },
                    onEndOfProductsListReached = { onUIEvent(WooPosCouponsUIEvent.EndOfListReached) },
                ) {
                    CouponsPaginationError(
                        onRetryClicked = {
                            onUIEvent(WooPosCouponsUIEvent.RetryLoadMoreTriggered)
                        }
                    )
                }
            }

            is WooPosCouponsViewState.Loading -> WooPosItemsLoadingIndicator()

            is WooPosCouponsViewState.Empty -> WooPosEmptyScreen(
                modifier = Modifier.fillMaxSize(),
                icon = WooPosIcons.CouponsEmpty,
                title = stringResource(id = R.string.woopos_coupons_empty_list_title),
                message = stringResource(id = R.string.woopos_coupons_empty_list_message),
                contentDescription = stringResource(id = R.string.woopos_coupons_empty_list_image_description),
                actionLabel = stringResource(id = R.string.woopos_coupons_empty_list_create_coupon_label),
                onActionClicked = { onUIEvent(WooPosCouponsUIEvent.CreateCouponClicked) }
            )

            is WooPosCouponsViewState.Error.GenericError -> {
                CouponsError { onUIEvent(WooPosCouponsUIEvent.RetryTriggered) }
            }

            is WooPosCouponsViewState.Error.CouponsDisabledError -> CouponsDisabledError()
        }
        PullRefreshIndicator(
            modifier = Modifier.align(Alignment.TopCenter),
            refreshing = state.value.pullToRefreshState == WooPosPullToRefreshState.Refreshing,
            state = pullToRefreshState
        )
    }
}

@Composable
private fun CouponsPaginationError(onRetryClicked: () -> Unit) {
    WooPosPaginationErrorIndicator(
        message = stringResource(id = R.string.woopos_coupons_pagination_error_title),
        description = stringResource(id = R.string.woopos_coupons_pagination_error_description),
        primaryButton = WooPosErrorScreenButtonState(
            text = stringResource(id = R.string.woopos_coupons_pagination_try_again_label),
            click = onRetryClicked
        ),
    )
}

@Composable
fun CouponsError(onRetryClicked: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        WooPosErrorScreen(
            message = stringResource(id = R.string.woopos_coupons_loading_error_title),
            reason = stringResource(id = R.string.woopos_coupons_loading_error_message),
            primaryButton = WooPosErrorScreenButtonState(
                text = stringResource(id = R.string.woopos_products_loading_error_retry_button),
                click = onRetryClicked
            )
        )
    }
}

@Composable
fun CouponsDisabledError() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        WooPosErrorScreen(
            message = stringResource(id = R.string.woopos_coupons_loading_error_coupons_disabled_title),
            reason = stringResource(id = R.string.woopos_coupons_loading_error_coupons_disabled_message),
            icon = WooPosIcons.CouponsEmpty,
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@WooPosPreview
fun WooPosCouponsScreenContentPreview() {
    val productState = MutableStateFlow(
        WooPosCouponsViewState.Content(
            items = listOf(
                WooPosItemSelectionViewState.Coupon(
                    id = 1L,
                    name = "Coupon 1 Coupon 1 Coupon 1 Coupon 1 Coupon 1 Coupon 1 Coupon 1 Coupon 1 Coupon 1 Coupon 1" +
                        "Coupon 1 Coupon 1 Coupon 1 Coupon 1 Coupon 1 Coupon 1 Coupon 1 Coupon 1 Coupon 1 Coupon 1",
                    summary = "10% off 10% off 10% off 10% off 10% off 10% off 10% off 10% off 10% off 10% off " +
                        "10% off 10% off 10% off 10% off 10% off 10% off 10% off 10% off 10% off 10% off 10% off",
                    expiredState = WooPosItemSelectionViewState.Coupon.ExpiredState.NotExpired,
                ),
                WooPosItemSelectionViewState.Coupon(
                    id = 2L,
                    name = "Coupon 2",
                    summary = "20% off",
                    expiredState = WooPosItemSelectionViewState.Coupon.ExpiredState.Expired(
                        formattedDate = "24 Apr 2025",
                    ),
                ),
            ),
            paginationState = WooPosPaginationState.None,
            pullToRefreshState = WooPosPullToRefreshState.Enabled,
        )
    )
    WooPosTheme {
        WooPosCouponsScreen(
            modifier = Modifier,
            listState = rememberLazyListState(),
            viewStateFlow = productState,
            onUIEvent = {},
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@WooPosPreview
fun WooPosCouponsScreenLoadingMorePreview() {
    val productState = MutableStateFlow(
        WooPosCouponsViewState.Content(
            items = listOf(
                WooPosItemSelectionViewState.Coupon(
                    id = 1L,
                    name = "Coupon 1",
                    summary = "20% off",
                    expiredState = WooPosItemSelectionViewState.Coupon.ExpiredState.NotExpired,
                ),
            ),
            paginationState = WooPosPaginationState.Loading,
            pullToRefreshState = WooPosPullToRefreshState.Enabled,
        )
    )
    WooPosTheme {
        WooPosCouponsScreen(
            modifier = Modifier,
            listState = rememberLazyListState(),
            viewStateFlow = productState,
            onUIEvent = {},
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@WooPosPreview
fun WooPosCouponsScreenLoadingPreview() {
    val productState = MutableStateFlow(
        WooPosCouponsViewState.Loading()
    )
    WooPosTheme {
        WooPosCouponsScreen(
            modifier = Modifier,
            listState = rememberLazyListState(),
            viewStateFlow = productState,
            onUIEvent = {},
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@WooPosPreview
fun WooPosCouponsEmptyListPreview() {
    val productState = MutableStateFlow(
        WooPosCouponsViewState.Empty()
    )
    WooPosTheme {
        WooPosCouponsScreen(
            modifier = Modifier,
            listState = rememberLazyListState(),
            viewStateFlow = productState,
            onUIEvent = {},
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@WooPosPreview
fun WooPosCouponsUIEventScreenErrorPreview() {
    val productState = MutableStateFlow(
        WooPosCouponsViewState.Error.GenericError()
    )
    WooPosTheme {
        WooPosCouponsScreen(
            modifier = Modifier,
            listState = rememberLazyListState(),
            viewStateFlow = productState,
            onUIEvent = {},
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@WooPosPreview
fun WooPosCouponsUIEventScreenCouponsDisabledErrorPreview() {
    val productState = MutableStateFlow(
        WooPosCouponsViewState.Error.CouponsDisabledError()
    )
    WooPosTheme {
        WooPosCouponsScreen(
            modifier = Modifier,
            listState = rememberLazyListState(),
            viewStateFlow = productState,
            onUIEvent = {},
        )
    }
}
