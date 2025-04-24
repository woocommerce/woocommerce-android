package com.woocommerce.android.ui.woopos.home.items.coupons

import com.woocommerce.android.ui.woopos.home.items.WooPosCouponsViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsDataSource.FetchingCouponsState.FETCHING_FIRST_PAGE
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsDataSource.FetchingCouponsState.IDLE
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsDataSource.FetchingCouponsState.LOADING_MORE
import com.woocommerce.android.ui.woopos.util.GetCachedStoreCurrency
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatCouponSummary
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import javax.inject.Inject

class WooPosCouponsListViewStateManager @Inject constructor(
    private val couponsDataSource: WooPosCouponsDataSource,
    private val formatCouponSummary: WooPosFormatCouponSummary,
    private val getCachedStoreCurrency: GetCachedStoreCurrency,
) {
    private val errorStates = MutableSharedFlow<CouponsErrorEvent>()

    private val contentFlow =
        couponsDataSource.couponsFlow.combine(couponsDataSource.fetchingState) { coupons, fetchingState ->
            if (coupons.isEmpty()) {
                if (fetchingState == IDLE) WooPosCouponsViewState.Empty()
                else WooPosCouponsViewState.Loading()
            } else {
                WooPosCouponsViewState.Content(
                    items = coupons.map { coupon ->
                        WooPosItemSelectionViewState.Coupon(
                            id = coupon.id,
                            name = coupon.code.orEmpty(),
                            summary = formatCouponSummary(coupon, getCachedStoreCurrency())
                        )
                    }, paginationState = when (fetchingState) {
                        IDLE, FETCHING_FIRST_PAGE -> WooPosPaginationState.None
                        LOADING_MORE -> WooPosPaginationState.Loading
                    }, pullToRefreshState = when (fetchingState) {
                        IDLE -> WooPosPullToRefreshState.Enabled
                        // We might need to keep this enabled and cancel the loadMore job when PTR starts.
                        LOADING_MORE -> WooPosPullToRefreshState.Disabled
                        FETCHING_FIRST_PAGE -> WooPosPullToRefreshState.Refreshing
                    }
                )
            }
        }.onStart {
            // initial load
            val result = couponsDataSource.clearCacheAndFetchFirstPage()
            if (!result.isSuccess) {
                // emit an Error state if first‐page fails
                errorStates.emit(CouponsErrorEvent.FullScreen)
            }
        }

    val viewState: Flow<WooPosCouponsViewState> = merge(
        contentFlow,
        errorStates
    ).scan(WooPosCouponsViewState.Loading() as WooPosCouponsViewState) { previousState, newState ->
        when (newState) {
            CouponsErrorEvent.FullScreen -> {
                WooPosCouponsViewState.Error()
            }

            CouponsErrorEvent.Pagination -> {
                if (previousState is WooPosCouponsViewState.Content) {
                    previousState.copy(paginationState = WooPosPaginationState.Error)
                } else {
                    WooPosCouponsViewState.Error()
                }
            }

            is WooPosCouponsViewState.Content -> newState
            is WooPosCouponsViewState.Error -> newState
            is WooPosCouponsViewState.Loading -> newState
            is WooPosCouponsViewState.Empty -> newState
            else -> error("Unknown state: $newState")
        }
    }

    suspend fun fetchCoupons() {
        val result = couponsDataSource.clearCacheAndFetchFirstPage()
        if (!result.isSuccess) {
            delay(500) // avoid UI flickering when there is no network connection
            errorStates.emit(CouponsErrorEvent.FullScreen)
        }
    }

    suspend fun loadMore() {
        val result = couponsDataSource.loadMore()
        if (!result.isSuccess) {
            delay(500) // avoid UI flickering when there is no network connection
            errorStates.emit(CouponsErrorEvent.Pagination)
        }
    }

    private enum class CouponsErrorEvent {
        FullScreen, Pagination
    }
}
