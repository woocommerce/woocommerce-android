package com.woocommerce.android.ui.woopos.home.items.coupons.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.home.items.WooPosCouponsViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel.ItemClickedData
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsListViewStateManager
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ItemAddedToCart
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosCouponsSearchViewModel @Inject constructor(
    private val viewStateManager: WooPosCouponsListViewStateManager,
    private val childToParentEventSender: WooPosChildrenToParentEventSender,
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver,
) : ViewModel() {

    val viewState: StateFlow<WooPosCouponsSearchViewState> = viewStateManager.viewState
        .map { couponsViewState -> couponsViewState.toSearchViewState() }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = WooPosCouponsSearchViewState.EmptySearchQuery,
        )

    init {
        listenEventsFromParent()
    }

    fun onUIEvent(event: WooPosCouponsSearchUiEvent) {
        when (event) {
            WooPosCouponsSearchUiEvent.OnNextPageRequested -> {
                viewStateManager.endOfListReached(viewModelScope)
            }
            is WooPosCouponsSearchUiEvent.OnCouponClicked -> {
                handleCouponClicked(coupon = event.coupon)
            }
            WooPosCouponsSearchUiEvent.LoadingErrorRetryButtonClicked -> {
                handleLoadingErrorRetryClick()
            }
        }
    }

    private fun handleLoadingErrorRetryClick() {
        viewStateManager.fetchCoupons(
            viewModelScope, 
            WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType.RETRY
        )
    }

    private fun handleCouponClicked(coupon: WooPosItemSelectionViewState.Coupon) {
        viewModelScope.launch {
            val itemData = ItemClickedData.Coupon(coupon.id, coupon.name)
            childToParentEventSender.sendToParent(
                ChildToParentEvent.ItemClickedInProductSelector(
                    itemData = itemData,
                    eventForTracking = ItemAddedToCart(
                        item = itemData,
                        source = WooPosAnalyticsEventConstant.ItemsListSource.COUPON,
                        sourceType = WooPosAnalyticsEventConstant.ItemsListSourceType.SEARCH_RESULT
                    )
                )
            )
        }
    }

    private fun listenEventsFromParent() {
        viewModelScope.launch {
            parentToChildrenEventReceiver.events.collect { event ->
                when (event) {
                    is ParentToChildrenEvent.SearchEvent.ChangedQuery -> {
                        viewStateManager.setSearchQuery(event.query, viewModelScope)
                    }
                    ParentToChildrenEvent.SearchEvent.Finished -> {
                        viewStateManager.setSearchQuery(null, viewModelScope)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun WooPosCouponsViewState.toSearchViewState(): WooPosCouponsSearchViewState {
        val currentQuery = viewStateManager.getCurrentSearchQuery()
        
        return when {
            currentQuery.isNullOrBlank() -> WooPosCouponsSearchViewState.EmptySearchQuery
            
            this is WooPosCouponsViewState.Loading -> WooPosCouponsSearchViewState.Loading
            this is WooPosCouponsViewState.Empty -> WooPosCouponsSearchViewState.Empty
            this is WooPosCouponsViewState.Content -> WooPosCouponsSearchViewState.Content(
                items = this.items.filterIsInstance<WooPosItemSelectionViewState.Coupon>(),
                searchQuery = currentQuery,
                paginationState = this.paginationState,
                pullToRefreshState = this.pullToRefreshState
            )
            this is WooPosCouponsViewState.Error.GenericError -> WooPosCouponsSearchViewState.Error(currentQuery)
            this is WooPosCouponsViewState.Error.CouponsDisabledError -> WooPosCouponsSearchViewState.Error(currentQuery)
            else -> WooPosCouponsSearchViewState.EmptySearchQuery
        }
    }
}
