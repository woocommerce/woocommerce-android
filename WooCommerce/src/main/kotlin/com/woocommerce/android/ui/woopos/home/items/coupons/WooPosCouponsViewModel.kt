package com.woocommerce.android.ui.woopos.home.items.coupons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosCouponsViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel.ItemClickedData
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsListViewStateManager.WooPosCouponsListRefreshType
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsUIEvent.BackButtonClicked
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsUIEvent.CouponClicked
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsUIEvent.EndOfListReached
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsUIEvent.PullToRefreshTriggered
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsUIEvent.RetryLoadMoreTriggered
import com.woocommerce.android.ui.woopos.home.items.coupons.WooPosCouponsUIEvent.RetryTriggered
import com.woocommerce.android.ui.woopos.home.items.coupons.creation.WooPosCouponCreationFacade
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator.WooPosItemsScreenNavigationEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
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
    private val couponCreationFacade: WooPosCouponCreationFacade,
    private val navigator: WooPosItemsNavigator,
    private val analyticsTracker: WooPosAnalyticsTracker,
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

        fetchCoupons(WooPosCouponsListRefreshType.INITIAL)
    }

    fun onUIEvent(event: WooPosCouponsUIEvent) {
        when (event) {
            is CouponClicked -> {
                handleCouponClicked(event)
            }

            PullToRefreshTriggered -> fetchCoupons(WooPosCouponsListRefreshType.PULL_TO_REFRESH).also {
                viewModelScope.launch {
                    analyticsTracker.track(
                        WooPosAnalyticsEvent.Event.PullToRefreshTriggered(
                            source = WooPosAnalyticsEventConstant.ItemsListSource.COUPON,
                            sourceType = WooPosAnalyticsEventConstant.ItemsListSourceType.LIST
                        )
                    )
                }
            }

            is EndOfListReached -> {
                onEndOfListReached()
            }

            RetryLoadMoreTriggered -> {
                retryLoadMore()
            }

            BackButtonClicked -> {
                navigateBackToItemListScreen()
            }

            RetryTriggered -> fetchCoupons(WooPosCouponsListRefreshType.RETRY)

            is WooPosCouponsUIEvent.CreateCouponClicked -> createAndAddCoupon()
        }
    }

    private fun fetchCoupons(refreshType: WooPosCouponsListRefreshType) {
        listViewStateManager.fetchCoupons(viewModelScope, refreshType)
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
            val itemData = ItemClickedData.Coupon(event.couponId, event.couponCode)
            fromChildToParentEventSender.sendToParent(
                // CouponsProject: rename ItemClickedInProductSelector to ItemClicked
                ChildToParentEvent.ItemClickedInProductSelector(
                    itemData = itemData,
                    eventForTracking = WooPosAnalyticsEvent.Event.ItemAddedToCart(
                        item = itemData,
                        source = WooPosAnalyticsEventConstant.ItemsListSource.COUPON,
                        sourceType = WooPosAnalyticsEventConstant.ItemsListSourceType.LIST
                    )
                )
            )
        }
    }

    private fun createAndAddCoupon() {
        viewModelScope.launch {
            analyticsTracker.track(WooPosAnalyticsEvent.Event.CouponsCreateTapped)
            val coupon = couponCreationFacade.createCoupon()
            if (coupon != null) {
                val itemData = ItemClickedData.Coupon(coupon.id, coupon.code ?: "")
                fromChildToParentEventSender.sendToParent(
                    ChildToParentEvent.ItemClickedInProductSelector(
                        itemData = itemData,
                        eventForTracking = WooPosAnalyticsEvent.Event.ItemAddedToCart(
                            item = itemData,
                            source = WooPosAnalyticsEventConstant.ItemsListSource.COUPON,
                            sourceType = WooPosAnalyticsEventConstant.ItemsListSourceType.LIST
                        )
                    )
                )
            }
        }
    }
}
