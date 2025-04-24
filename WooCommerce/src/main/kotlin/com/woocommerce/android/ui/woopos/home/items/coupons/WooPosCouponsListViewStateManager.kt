package com.woocommerce.android.ui.woopos.home.items.coupons

import com.woocommerce.android.model.Coupon
import com.woocommerce.android.ui.woopos.home.items.WooPosCouponsViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState.Error
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState.Loading
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState.None
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState.Disabled
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState.Enabled
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState.Refreshing
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsListViewStateManager.FetchingCouponsState.ERROR_FETCHING_FIRST_PAGE
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsListViewStateManager.FetchingCouponsState.ERROR_LOADING_MORE
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsListViewStateManager.FetchingCouponsState.FETCHING_FIRST_PAGE
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsListViewStateManager.FetchingCouponsState.IDLE
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsListViewStateManager.FetchingCouponsState.LOADING_MORE
import com.woocommerce.android.ui.woopos.util.GetCachedStoreCurrency
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatCouponSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

class WooPosCouponsListViewStateManager @Inject constructor(
    private val couponsDataSource: WooPosCouponsDataSource,
    private val formatCouponSummary: WooPosFormatCouponSummary,
    private val getCachedStoreCurrency: GetCachedStoreCurrency,
) {
    private val fetchingState: MutableStateFlow<FetchingCouponsState> = MutableStateFlow(IDLE)

    private var loadingMoreJob: Job? = null
    private var fetchingFirstPageJob: Job? = null
    private var canLoadMore: Boolean = false

    enum class FetchingCouponsState {
        IDLE, LOADING_MORE, FETCHING_FIRST_PAGE, ERROR_LOADING_MORE, ERROR_FETCHING_FIRST_PAGE
    }

    private val contentFlow = couponsDataSource.couponsFlow.combine(fetchingState) { coupons, fetchingState ->
        if (fetchingState == FETCHING_FIRST_PAGE) {
            WooPosCouponsViewState.Loading()
        } else if (coupons.isEmpty()) {
            return@combine if (fetchingState == IDLE) {
                WooPosCouponsViewState.Empty()
            } else {
                WooPosCouponsViewState.Loading()
            }
        } else {
            return@combine if (fetchingState == ERROR_FETCHING_FIRST_PAGE) {
                WooPosCouponsViewState.Error()
            } else {
                WooPosCouponsViewState.Content(
                    items = mapCouponsToSelectionState(coupons),
                    paginationState = mapFetchingStateToPaginationState(fetchingState),
                    pullToRefreshState = mapFetchingStateToPTRState(fetchingState)
                )
            }
        }
    }

    val viewState: Flow<WooPosCouponsViewState> = contentFlow

    fun fetchCoupons(viewModelScope: CoroutineScope) {
        if (fetchingFirstPageJob?.isActive == true) {
            return
        }

        fetchingFirstPageJob = viewModelScope.launch(Dispatchers.IO) {
            loadingMoreJob?.cancelAndJoin()
            fetchingState.emit(FETCHING_FIRST_PAGE)
            val result = couponsDataSource.clearCacheAndFetchFirstPage()
            if (!result.isSuccess) {
                delay(500) // avoid UI flickering when there is no network connection
                fetchingState.emit(ERROR_FETCHING_FIRST_PAGE)
            } else {
                canLoadMore = result.getOrNull() ?: false
                fetchingState.emit(IDLE)
            }
        }
    }

    fun endOfListReached(viewModelScope: CoroutineScope) {
        if (fetchingState.value == ERROR_LOADING_MORE) return
        loadMore(viewModelScope)
    }

    fun loadMore(viewModelScope: CoroutineScope) {
        if (!canLoadMore || loadingMoreJob?.isActive == true) {
            return
        }
        loadingMoreJob = viewModelScope.launch(Dispatchers.IO) {
            fetchingFirstPageJob?.join()
            fetchingState.emit(LOADING_MORE)
            val result = couponsDataSource.loadMore()
            if (!result.isSuccess) {
                delay(500) // avoid UI flickering when there is no network connection
                fetchingState.emit(ERROR_LOADING_MORE)
            } else {
                canLoadMore = result.getOrNull() ?: false
                fetchingState.emit(IDLE)
            }
        }
    }

    private suspend fun WooPosCouponsListViewStateManager.mapCouponsToSelectionState(
        coupons: List<Coupon>
    ) = coupons.map { coupon ->
        WooPosItemSelectionViewState.Coupon(
            id = coupon.id,
            name = coupon.code.orEmpty(),
            summary = formatCouponSummary(coupon, getCachedStoreCurrency())
        )
    }

    private fun mapFetchingStateToPaginationState(fetchingState: FetchingCouponsState) =
        when (fetchingState) {
            IDLE, FETCHING_FIRST_PAGE, LOADING_MORE -> if (canLoadMore) Loading else None
            ERROR_LOADING_MORE -> Error
            ERROR_FETCHING_FIRST_PAGE -> error("Full screen error should be displayed")
        }

    private fun mapFetchingStateToPTRState(fetchingState: FetchingCouponsState) =
        when (fetchingState) {
            ERROR_LOADING_MORE, IDLE, ERROR_FETCHING_FIRST_PAGE -> Enabled
            // We might need to keep this enabled and cancel the loadMore job when PTR starts.
            LOADING_MORE -> Disabled
            FETCHING_FIRST_PAGE -> Refreshing
        }
}
