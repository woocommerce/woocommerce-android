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
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsListViewStateManager.FetchingCouponsState.ERROR_FETCHING_MORE
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsListViewStateManager.FetchingCouponsState.FETCHING_FIRST_PAGE
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsListViewStateManager.FetchingCouponsState.FETCHING_MORE
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsListViewStateManager.FetchingCouponsState.IDLE
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsListViewStateManager.FetchingCouponsState.PTR_FETCHING_FIRST_PAGE
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsListViewStateManager.FetchingCouponsState.RETRY_FETCHING_FIRST_PAGE
import com.woocommerce.android.ui.woopos.util.WooPosGetCachedStoreCurrency
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.format.WooPosCouponsFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Date
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

private const val AVOID_UI_FLICKERING_DELAY = 500L
private const val SEARCH_DEBOUNCE_TIME = 500L
private const val MAX_RECENT_SEARCHES = 10

class WooPosCouponsListViewStateManager @Inject constructor(
    private val couponsDataSource: WooPosCouponsDataSource,
    private val couponsFormatter: WooPosCouponsFormatter,
    private val getCachedStoreCurrency: WooPosGetCachedStoreCurrency,
    private val cachedCouponEnabledChecker: CachedCouponEnabledChecker,
    private val analyticsTracker: WooPosAnalyticsTracker,
) {
    private val fetchingState: MutableStateFlow<FetchingCouponsState> = MutableStateFlow(IDLE)
    private val currentSearchQuery = AtomicReference<String?>(null)
    private val recentSearches = AtomicReference<List<String>>(emptyList())

    private var loadingMoreJob: Job? = null
    private var fetchingFirstPageJob: Job? = null
    private var searchJob: Job? = null
    private var canLoadMore: Boolean = true

    enum class FetchingCouponsState {
        IDLE, FETCHING_FIRST_PAGE, PTR_FETCHING_FIRST_PAGE, RETRY_FETCHING_FIRST_PAGE, FETCHING_MORE,
        ERROR_FETCHING_FIRST_PAGE, ERROR_FETCHING_MORE
    }

    private val contentFlow = couponsDataSource.couponsFlow.combine(fetchingState) { coupons, fetchingState ->
        if (!cachedCouponEnabledChecker.isEnabled()) {
            return@combine WooPosCouponsViewState.Error.CouponsDisabledError()
        }
        when (fetchingState) {
            FETCHING_FIRST_PAGE, PTR_FETCHING_FIRST_PAGE -> {
                if (coupons.isEmpty()) {
                    createLoadingState(fetchingState)
                } else {
                    createContentViewState(coupons, fetchingState)
                }
            }

            RETRY_FETCHING_FIRST_PAGE -> {
                createLoadingState(fetchingState)
            }

            ERROR_FETCHING_FIRST_PAGE -> {
                WooPosCouponsViewState.Error.GenericError()
            }

            IDLE, FETCHING_MORE, ERROR_FETCHING_MORE -> {
                if (coupons.isEmpty()) {
                    WooPosCouponsViewState.Empty()
                } else {
                    createContentViewState(coupons, fetchingState)
                }
            }
        }
    }

    private fun createLoadingState(fetchingState: FetchingCouponsState) =
        WooPosCouponsViewState.Loading(
            pullToRefreshState = if (fetchingState == PTR_FETCHING_FIRST_PAGE) {
                Refreshing
            } else {
                Disabled
            }
        )

    private suspend fun createContentViewState(
        coupons: List<Coupon>,
        fetchingState: FetchingCouponsState
    ) = WooPosCouponsViewState.Content(
        items = mapCouponsToSelectionState(coupons),
        paginationState = getPaginationState(fetchingState),
        pullToRefreshState = getPullToRefreshState(fetchingState)
    )

    val viewState: Flow<WooPosCouponsViewState> = contentFlow

    fun getCurrentSearchQuery(): String? = currentSearchQuery.get()

    fun getRecentSearches(): List<String> = recentSearches.get()

    fun setRecentSearches(searches: List<String>) {
        recentSearches.set(searches.take(MAX_RECENT_SEARCHES))
    }

    fun setSearchQuery(query: String?, viewModelScope: CoroutineScope) {
        searchJob?.cancel()
        loadingMoreJob?.cancel()
        fetchingFirstPageJob?.cancel()

        if (query.isNullOrBlank()) {
            currentSearchQuery.set(null)
            fetchCoupons(viewModelScope, WooPosCouponsListRefreshType.INITIAL)
            return
        }

        currentSearchQuery.set(query)

        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_TIME)
            fetchCoupons(viewModelScope, WooPosCouponsListRefreshType.INITIAL)
        }
    }

    fun fetchCoupons(viewModelScope: CoroutineScope, refreshType: WooPosCouponsListRefreshType) {
        if (fetchingFirstPageJob?.isActive == true) {
            return
        }

        fetchingFirstPageJob = viewModelScope.launch {
            loadingMoreJob?.cancelAndJoin()
            fetchingState.emit(
                when (refreshType) {
                    WooPosCouponsListRefreshType.INITIAL -> FETCHING_FIRST_PAGE
                    WooPosCouponsListRefreshType.PULL_TO_REFRESH -> PTR_FETCHING_FIRST_PAGE
                    WooPosCouponsListRefreshType.RETRY -> RETRY_FETCHING_FIRST_PAGE
                }
            )
            val result = couponsDataSource.clearCacheAndFetchFirstPage(currentSearchQuery.get())
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

        loadingMoreJob = viewModelScope.launch {
            fetchingState.emit(FETCHING_MORE)
            val result = couponsDataSource.loadMore()
            if (!result.isSuccess) {
                delay(AVOID_UI_FLICKERING_DELAY)
                fetchingState.emit(ERROR_FETCHING_MORE)
            } else {
                canLoadMore = result.getOrNull() ?: false
                fetchingState.emit(IDLE)
                trackNextPageLoaded()
            }
        }
    }

    private suspend fun trackNextPageLoaded() {
        val sourceType = if (currentSearchQuery.get().isNullOrBlank()) {
            WooPosAnalyticsEventConstant.ItemsListSourceType.LIST
        } else {
            WooPosAnalyticsEventConstant.ItemsListSourceType.SEARCH_RESULT
        }

        analyticsTracker.track(
            WooPosAnalyticsEvent.Event.ItemsNextPageLoaded(
                source = WooPosAnalyticsEventConstant.ItemsListSource.COUPON,
                sourceType = sourceType
            )
        )
    }

    // This method should eventually be moved out of this class
    private suspend fun WooPosCouponsListViewStateManager.mapCouponsToSelectionState(
        coupons: List<Coupon>
    ): List<WooPosItemSelectionViewState.Coupon> {
        val currentDate = Date()
        return coupons.map { coupon ->
            WooPosItemSelectionViewState.Coupon(
                id = coupon.id,
                name = coupon.code.orEmpty(),
                summary = couponsFormatter.formatSummary(coupon, getCachedStoreCurrency()),
                expiredState = determineExpiredState(coupon, currentDate)
            )
        }
    }

    private fun getPaginationState(fetchingState: FetchingCouponsState) =
        when (fetchingState) {
            IDLE, FETCHING_FIRST_PAGE, PTR_FETCHING_FIRST_PAGE, RETRY_FETCHING_FIRST_PAGE, FETCHING_MORE -> {
                if (canLoadMore) Loading else None
            }

            ERROR_FETCHING_MORE -> Error
            ERROR_FETCHING_FIRST_PAGE -> error("Full screen error should be displayed")
        }

    private fun getPullToRefreshState(fetchingState: FetchingCouponsState) =
        when (fetchingState) {
            IDLE, FETCHING_MORE, ERROR_FETCHING_MORE -> Enabled
            FETCHING_FIRST_PAGE, RETRY_FETCHING_FIRST_PAGE -> Disabled
            PTR_FETCHING_FIRST_PAGE -> Refreshing
            ERROR_FETCHING_FIRST_PAGE -> error("Full screen error should be displayed")
        }

    private fun determineExpiredState(
        coupon: Coupon,
        currentDate: Date
    ): WooPosItemSelectionViewState.Coupon.ExpiredState {
        val expiryDateGmt = coupon.dateExpiresGmt
            ?: return WooPosItemSelectionViewState.Coupon.ExpiredState.NotExpired

        return if (expiryDateGmt.before(currentDate)) {
            WooPosItemSelectionViewState.Coupon.ExpiredState.Expired(
                coupon.dateExpiresLocal?.let(couponsFormatter::formatExpiredText)
            )
        } else {
            WooPosItemSelectionViewState.Coupon.ExpiredState.NotExpired
        }
    }

    enum class WooPosCouponsListRefreshType {
        INITIAL, PULL_TO_REFRESH, RETRY
    }
}
