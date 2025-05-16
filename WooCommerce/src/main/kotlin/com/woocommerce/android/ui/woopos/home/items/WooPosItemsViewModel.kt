package com.woocommerce.android.ui.woopos.home.items

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewState.Tab
import com.woocommerce.android.ui.woopos.home.items.coupons.creation.WooPosCouponCreationFacade
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator
import com.woocommerce.android.ui.woopos.home.items.navigation.WooPosItemsNavigator.WooPosItemsScreenNavigationEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.SearchButtonTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class WooPosItemsViewModel @Inject constructor(
    private val navigator: WooPosItemsNavigator,
    private val searchHelper: WooPosItemsSearchHelper,
    private val tabsHelper: WooPosItemsTabsHelper,
    private val couponCreationFacade: WooPosCouponCreationFacade,
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender,
    private val analyticsTracker: WooPosAnalyticsTracker,
) : ViewModel() {
    private val _viewState = MutableStateFlow<WooPosItemsViewState>(
        WooPosItemsViewState.ProductList(
            tabs = tabsHelper.defaultTabs,
            search = searchHelper.getInitialSearchState(),
        )
    )
    val viewState: StateFlow<WooPosItemsViewState> = _viewState
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = _viewState.value,
        )

    init {
        searchHelper.initialize(
            coroutineScope = viewModelScope,
            viewStateFlow = _viewState
        )
    }

    fun onUIEvent(event: WooPosItemsUIEvent) {
        when (event) {
            WooPosItemsUIEvent.BackButtonClicked -> {
                navigateBackToItemListScreen()
            }

            WooPosItemsUIEvent.ClearSearchClicked -> searchHelper.onClearSearchClicked()
            WooPosItemsUIEvent.CloseSearchClicked -> searchHelper.onCloseSearchClicked()
            is WooPosItemsUIEvent.SearchChanged -> searchHelper.onSearchChanged(
                event.query,
                event.cursorPosition
            )

            WooPosItemsUIEvent.SearchAnimationComplete -> searchHelper.onAnimationComplete()

            is WooPosItemsUIEvent.OnTabClicked -> selectTab(event.tab)
            WooPosItemsUIEvent.SearchIconClicked -> {
                searchHelper.onSearchChanged("", 0)
                trackSearchIconClicked()
            }

            is WooPosItemsUIEvent.AddCouponIconClicked -> createAndAddCoupon()
        }
    }

    private fun trackSearchIconClicked() {
        viewModelScope.launch {
            val event = SearchButtonTapped(
                source = WooPosAnalyticsEventConstant.ItemsListSource.PRODUCT,
            )
            analyticsTracker.track(event)
        }
    }

    private fun navigateBackToItemListScreen() {
        viewModelScope.launch {
            navigator.sendNavigationEvent(
                WooPosItemsScreenNavigationEvent.NavigateBackToItemListScreen
            )
        }
    }

    private fun selectTab(selectedTab: Tab) {
        if (_viewState.value.tabs.size == 1) return

        val state = _viewState.value

        _viewState.value = when (selectedTab.stringId) {
            R.string.woopos_products_screen_title -> WooPosItemsViewState.ProductList(
                tabs = tabsHelper.selectTab(state.tabs, selectedTab),
                search = searchHelper.getInitialSearchState(),
            ).also {
                viewModelScope.launch {
                    analyticsTracker.track(
                        WooPosAnalyticsEvent.Event.ItemsHeaderTapped(
                            WooPosAnalyticsEventConstant.ItemsHeaderType.PRODUCT
                        )
                    )
                }
            }

            R.string.woopos_coupons_screen_title -> WooPosItemsViewState.CouponList(
                tabs = tabsHelper.selectTab(state.tabs, selectedTab),
            ).also {
                viewModelScope.launch {
                    analyticsTracker.track(
                        WooPosAnalyticsEvent.Event.ItemsHeaderTapped(
                            WooPosAnalyticsEventConstant.ItemsHeaderType.COUPON
                        )
                    )
                }
            }

            else -> error("Invalid tab $selectedTab")
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

    @Parcelize
    sealed class ItemClickedData(open val id: Long) : Parcelable {
        @Parcelize
        sealed class Product(override val id: Long) : ItemClickedData(id), Parcelable {
            @Parcelize
            data class Simple(override val id: Long) : Product(id), Parcelable

            @Parcelize
            data class Variation(val productId: Long, override val id: Long) : Product(id), Parcelable
        }

        @Parcelize
        data class Coupon(override val id: Long, val couponCode: String) : ItemClickedData(id), Parcelable
    }
}
