package com.woocommerce.android.ui.woopos.home.items.coupons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosCouponsViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel.ItemClickedData
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
        // TODO load initial coupons
    }

    fun onUIEvent(event: WooPosCouponsUIEvent) {
        when (event) {
            is WooPosCouponsUIEvent.CouponClicked -> {
                handleCouponClicked(event)
            }

            WooPosCouponsUIEvent.PullToRefreshTriggered -> {
                // TODO PTR Action
            }

            is WooPosCouponsUIEvent.EndOfItemsListReached -> {
                onEndOfProductsListReached()
            }

            WooPosCouponsUIEvent.RetryLoadMoreTriggered -> {
                // TODO retry load more action
            }

            WooPosCouponsUIEvent.BackButtonClicked -> {
                navigateBackToItemListScreen()
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

    private fun handleCouponClicked(event: WooPosCouponsUIEvent.CouponClicked) {
        // TODO handle coupon click
    }

    private fun onEndOfProductsListReached() {
        // TODO Load More
    }

    private fun onItemClicked(itemData: ItemClickedData) {
        viewModelScope.launch {
            fromChildToParentEventSender.sendToParent(
                // TODO rename ItemClickedInProductSelector to ItemClicked
                ChildToParentEvent.ItemClickedInProductSelector(itemData)
            )
        }
    }
}

