package com.woocommerce.android.ui.woopos.home.items.coupons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosCouponsViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel.ItemClickedData
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsUIEvent.BackButtonClicked
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsUIEvent.CouponClicked
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsUIEvent.EndOfListReached
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsUIEvent.PullToRefreshTriggered
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsUIEvent.RetryLoadMoreTriggered
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsUIEvent.RetryTriggered
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator.WooPosItemsScreenNavigationEvent
import com.woocommerce.android.ui.woopos.home.items.updatePullToRefreshState
import com.woocommerce.android.ui.woopos.util.GetCachedStoreCurrency
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatCouponSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosCouponsViewModel @Inject constructor(
    private val couponsDataSource: WooPosCouponsDataSource,
    private val formatCouponSummary: WooPosFormatCouponSummary,
    private val getCachedStoreCurrency: GetCachedStoreCurrency,
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender,
    private val navigator: WooPosItemsNavigator,
) : ViewModel() {
    private val _viewState =
        MutableStateFlow<WooPosCouponsViewState>(WooPosCouponsViewState.Loading())

    val viewState: StateFlow<WooPosCouponsViewState> = _viewState
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = _viewState.value,
        )

    private val couponsList = couponsDataSource.couponsFlow
    private val isFetching = couponsDataSource.isFetching

    init {
        viewModelScope.launch {
            couponsList.combine(isFetching) { coupons, isFetching ->
                if (coupons.isEmpty()) {
                    if (isFetching) {
                        WooPosCouponsViewState.Loading()
                    } else {
                        WooPosCouponsViewState.Empty()
                    }
                } else {
                    WooPosCouponsViewState.Content(
                        items = coupons.map { coupon ->
                            WooPosItemSelectionViewState.Coupon(
                                id = coupon.id,
                                name = coupon.code ?: "",
                                summary = formatCouponSummary(coupon, getCachedStoreCurrency()),
                            )
                        },
                        paginationState = WooPosPaginationState.None,
                        pullToRefreshState = WooPosPullToRefreshState.Enabled,
                    )
                }
            }.collect { newState ->
                _viewState.value = newState
            }
        }

        fetchCoupons(
            withPullToRefresh = false
        )
    }

    fun onUIEvent(event: WooPosCouponsUIEvent) {
        when (event) {
            is CouponClicked -> {
                handleCouponClicked(event)
            }

            PullToRefreshTriggered -> {
                // CouponsProject: PTR Action
            }

            is EndOfListReached -> {
                onEndOfListReached()
            }

            RetryLoadMoreTriggered -> {
                // CouponsProject: retry load more action
            }

            BackButtonClicked -> {
                navigateBackToItemListScreen()
            }

            RetryTriggered -> fetchCoupons(withPullToRefresh = false)
        }
    }

    private fun fetchCoupons(withPullToRefresh: Boolean) {
        viewModelScope.launch {
            _viewState.value = if (withPullToRefresh) {
                _viewState.value.updatePullToRefreshState(newState = WooPosPullToRefreshState.Refreshing)
            } else {
                WooPosCouponsViewState.Loading()
            }
            val result = couponsDataSource.clearCacheAndFetchFirstPage()

            if (!result.isSuccess) {
                _viewState.value = WooPosCouponsViewState.Error()
            }
        }
    }

    private fun navigateBackToItemListScreen() {
        viewModelScope.launch {
            navigator.sendNavigationEvent(
                WooPosItemsScreenNavigationEvent.NavigateBackToItemListScreen
            )
        }
    }

    private fun handleCouponClicked(event: CouponClicked) {
        viewModelScope.launch {
            fromChildToParentEventSender.sendToParent(
                // CouponsProject: rename ItemClickedInProductSelector to ItemClicked
                ChildToParentEvent.ItemClickedInProductSelector(ItemClickedData.Coupon(event.couponId))
            )
        }
    }

    private fun onEndOfListReached() {
        val currentState = _viewState.value
        if (currentState !is WooPosCouponsViewState.Content) {
            return
        }

        _viewState.value = currentState.copy(paginationState = WooPosPaginationState.Loading)
        viewModelScope.launch {
            val result = couponsDataSource.loadMore()
            if (!result.isSuccess) {
                _viewState.value = currentState.copy(paginationState = WooPosPaginationState.Error)
            }
        }
    }
}
