package com.woocommerce.android.ui.woopos.home.items.coupons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosCouponsViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel.ItemClickedData
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsUIEvent.BackButtonClicked
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsUIEvent.CouponClicked
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsUIEvent.EndOfListReached
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsUIEvent.PullToRefreshTriggered
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsUIEvent.RetryLoadMoreTriggered
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsUIEvent.RetryTriggered
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator.WooPosItemsScreenNavigationEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosCouponsViewModel @Inject constructor(
    private val listViewStateManager: WooPosCouponsListViewStateManager,
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

    init {
        viewModelScope.launch {
            listViewStateManager.viewState.collect { newState ->
                _viewState.value = newState
            }
        }

        listViewStateManager.fetchCoupons(viewModelScope)
    }

    fun onUIEvent(event: WooPosCouponsUIEvent) {
        when (event) {
            is CouponClicked -> {
                handleCouponClicked(event)
            }

            PullToRefreshTriggered -> fetchCoupons()

            is EndOfListReached -> {
                onEndOfListReached()
            }

            RetryLoadMoreTriggered -> {
                retryLoadMore()
            }

            BackButtonClicked -> {
                navigateBackToItemListScreen()
            }

            RetryTriggered -> fetchCoupons()
        }
    }

    private fun fetchCoupons() {
        listViewStateManager.fetchCoupons(viewModelScope)
    }

    private fun onEndOfListReached() {
        listViewStateManager.endOfListReached(viewModelScope)
    }

    private fun retryLoadMore() {
        listViewStateManager.retryLoadMore(viewModelScope)
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
}
