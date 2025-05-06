package com.woocommerce.android.ui.woopos.home.items.coupons

import com.woocommerce.android.model.Coupon
import com.woocommerce.android.ui.woopos.home.items.WooPosCouponsViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState.Error
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState.Loading
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState.None
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState.Enabled
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState.Refreshing
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsListViewStateManager.FetchingCouponsState.ERROR_FETCHING_FIRST_PAGE
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsListViewStateManager.FetchingCouponsState.ERROR_FETCHING_MORE
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsListViewStateManager.FetchingCouponsState.FETCHING_FIRST_PAGE
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsListViewStateManager.FetchingCouponsState.FETCHING_MORE
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsListViewStateManager.FetchingCouponsState.IDLE
import com.woocommerce.android.ui.woopos.util.WooPosGetCachedStoreCurrency
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatCouponSummary
import kotlinx.coroutines.CoroutineDispatcher
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

private const val AVOID_UI_FLICKERING_DELAY = 500L

class WooPosCouponsListViewStateManager @Inject constructor(
    private val couponsDataSource: WooPosCouponsDataSource,
    private val formatCouponSummary: WooPosFormatCouponSummary,
    private val getCachedStoreCurrency: WooPosGetCachedStoreCurrency,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val fetchingState: MutableStateFlow<FetchingCouponsState> = MutableStateFlow(IDLE)

    private var loadingMoreJob: Job? = null
    private var fetchingFirstPageJob: Job? = null
    private var canLoadMore: Boolean = false

    enum class FetchingCouponsState {
        IDLE, FETCHING_FIRST_PAGE, FETCHING_MORE, ERROR_FETCHING_FIRST_PAGE, ERROR_FETCHING_MORE
    }

    private val contentFlow = couponsDataSource.couponsFlow.combine(fetchingState) { coupons, fetchingState ->
        when (fetchingState) {
            FETCHING_FIRST_PAGE -> {
                if (coupons.isEmpty()) {
                    WooPosCouponsViewState.Loading()
                } else {
                    showCachedData(coupons, fetchingState)
                }
            }

            ERROR_FETCHING_FIRST_PAGE -> {
                WooPosCouponsViewState.Error()
            }

            IDLE, FETCHING_MORE, ERROR_FETCHING_MORE -> {
                when {
                    coupons.isEmpty() -> WooPosCouponsViewState.Empty()
                    else -> {
                        showCachedData(coupons, fetchingState)
                    }
                }
            }
        }
    }

    private suspend fun WooPosCouponsListViewStateManager.showCachedData(
        coupons: List<Coupon>,
        fetchingState: FetchingCouponsState
    ) = WooPosCouponsViewState.Content(
        items = mapCouponsToSelectionState(coupons),
        paginationState = getPaginationState(fetchingState),
        pullToRefreshState = getPullToRefreshState(fetchingState)
    )

    val viewState: Flow<WooPosCouponsViewState> = contentFlow

    fun fetchCoupons(viewModelScope: CoroutineScope) {
        if (fetchingFirstPageJob?.isActive == true) {
            return
        }

        fetchingFirstPageJob = viewModelScope.launch(dispatcher) {
            loadingMoreJob?.cancelAndJoin()
            fetchingState.emit(FETCHING_FIRST_PAGE)
            val result = couponsDataSource.clearCacheAndFetchFirstPage()
            if (result.isSuccess) {
                canLoadMore = result.getOrNull() ?: false
                fetchingState.emit(IDLE)
            } else {
                delay(AVOID_UI_FLICKERING_DELAY)
                fetchingState.emit(ERROR_FETCHING_FIRST_PAGE)
            }
        }
    }

    fun endOfListReached(viewModelScope: CoroutineScope) {
        if (fetchingState.value == ERROR_FETCHING_MORE) return

        fetchingFirstPageJob?.invokeOnCompletion {
            retryLoadMore(viewModelScope)
        } ?: retryLoadMore(viewModelScope)
    }

    fun retryLoadMore(viewModelScope: CoroutineScope) {
        if (!canLoadMore || loadingMoreJob?.isActive == true || fetchingFirstPageJob?.isActive == true) {
            return
        }

        loadingMoreJob = viewModelScope.launch(dispatcher) {
            fetchingState.emit(FETCHING_MORE)
            val result = couponsDataSource.loadMore()
            if (!result.isSuccess) {
                delay(AVOID_UI_FLICKERING_DELAY)
                fetchingState.emit(ERROR_FETCHING_MORE)
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

    private fun getPaginationState(fetchingState: FetchingCouponsState) =
        when (fetchingState) {
            IDLE, FETCHING_FIRST_PAGE, FETCHING_MORE -> if (canLoadMore) Loading else None
            ERROR_FETCHING_MORE -> Error
            ERROR_FETCHING_FIRST_PAGE -> error("Full screen error should be displayed")
        }

    private fun getPullToRefreshState(fetchingState: FetchingCouponsState) =
        when (fetchingState) {
            ERROR_FETCHING_MORE, IDLE, FETCHING_MORE -> Enabled
            FETCHING_FIRST_PAGE -> Refreshing
            ERROR_FETCHING_FIRST_PAGE -> error("Full screen error should be displayed")
        }
}
