package com.woocommerce.android.ui.woopos.home.items.coupons.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.Coupon
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.util.WooPosGetCachedStoreCurrency
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant
import com.woocommerce.android.ui.woopos.util.format.WooPosCouponsFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

@HiltViewModel
class WooPosCouponsSearchViewModel @Inject constructor(
    private val dataSource: WooPosCouponsSearchDataSource,
    private val childToParentEventSender: WooPosChildrenToParentEventSender,
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver,
    private val couponsFormatter: WooPosCouponsFormatter,
    private val getCachedStoreCurrency: WooPosGetCachedStoreCurrency,
    private val emptyStateRepository: WooPosCouponsSearchEmptyStateRepository,
) : ViewModel() {
    private val _viewState =
        MutableStateFlow<WooPosCouponsSearchViewState>(
            WooPosCouponsSearchViewState.EmptySearchQuery(
                recentSearches = emptyList()
            )
        )
    val viewState: StateFlow<WooPosCouponsSearchViewState> = _viewState
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = _viewState.value,
        )

    private var loadMoreJob: Job? = null
    private var searchJob: Job? = null

    private val currentQuery = AtomicReference("")

    init {
        setEmptySearchQueryState()
        listenEventsFromParent()
    }

    fun onUIEvent(event: WooPosCouponsSearchUiEvent) {
        when (event) {
            WooPosCouponsSearchUiEvent.OnNextPageRequested -> onEndOfListReached()
            is WooPosCouponsSearchUiEvent.OnCouponClicked -> {
                handleCouponClicked(event.coupon, WooPosAnalyticsEventConstant.ItemsListSourceType.SEARCH_RESULT)
            }

            WooPosCouponsSearchUiEvent.LoadingErrorRetryButtonClicked -> handleLoadingErrorRetryClick()
            is WooPosCouponsSearchUiEvent.OnRecentSearchClicked -> {
                viewModelScope.launch {
                    childToParentEventSender.sendToParent(
                        ChildToParentEvent.SearchEvent.RecentSearchSelected(
                            event.recentSearch
                        )
                    )
                }
            }
        }
    }

    private fun handleLoadingErrorRetryClick() {
        val currentState = _viewState.value as? WooPosCouponsSearchViewState.Error ?: return

        _viewState.value = WooPosCouponsSearchViewState.Loading
        performSearch(currentState.searchQuery)
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) {
            setEmptySearchQueryState()
            return
        }

        currentQuery.set(query)

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _viewState.value = WooPosCouponsSearchViewState.Loading

            delay(SEARCH_DEBOUNCING_TIME)

            val searchResult = dataSource.searchCoupons(query)
            if (searchResult.isSuccess) {
                handleSearchSuccess(query)
            } else {
                _viewState.value = WooPosCouponsSearchViewState.Error(searchQuery = query)
            }
        }
    }

    private fun setEmptySearchQueryState() {
        viewModelScope.launch {
            val recentSearches = emptyStateRepository.getLastSearches()
            _viewState.value = WooPosCouponsSearchViewState.EmptySearchQuery(
                recentSearches = recentSearches.take(MAX_LAST_SEARCHES)
            )
        }
    }

    private suspend fun handleSearchSuccess(query: String) {
        val coupons = dataSource.couponsFlow.stateIn(viewModelScope).value

        if (coupons.isEmpty()) {
            _viewState.value = WooPosCouponsSearchViewState.Empty
        } else {
            _viewState.value = coupons.toContentState(query)
        }
    }

    private fun onEndOfListReached() {
        if (loadMoreJob?.isActive == true) return

        loadMoreJob = viewModelScope.launch {
            val currentState =
                _viewState.value as? WooPosCouponsSearchViewState.Content ?: return@launch

            _viewState.value = currentState.copy(
                paginationState = WooPosPaginationState.Loading
            )

            val result = dataSource.loadMore()
            if (result.isSuccess) {
                val coupons = dataSource.couponsFlow.stateIn(viewModelScope).value
                _viewState.value = coupons.toContentState(
                    currentState.searchQuery,
                    WooPosPaginationState.None
                )
            } else {
                _viewState.value = currentState.copy(
                    paginationState = WooPosPaginationState.Error
                )
            }
        }
    }

    private fun handleCouponClicked(
        coupon: WooPosItemSelectionViewState.Coupon,
        sourceType: WooPosAnalyticsEventConstant.ItemsListSourceType
    ) {
        viewModelScope.launch {
            val itemData = WooPosItemsViewModel.ItemClickedData.Coupon(coupon.id, coupon.name)
            childToParentEventSender.sendToParent(
                ChildToParentEvent.ItemClickedInProductSelector(
                    itemData = itemData,
                    eventForTracking = WooPosAnalyticsEvent.Event.ItemAddedToCart(
                        item = itemData,
                        source = WooPosAnalyticsEventConstant.ItemsListSource.COUPON,
                        sourceType = sourceType
                    )
                )
            )
        }
        storeRecentSearch()
    }

    private fun storeRecentSearch() {
        (_viewState.value as? WooPosCouponsSearchViewState.Content)?.let {
            viewModelScope.launch {
                emptyStateRepository.addRecentSearch(it.searchQuery)
            }
        }
    }

    private fun listenEventsFromParent() {
        viewModelScope.launch {
            parentToChildrenEventReceiver.events.collect { event ->
                when (event) {
                    is ParentToChildrenEvent.SearchEvent.ChangedQuery -> performSearch(event.query)
                    ParentToChildrenEvent.SearchEvent.Finished -> setEmptySearchQueryState()
                    else -> {}
                }
            }
        }
    }

    private suspend fun List<Coupon>.toContentState(
        searchQuery: String,
        paginationState: WooPosPaginationState = WooPosPaginationState.None,
    ): WooPosCouponsSearchViewState.Content {
        val currency = getCachedStoreCurrency()
        return WooPosCouponsSearchViewState.Content(
            items = map { it.toViewState(currency) },
            searchQuery = searchQuery,
            paginationState = paginationState,
        )
    }

    private fun Coupon.toViewState(currency: String): WooPosItemSelectionViewState.Coupon {
        val expiredState = if (this.dateExpires != null) {
            WooPosItemSelectionViewState.Coupon.ExpiredState.Expired(
                formattedDate = couponsFormatter.formatExpiredText(this.dateExpires!!)
            )
        } else {
            WooPosItemSelectionViewState.Coupon.ExpiredState.NotExpired
        }

        return WooPosItemSelectionViewState.Coupon(
            id = this.id,
            name = this.code ?: "",
            summary = couponsFormatter.formatSummary(this, currency),
            expiredState = expiredState
        )
    }

    private companion object {
        const val SEARCH_DEBOUNCING_TIME = 500L
        const val MAX_LAST_SEARCHES = 10
    }
}
